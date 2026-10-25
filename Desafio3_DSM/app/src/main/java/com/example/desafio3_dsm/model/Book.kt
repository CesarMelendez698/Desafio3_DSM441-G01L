package com.example.desafio3_dsm.model

import java.io.Serializable

data class Book(
    var id: Long = 0,
    val userId: String,
    var title: String,
    var author: String,
    var genre: String,
    var totalPages: Int,
    var readPages: Int,
    var status: String,
    var rating: Float = 0f,
    var finishDate: Long? = null,
    var finishYear: Int? = null
) : Serializable {

    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((readPages.toDouble() / totalPages.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val isFinished: Boolean
        get() = status.equals("Terminado", ignoreCase = true)
}
