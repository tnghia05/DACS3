package com.eritlab.jexmon.presentation.screens.checkout_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eritlab.jexmon.domain.model.AddressModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShippingAddressViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var addressList by mutableStateOf<List<AddressModel>>(emptyList())
        private set

    var selectedAddressId by mutableStateOf<String?>(null)
        private set

    init {
        loadAddresses()
    }

    fun loadAddresses() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("address")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val addresses = snapshot?.toObjects(AddressModel::class.java) ?: emptyList()
                addressList = addresses

                // Nếu chưa chọn thì tự động chọn địa chỉ mặc định
                if (selectedAddressId == null) {
                    selectedAddressId = addresses.find { it.isDefault }?.id
                }
            }
    }

    fun selectAddress(addressId: String) {
        selectedAddressId = addressId
    }
}
