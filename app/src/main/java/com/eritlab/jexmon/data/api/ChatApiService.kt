package com.eritlab.jexmon.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(
    val message: String
)

data class ChatResponse(
    val response: String
)

interface ChatApiService {
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): Response<ChatResponse>
}

class ChatRepository(private val apiService: ChatApiService) {
    suspend fun sendMessage(message: String): Result<String> {
        return try {
            val response = apiService.sendMessage(ChatRequest(message))
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.response)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}