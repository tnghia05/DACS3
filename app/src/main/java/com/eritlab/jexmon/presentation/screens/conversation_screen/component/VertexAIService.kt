package com.eritlab.jexmon.presentation.screens.conversation_screen.component

import android.content.Context
import android.util.Log
import com.eritlab.jexmon.domain.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class PriceRange(
    val min: Long,
    val max: Long
)

class VertexAIService(private val context: Context) {
    private val generativeModel by lazy {
        Firebase.vertexAI.generativeModel("gemini-2.0-flash")
    }
    
    private val firestore = FirebaseFirestore.getInstance()

    private var conversationContext = ConversationContext()

    private data class ConversationContext(
        var lastQuery: String = "",
        var lastResponse: String = "",
        var currentTopic: String = "",
        var mentionedProducts: MutableSet<String> = mutableSetOf(),
        var pricePreference: PriceRange? = null,
        var stylePreference: String = "",
        var sizePreference: String = "",
        var colorPreference: String = "",
        var brandPreference: String = ""
    )

    private fun updateContext(query: String, response: String) {
        conversationContext.lastQuery = query
        conversationContext.lastResponse = response
        
        // Cập nhật chủ đề hiện tại
        when {
            query.contains("giá") || query.contains("tiền") -> 
                conversationContext.currentTopic = "price"
            query.contains("size") || query.contains("số") -> 
                conversationContext.currentTopic = "size"
            query.contains("màu") || query.contains("color") -> 
                conversationContext.currentTopic = "color"
            query.contains("thương hiệu") || query.contains("hãng") -> 
                conversationContext.currentTopic = "brand"
            query.contains("phong cách") || query.contains("style") -> 
                conversationContext.currentTopic = "style"
        }

        // Cập nhật preferences
        extractPriceRange(query)?.let {
            conversationContext.pricePreference = it
        }
        
        // Cập nhật size preference
        Regex("size\\s*(\\d+)", RegexOption.IGNORE_CASE).find(query)?.let {
            conversationContext.sizePreference = it.groupValues[1]
        }
        
        // Cập nhật color preference
        listOf("đen", "trắng", "đỏ", "xanh", "vàng", "black", "white", "red", "blue", "yellow")
            .find { query.lowercase().contains(it) }?.let {
                conversationContext.colorPreference = it
            }
            
        // Cập nhật brand preference
        listOf("nike", "adidas", "puma", "bitis", "converse")
            .find { query.lowercase().contains(it) }?.let {
                conversationContext.brandPreference = it
            }
    }

    private fun buildProductCard(product: ProductModel): String {
        val features = extractKeyFeatures(product.description)
        // Nhóm các đặc điểm theo loại
        val groupedFeatures = features.groupBy { feature ->
            when {
                feature.contains("[Chất liệu]") -> "Chất liệu"
                feature.contains("[Công nghệ]") -> "Công nghệ"
                feature.contains("[Thiết kế]") -> "Thiết kế"
                feature.contains("[Đối tượng]") -> "Đối tượng sử dụng"
                feature.contains("[Mục đích sử dụng]") -> "Mục đích sử dụng"
                else -> "Khác"
            }
        }
        
        val formattedFeatures = groupedFeatures.map { (category, featureList) ->
            """
            |🔹 $category:
            |${featureList.joinToString("\n|") { "   • ${it.substringAfter("]").trim()}" }}
            """.trimMargin()
        }.joinToString("\n|")

        return """[PRODUCT_START]
        |Tên: ${product.name}
        |Hình ảnh: ${product.images.firstOrNull()}
        |Giá: ${formatPrice(product.price.toLong())}
        |${if (product.discount > 0) "Giảm giá: ${product.discount}%" else ""}
        |Size: ${product.stock.filter { it.quantity > 0 }.map { it.size }.distinct().sorted().joinToString(", ")}
        |Màu: ${product.stock.filter { it.quantity > 0 }.map { it.color }.distinct().joinToString(", ")}
        |Đánh giá: ${product.rating}/5
        |Đã bán: ${product.sold}
        |
        |📋 ĐẶC ĐIỂM NỔI BẬT:
        |$formattedFeatures
        |
        |Link: /product/${product.id}
        |[PRODUCT_END]""".trimMargin()
    }

    private fun buildContextualResponse(query: String, products: List<ProductModel>): String {
        val response = StringBuilder()
        
        // Thêm câu mở đầu dựa vào context
        when {
            conversationContext.currentTopic == "price" && conversationContext.pricePreference != null -> {
                response.append("Dạ, với khoảng giá từ ${formatPrice(conversationContext.pricePreference!!.min)} đến ${formatPrice(conversationContext.pricePreference!!.max)}, ")
            }
            conversationContext.brandPreference.isNotEmpty() -> {
                response.append("Dạ, về các sản phẩm ${conversationContext.brandPreference.capitalize()}, ")
            }
            else -> {
                response.append("Dạ, ")
            }
        }

        // Thêm câu chuyển tiếp dựa vào context trước đó
        when (conversationContext.currentTopic) {
            "price" -> response.append("em xin giới thiệu một số mẫu phù hợp với ngân sách:\n\n")
            "size" -> response.append("em xin giới thiệu các mẫu có size ${conversationContext.sizePreference}:\n\n")
            "color" -> response.append("em xin giới thiệu các mẫu màu ${conversationContext.colorPreference}:\n\n")
            "brand" -> response.append("em xin giới thiệu các mẫu đang có sẵn:\n\n")
            "style" -> response.append("em xin giới thiệu các mẫu phong cách ${conversationContext.stylePreference}:\n\n")
            else -> response.append("em xin giới thiệu một số mẫu phù hợp:\n\n")
        }

        // Thêm thông tin sản phẩm
        val productCards = products.take(3).joinToString("\n\n") { product ->
            buildProductCard(product)
        }
        response.append(productCards)

        // Thêm phân tích chi tiết
        val analysis = generateProductComparison(products)
        response.append("\n\n").append(analysis)

        // Thêm câu kết nối cho lần tương tác tiếp theo
        response.append("\n\nBạn có muốn em tư vấn thêm về:")
        when (conversationContext.currentTopic) {
            "price" -> response.append("\n• Các mẫu ở phân khúc giá khác")
            "size" -> response.append("\n• Các size khác đang có sẵn")
            "color" -> response.append("\n• Các màu sắc khác của mẫu này")
            "brand" -> response.append("\n• Các thương hiệu tương tự")
            "style" -> response.append("\n• Các phong cách khác")
        }
        response.append("\n• Thông tin chi tiết về bất kỳ sản phẩm nào bạn quan tâm")
        response.append("\n• Các chương trình khuyến mãi đang áp dụng")

        return response.toString()
    }

    private fun extractPriceRange(text: String): PriceRange? {
        val normalizedText = text.lowercase().replace("đ", "")
            .replace("vnd", "")
            .replace(".", "")
            .replace(",", "")
        
        // Convert "triệu" to actual number
        val processedText = normalizedText.replace(Regex("(\\d+)\\s*triệu")) { matchResult ->
            val number = matchResult.groupValues[1].toLong() * 1000000
            number.toString()
        }
        
        // Convert "k" to actual number
        val finalText = processedText.replace(Regex("(\\d+)\\s*k")) { matchResult ->
            val number = matchResult.groupValues[1].toLong() * 1000
            number.toString()
        }

        return when {
            // Pattern: "từ X đến Y"
            finalText.matches(Regex(".*(từ|from)\\s*(\\d+)\\s*(đến|to)\\s*(\\d+).*")) -> {
                val numbers = Regex("\\d+").findAll(finalText)
                    .map { it.value.toLong() }
                    .toList()
                PriceRange(numbers[0], numbers[1])
            }
            
            // Pattern: "dưới X" or "less than X"
            finalText.matches(Regex(".*(dưới|under|less than)\\s*(\\d+).*")) -> {
                val max = Regex("\\d+").find(finalText)?.value?.toLong() ?: return null
                PriceRange(0, max)
            }
            
            // Pattern: "trên X" or "more than X"
            finalText.matches(Regex(".*(trên|over|more than)\\s*(\\d+).*")) -> {
                val min = Regex("\\d+").find(finalText)?.value?.toLong() ?: return null
                PriceRange(min, Long.MAX_VALUE)
            }
            
            // Pattern: "tầm X" or "khoảng X" or "around X"
            finalText.matches(Regex(".*(tầm|khoảng|about|around)\\s*(\\d+).*")) -> {
                val center = Regex("\\d+").find(finalText)?.value?.toLong() ?: return null
                val margin = center * 0.2 // 20% margin
                PriceRange((center - margin).toLong(), (center + margin).toLong())
            }
            
            else -> null
        }
    }

    private fun buildSystemPrompt(userQuery: String, priceRange: PriceRange? = null): String {
        return """
            |Bạn là trợ lý AI của cửa hàng giày. Nhiệm vụ của bạn là giúp khách hàng tìm kiếm và gợi ý sản phẩm phù hợp.
            |
            |NGUYÊN TẮC TRẢ LỜI:
            |1. LUÔN duy trì chủ đề và ngữ cảnh của cuộc trò chuyện
            |2. KHÔNG hỏi lại thông tin mà khách hàng đã cung cấp
            |3. KHÔNG thay đổi chủ đề đột ngột
            |4. Khi khách hỏi về sản phẩm cụ thể, tập trung vào sản phẩm đó
            |5. Chỉ hỏi thêm thông tin khi thực sự cần thiết và liên quan đến chủ đề hiện tại
            |
            |THÔNG TIN CỬA HÀNG:
            |* Chuyên cung cấp các loại giày: thể thao, chạy bộ, đi chơi, công sở
            |* Có sẵn cho cả nam và nữ
            |* Các thương hiệu: Nike, Adidas, Puma, Bitis, Converse
            |* Khoảng giá: từ 200.000đ đến 20.000.000đ
            |
            |CÁCH HIỂN THỊ SẢN PHẨM:
            |1. Luôn hiển thị hình ảnh sản phẩm nếu có
            |2. Thông tin hiển thị theo thứ tự:
            |   - Tên và hình ảnh sản phẩm
            |   - Giá và khuyến mãi (nếu có)
            |   - Size và màu sắc còn hàng
            |   - Đánh giá và số lượng đã bán
            |   - Đặc điểm nổi bật
            |3. Chỉ hiển thị size và màu còn hàng
            |
            |QUERY HIỆN TẠI: $userQuery
            |${priceRange?.let { "KHOẢNG GIÁ ĐÃ XÁC ĐỊNH: ${formatPrice(it.min)} - ${formatPrice(it.max)}" } ?: ""}
            |
            |Hãy trả lời một cách tự nhiên, thân thiện và luôn giữ tính liền mạch của cuộc trò chuyện.
        """.trimMargin()
    }

    private suspend fun buildProductContext(): String = withContext(Dispatchers.IO) {
        try {
            val categoriesRef = firestore.collection("categories").get().await()
            val brandsRef = firestore.collection("brands").get().await()
            val productsRef = firestore.collection("products").get().await()

            val categories = categoriesRef.documents.mapNotNull { it.getString("name") }
            val brands = brandsRef.documents.mapNotNull { it.getString("name") }
            val priceStats = calculatePriceStatistics(productsRef.documents)

            """
            |THÔNG TIN SẢN PHẨM TRONG KHO:
            |* Tổng số sản phẩm: ${productsRef.size()}
            |* Danh mục: ${categories.joinToString(", ")}
            |* Thương hiệu: ${brands.joinToString(", ")}
            |* Giá thấp nhất: ${formatPrice(priceStats.minPrice)}
            |* Giá cao nhất: ${formatPrice(priceStats.maxPrice)}
            |* Giá trung bình: ${formatPrice(priceStats.avgPrice)}
            |* Các mức giá phổ biến: ${priceStats.popularPriceRanges.joinToString(", ") { "${formatPrice(it.first)} - ${formatPrice(it.second)}" }}
            """.trimMargin()
        } catch (e: Exception) {
            Log.e(TAG, "Error building product context", e)
            ""
        }
    }

    private data class PriceStatistics(
        val minPrice: Long,
        val maxPrice: Long,
        val avgPrice: Long,
        val popularPriceRanges: List<Pair<Long, Long>>
    )

    private fun calculatePriceStatistics(products: List<DocumentSnapshot>): PriceStatistics {
        val prices = products.mapNotNull { doc ->
            doc.getLong("price")
        }

        if (prices.isEmpty()) {
            return PriceStatistics(0, 0, 0, emptyList())
        }

        val minPrice = prices.minOrNull() ?: 0
        val maxPrice = prices.maxOrNull() ?: 0
        val avgPrice = prices.average().toLong()

        // Tạo các khoảng giá phổ biến
        val priceRanges = mutableListOf<Pair<Long, Long>>()
        val range = maxPrice - minPrice
        val step = range / 4 // Chia thành 4 khoảng giá

        for (i in 0..3) {
            val start = minPrice + (step * i)
            val end = if (i == 3) maxPrice else minPrice + (step * (i + 1))
            val count = prices.count { it in start..end }
            if (count > prices.size / 10) { // Chỉ lấy khoảng giá có ít nhất 10% sản phẩm
                priceRanges.add(Pair(start, end))
            }
        }

        return PriceStatistics(minPrice, maxPrice, avgPrice, priceRanges)
    }

    private suspend fun findSimilarProducts(product: ProductModel): List<ProductModel> = withContext(Dispatchers.IO) {
        try {
            val productsRef = firestore.collection("products")
            val query = productsRef
                .whereEqualTo("categoryId", product.categoryId)
                .whereLessThanOrEqualTo("price", product.price * 1.2) // Giá cao hơn 20%
                .whereGreaterThanOrEqualTo("price", product.price * 0.8) // Giá thấp hơn 20%
                .limit(5)

            val snapshot = query.get().await()
            return@withContext snapshot.documents
                .mapNotNull { doc -> doc.toObject(ProductModel::class.java)?.copy(id = doc.id) }
                .filter { it.id != product.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding similar products", e)
            emptyList()
        }
    }

    private suspend fun enhanceProductContext(product: ProductModel): String = withContext(Dispatchers.IO) {
        try {
            // Lấy thông tin category
            val category = firestore.collection("categories")
                .document(product.categoryId)
                .get()
                .await()
            
            // Lấy thông tin brand
            val brand = firestore.collection("brands")
                .document(product.brandId)
                .get()
                .await()
            // Lấy thông tin stock (size và màu sắc) của sản phẩm
            val stock = firestore.collection("products")
                .document(product.id)
                .collection("stock")
                .get()
                .await()

            // Phân tích description
            val features = extractKeyFeatures(product.description)
            
            // Phân tích size và màu sắc có sẵn
            val stockItems = stock.documents.mapNotNull { doc ->
                Triple(
                    doc.getLong("size")?.toInt() ?: 0,
                    doc.getString("color") ?: "",
                    doc.getLong("quantity")?.toInt() ?: 0
                )
            }
            
            val availableSizes = stockItems
                .filter { it.third > 0 } // Chỉ lấy những size còn hàng
                .map { it.first }
                .distinct()
                .sorted()
            
            val availableColors = stockItems
                .filter { it.third > 0 } // Chỉ lấy những màu còn hàng
                .map { it.second }
                .distinct()

            // Tính toán tổng số lượng hàng tồn kho
            val totalStock = stockItems.sumOf { it.third }
            
            // Tính toán độ phổ biến
            val popularity = calculatePopularity(product.rating, product.sold)
            Log.d(TAG, "Product popularity: {${product.images}}  " )
            
            """
            |[PRODUCT_CONTEXT]
            |Tên: ${product.name}
            |Danh mục: ${category.getString("name") ?: product.categoryId}
            |Thương hiệu: ${brand.getString("name") ?: product.brandId}
            |Giá: ${formatPrice(product.price.toLong())}
            |${if (product.discount > 0) "Giảm giá: ${product.discount}%" else ""}
            |Đánh giá: ${product.rating}/5 (${product.sold} đã bán)
            |
            |Anh mô tả: ${product.images.firstOrNull()}
            |
            |Đặc điểm nổi bật:
            |${features.joinToString("\n") { "- $it" }}
            |
            |Size có sẵn: ${availableSizes.joinToString(", ")}
            |Màu sắc: ${availableColors.joinToString(", ")}
            |
            |Độ phổ biến: $popularity
            |[/PRODUCT_CONTEXT]
            """.trimMargin()
        } catch (e: Exception) {
            Log.e(TAG, "Error enhancing product context", e)
            return@withContext """
                |[PRODUCT_CONTEXT]
                |Tên: ${product.name}
                |Danh mục: ${product.categoryId}
                |Thương hiệu: ${product.brandId}
                |Giá: ${formatPrice(product.price.toLong())}
                |Đánh giá: ${product.rating}/5 (${product.sold} đã bán)
                |
                |Đặc điểm nổi bật:
                |Không có thông tin
                |
                |Size có sẵn: Không có thông tin
                |Màu sắc: Không có thông tin
                |
                |Độ phổ biến: Không có thông tin
                |[/PRODUCT_CONTEXT]
            """.trimMargin()

        }
    }

    private fun extractKeyFeatures(description: String): List<String> {
        val features = mutableListOf<String>()
        val normalizedDesc = description.lowercase()
        
        // Patterns cho từng loại thông tin
        val patterns = mapOf(
            "Chất liệu" to listOf(
                "(được làm từ|chất liệu|sử dụng) (.*?)(\\.|$)",
                "(da|vải|cao su|canvas|mesh|knit|leather|fabric) (.*?)(\\.|$)",
                "chất liệu (.*?) (cao cấp|thoáng khí|bền bỉ|mềm mại)"
            ),
            "Thiết kế" to listOf(
                "(thiết kế|kiểu dáng) (.*?)(\\.|$)",
                "phong cách (.*?) (hiện đại|trẻ trung|năng động|thanh lịch)",
                "(đường may|họa tiết|logo|điểm nhấn) (.*?)(\\.|$)"
            ),
            "Đối tượng" to listOf(
                "(phù hợp|thích hợp|dành cho) (.*?)(\\.|$)",
                "(nam|nữ|unisex|người lớn|trẻ em) (.*?)(\\.|$)",
                "phong cách (.*?) (năng động|công sở|thể thao)"
            ),
            "Công nghệ" to listOf(
                "(công nghệ|technology|tech) (.*?)(\\.|$)",
                "(đế giày|lót giày|upper) (.*?)(\\.|$)",
                "(chống trượt|kháng khuẩn|thoáng khí) (.*?)(\\.|$)"
            ),
            "Mục đích sử dụng" to listOf(
                "(sử dụng|dùng|mang) (cho|để|khi) (.*?)(\\.|$)",
                "(đi làm|đi học|tập gym|chạy bộ|đi chơi) (.*?)(\\.|$)",
                "(trong|ngoài) (.*?) (trời|nhà|môi trường)"
            )
        )

        // Phân tích theo từng pattern
        patterns.forEach { (category, patternList) ->
            patternList.forEach { pattern ->
                val regex = Regex(pattern)
                val matches = regex.findAll(normalizedDesc)
                matches.forEach { match ->
                    val feature = match.groupValues[0].trim()
                    if (feature.length >= 10) { // Chỉ lấy các đặc điểm có ý nghĩa
                        features.add("[$category] $feature")
                    }
                }
            }
        }

        return features.distinct().take(7) // Lấy 7 đặc điểm quan trọng nhất
    }

    private fun calculatePopularity(rating: Double, sold: Int): String {
        // Tính điểm phổ biến (0-100)
        val popularityScore = ((rating * 10) + when (sold) {
            in 0..10 -> 10.0
            in 11..50 -> 20.0
            in 51..200 -> 40.0
            in 201..500 -> 60.0
            in 501..1000 -> 80.0
            else -> 100.0
        }).toInt()

        return when (popularityScore) {
            in 0..30 -> "Mới ra mắt"
            in 31..50 -> "Bình thường"
            in 51..70 -> "Khá phổ biến"
            in 71..90 -> "Phổ biến"
            else -> "Rất phổ biến"
        }
    }

    suspend fun generateResponse(prompt: String): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            val priceRange = extractPriceRange(prompt)
            val productContext = buildProductContext()
            val systemPrompt = buildSystemPrompt(prompt, priceRange)
            
            // Kiểm tra nếu prompt liên quan đến sản phẩm
            if (isProductRelatedQuery(prompt)) {
                val products = searchProducts(prompt, priceRange)
                if (products.isNotEmpty()) {
                    val response = buildContextualResponse(prompt, products)
                    updateContext(prompt, response)
                    return@withContext AIResult.Success(response)
                } else {
                    val response = generateSearchSuggestions(prompt, priceRange)
                    updateContext(prompt, response)
                    return@withContext AIResult.Success(response)
                }
            }
            
            // Xử lý các câu hỏi thông thường
            val response = generativeModel.generateContent(
                """
                |$systemPrompt
                |
                |CONTEXT HIỆN TẠI:
                |Câu hỏi trước: ${conversationContext.lastQuery}
                |Trả lời trước: ${conversationContext.lastResponse}
                |Chủ đề: ${conversationContext.currentTopic}
                |
                |Hãy trả lời câu hỏi của khách hàng một cách tự nhiên và liền mạch, 
                |dựa trên ngữ cảnh cuộc trò chuyện và thông tin đã trao đổi trước đó.
                """.trimMargin()
            ).text ?: ""
            
            if (response.isNotEmpty()) {
                updateContext(prompt, response)
                AIResult.Success(response)
            } else {
                AIResult.Error(Exception("No response generated"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            AIResult.Error(e)
        }
    }

    private fun generateProductComparison(products: List<ProductModel>): String {
        if (products.isEmpty()) return ""

        val comparison = StringBuilder("\n💡 PHÂN TÍCH CHI TIẾT SẢN PHẨM:\n\n")

        // Phân loại sản phẩm theo phân khúc giá
        val priceCategories = products.groupBy { product ->
            when {
                product.price <= 2000000 -> "Phân khúc phổ thông"
                product.price <= 4000000 -> "Phân khúc trung cấp"
                else -> "Phân khúc cao cấp"
            }
        }

        // Phân tích theo phân khúc giá
        comparison.append("💰 PHÂN TÍCH THEO GIÁ:\n")
        priceCategories.forEach { (category, productList) ->
            comparison.append("• $category:\n")
            productList.forEach { product ->
                val priceValue = formatPrice(product.price.toLong())
                val discountInfo = if (product.discount > 0) " (Giảm ${product.discount}%)" else ""
                comparison.append("  - ${product.name}: $priceValue$discountInfo\n")
            }
        }

        // Phân tích đánh giá và độ phổ biến
        comparison.append("\n⭐ ĐÁNH GIÁ VÀ ĐỘ PHỔ BIẾN:\n")
        val bestRated = products.maxByOrNull { it.rating }
        val mostSold = products.maxByOrNull { it.sold }
        
        bestRated?.let {
            comparison.append("• Sản phẩm được đánh giá cao nhất:\n")
            comparison.append("  - ${it.name}: ${it.rating}/5 sao")
            if (it.sold > 0) {
                comparison.append(" | Đã bán: ${it.sold}")
            }
            comparison.append("\n")
        }

        mostSold?.let {
            if (it.sold > 0) {
                comparison.append("• Sản phẩm bán chạy nhất:\n")
                comparison.append("  - ${it.name}: Đã bán ${it.sold} | Đánh giá: ${it.rating}/5 sao\n")
            }
        }

        // Phân tích size và màu sắc
        comparison.append("\n👟 SIZE VÀ MÀU SẮC:\n")
        products.forEach { product ->
            comparison.append("• ${product.name}:\n")
            comparison.append("  - Size có sẵn: ${product.stock.filter { it.quantity > 0 }.map { it.size }.distinct().sorted().joinToString(", ")}\n")
            comparison.append("  - Màu sắc: ${product.stock.filter { it.quantity > 0 }.map { it.color }.distinct().joinToString(", ")}\n")
        }

        // Đề xuất theo mục đích sử dụng
        comparison.append("\n🎯 GỢI Ý THEO MỤC ĐÍCH SỬ DỤNG:\n")
        products.forEach { product ->
            val features = extractKeyFeatures(product.description)
            val usageFeatures = features.filter { it.contains("[Mục đích sử dụng]") }
            if (usageFeatures.isNotEmpty()) {
                comparison.append("• ${product.name}:\n")
                usageFeatures.forEach { feature ->
                    comparison.append("  - ${feature.substringAfter("]").trim()}\n")
                }
            }
        }

        // Tổng kết và đề xuất
        comparison.append("\n💎 TỔNG KẾT VÀ ĐỀ XUẤT:\n")
        comparison.append("• Sản phẩm tốt nhất về giá/chất lượng: ")
        val bestValueProduct = products.maxByOrNull { (it.rating * 5 + it.sold) / it.price.toDouble() }
        bestValueProduct?.let {
            comparison.append("${it.name} (Đánh giá: ${it.rating}/5, Giá: ${formatPrice(it.price.toLong())})\n")
        }

        comparison.append("• Sản phẩm đáng chú ý:\n")
        products.filter { it.discount > 20 || it.rating >= 4.0 || it.sold > 100 }.forEach { product ->
            val highlights = mutableListOf<String>()
            if (product.discount > 20) highlights.add("Giảm giá lớn ${product.discount}%")
            if (product.rating >= 4.0) highlights.add("Đánh giá cao ${product.rating}/5")
            if (product.sold > 100) highlights.add("Bán chạy ${product.sold} sản phẩm")
            
            if (highlights.isNotEmpty()) {
                comparison.append("  - ${product.name}: ${highlights.joinToString(" | ")}\n")
            }
        }

        return comparison.toString()
    }

    private fun isProductSuggestionQuery(prompt: String): Boolean {
        val suggestionKeywords = listOf(
            "gợi ý", "suggest", "recommend", "đề xuất",
            "tìm giúp", "tìm cho", "cho xem", "hiển thị",
            "tìm sản phẩm", "tìm giày", "xem giày",
            "muốn mua", "cần mua", "tư vấn", "tầm", "khoảng"
        )
        return suggestionKeywords.any { keyword ->
            prompt.lowercase().contains(keyword.lowercase())
        }
    }

    private suspend fun searchProducts(query: String, priceRange: PriceRange? = null): List<ProductModel> = withContext(Dispatchers.IO) {
        try {
            val queryWords = normalizeSearchQuery(query)
            val productsRef = firestore.collection("products")
            
            // Nhận diện tìm kiếm sản phẩm cụ thể dựa trên pattern câu hỏi
            val isSpecificSearch = query.lowercase().let { q ->
                // Pattern cho câu hỏi về sản phẩm cụ thể
                val patterns = listOf(
                    "thông tin.*về.*giày.*",           // "thông tin về giày X"
                    "cho.*biết.*về.*giày.*",           // "cho biết về giày X"
                    "giày.*có.*gì.*đặc biệt",          // "giày X có gì đặc biệt"
                    "chi tiết.*giày.*",                // "chi tiết giày X"
                    "mô tả.*giày.*",                   // "mô tả giày X"
                    "đặc điểm.*giày.*",                // "đặc điểm giày X"
                    "giới thiệu.*giày.*"               // "giới thiệu giày X"
                )
                
                patterns.any { pattern -> 
                    q.matches(Regex(pattern, RegexOption.IGNORE_CASE))
                }
            }

            // Tạo các filter cơ bản
            var baseQuery: Query = productsRef
            
            // Áp dụng filter giá nếu có
            if (priceRange != null) {
                baseQuery = baseQuery
                    .whereGreaterThanOrEqualTo("price", priceRange.min)
                    .whereLessThanOrEqualTo("price", priceRange.max)
            }

            val snapshot = baseQuery.get().await()
            
            // Phân tích và cho điểm từng sản phẩm
            val scoredProducts = snapshot.documents.mapNotNull { doc ->
                val product = doc.toObject(ProductModel::class.java)?.copy(id = doc.id)
                if (product != null) {
                    val score = calculateProductScore(product, queryWords)
                    if (score > 0) ProductWithScore(product, score) else null
                } else null
            }
            .sortedByDescending { it.score }

            return@withContext if (isSpecificSearch && scoredProducts.isNotEmpty()) {
                // Nếu là tìm sản phẩm cụ thể và có kết quả phù hợp, chỉ trả về sản phẩm điểm cao nhất
                scoredProducts.take(1).map { it.product }
            } else {
                // Nếu là tìm kiếm chung hoặc không tìm thấy sản phẩm phù hợp, trả về nhiều sản phẩm
                scoredProducts.take(7).map { it.product }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching products", e)
            emptyList()
        }
    }

    private data class ProductWithScore(
        val product: ProductModel,
        val score: Double
    )

    private fun normalizeSearchQuery(query: String): List<String> {
        return query.lowercase()
            .replace(Regex("[^\\w\\s]"), " ")
            .split(" ")
            .filter { it.length > 2 } // Bỏ qua các từ quá ngắn
    }

    private fun calculateProductScore(product: ProductModel, queryWords: List<String>): Double {
        var score = 0.0
        val productText = """
            ${product.name}
            ${product.description}
            ${product.categoryId}
            ${product.brandId}
        """.lowercase()

        // Tính điểm cho match chính xác tên sản phẩm
        val productNameWords = product.name.lowercase().split(" ")
        
        // Tìm chuỗi từ liên tiếp dài nhất match với tên sản phẩm
        val maxConsecutiveMatches = queryWords.windowed(queryWords.size, partialWindows = true)
            .maxOfOrNull { window ->
                var matchCount = 0
                var maxMatchCount = 0
                productNameWords.forEach { word ->
                    if (window.any { it in word }) {
                        matchCount++
                        maxMatchCount = maxOf(maxMatchCount, matchCount)
                    } else {
                        matchCount = 0
                    }
                }
                maxMatchCount
            } ?: 0

        // Cho điểm dựa trên số từ match liên tiếp
        score += maxConsecutiveMatches * 15.0

        // Tính điểm cho các match riêng lẻ
        queryWords.forEach { word ->
            when {
                productNameWords.any { it.contains(word) } -> score += 10.0
                product.description.lowercase().contains(word) -> score += 5.0
                product.categoryId.lowercase().contains(word) -> score += 3.0
                product.brandId.lowercase().contains(word) -> score += 3.0
            }
        }

        // Điểm cho đánh giá và doanh số
        score += (product.rating * 2)
        score += when (product.sold) {
            in 0..10 -> 1.0
            in 11..50 -> 2.0
            in 51..200 -> 3.0
            in 201..500 -> 4.0
            else -> 5.0
        }

        // Điểm cho tồn kho
        if (product.stock.any { it.quantity > 0 }) {
            score += 2.0
        }

        return score
    }

    private fun formatPrice(price: Long): String {
        return when {
            price >= 1_000_000 -> "${price / 1_000_000} triệu"
            price >= 1_000 -> "${price / 1_000}k"
            else -> price.toString()
        }
    }

    private fun generateSearchSuggestions(prompt: String, priceRange: PriceRange?): String {
        val priceRangeText = when {
            priceRange == null -> "sản phẩm"
            priceRange.max == Long.MAX_VALUE -> "sản phẩm trên ${formatPrice(priceRange.min)}"
            priceRange.min == 0L -> "sản phẩm dưới ${formatPrice(priceRange.max)}"
            else -> "sản phẩm từ ${formatPrice(priceRange.min)} đến ${formatPrice(priceRange.max)}"
        }

        return """
            |Để tìm $priceRangeText phù hợp nhất với bạn, cho tôi biết thêm:
            |
            |* Bạn thích phong cách nào? (năng động, thanh lịch, basic, cá tính)
            |* Bạn có ưu tiên thương hiệu nào không? (Nike, Adidas, Puma, v.v.)
            |* Bạn thường xuyên sử dụng giày trong hoàn cảnh nào? (đi học, đi làm, tập thể thao)
            |
            |Tôi sẽ giúp bạn tìm được sản phẩm ưng ý nhất!
        """.trimMargin()
    }

    private fun isProductRelatedQuery(prompt: String): Boolean {
        val productKeywords = listOf(
            "giày", "sản phẩm", "sản phẩm giày", "thể thao", "đi chơi", "công sở", "chạy bộ", "giá", "tiền", "size", "màu", "thương hiệu", "hãng", "phong cách", "style"
        )
        return productKeywords.any { keyword ->
            prompt.lowercase().contains(keyword.lowercase())
        }
    }

    companion object {
        private const val TAG = "VertexAIService"
        
        // Training categories for better product matching
        private val CATEGORIES = mapOf(
            "thể thao" to listOf("running", "training", "gym", "sports"),
            "đi chơi" to listOf("casual", "lifestyle", "sneakers"),
            "công sở" to listOf("formal", "business", "office"),
            "chạy bộ" to listOf("running", "jogging", "athletics")
        )
        
        private val STYLES = mapOf(
            "năng động" to listOf("sporty", "athletic", "casual"),
            "thanh lịch" to listOf("formal", "elegant", "classic"),
            "basic" to listOf("simple", "minimal", "casual"),
            "cá tính" to listOf("unique", "trendy", "streetwear")
        )
    }
} 