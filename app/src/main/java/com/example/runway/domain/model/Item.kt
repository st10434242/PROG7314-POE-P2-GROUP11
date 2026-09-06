package com.example.runway.domain.model

/**
 * The model the app reasons about. Deliberately plain Kotlin: no Room
 * annotations, no Android imports. Everything above the data layer speaks
 * this type, so the database schema can change without touching the UI.
 */
data class Item(
    val id: Long = 0L,
    val title: String,
    val note: String,
)
