package com.eritlab.jexmon.presentation.screens.admin.Brand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eritlab.jexmon.domain.model.BrandModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BrandViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _brandList = MutableStateFlow<List<BrandModel>>(emptyList())
    val brandList: StateFlow<List<BrandModel>> = _brandList

    init {
        fetchBrands()
    }

    private fun fetchBrands() {
        firestore.collection("brands").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val brands = it.documents.mapNotNull { doc ->
                    doc.toObject(BrandModel::class.java)?.copy(id = doc.id)
                }
                _brandList.value = brands
            }
        }
    }

    fun addBrand(brand: BrandModel, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("brands").add(brand)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateBrand(brand: BrandModel, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("brands").document(brand.id)
            .set(brand)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteBrand(brandId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("brands").document(brandId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
