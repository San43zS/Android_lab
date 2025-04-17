package com.example.mobile_lab_android.models

import com.google.firebase.firestore.DocumentId

data class ProductModel(
    @DocumentId var id: String? = null,
    var name: String = "",
    var description: String = "",
    var images: List<String> = emptyList(),
    var isFavorite: Boolean = true
)
