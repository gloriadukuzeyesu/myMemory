package com.example.mymemory.models

import java.io.Serializable

enum class BoardSize (val numCards: Int) : Serializable {

    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object{
        fun getByValue (value: Int) = values().first{it.numCards == value}
    }

    /**
     * returns the widht of the board game depending on  the level  of the game
     */
    fun getWidth() : Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    /**
     * gets the current height of the board game. Known when we know the num cards and width
     */
    fun getHeight() : Int {
        return numCards / getWidth()
    }

    /**
     * how many  unique pairs of cards are there
     */
    fun getNumPairs() : Int {
        return numCards / 2
    }

}