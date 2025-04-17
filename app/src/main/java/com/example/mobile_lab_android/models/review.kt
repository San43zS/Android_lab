package com.example.mobile_lab_android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ReviewModel(
    @DocumentId val id: String? = null,
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Timestamp = Timestamp.now()
)