package com.example.desafio3_dsm.model

data class YearStats(
    val year: Int,
    val countNovel: Int,
    val countSciFi: Int,
    val countHistory: Int,
    val countSelfHelp: Int,
    val mostReadGenre: String,
    val mostReadGenreCount: Int,
    val totalPagesReadInYear: Int,
    val bestRatedBook: Book?
)
