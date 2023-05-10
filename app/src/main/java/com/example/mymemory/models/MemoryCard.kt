package com.example.mymemory.models

data class MemoryCard(
    // represents the uniqueness of the memory card
    val identifier: Int, // Val can not be changed while var can be changed
    var isFaceUp : Boolean = false,
    var isMatched: Boolean = false
)