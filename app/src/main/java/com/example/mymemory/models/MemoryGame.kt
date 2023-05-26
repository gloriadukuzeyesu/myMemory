package com.example.mymemory.models

import com.example.mymemory.utils.DEFAULT_ICONS

// crate the functionality of the game
class MemoryGame(private val boardSize: BoardSize, private val customImages: List<String>?){

    val cards: List<MemoryCard>
    var numPairsFound = 0

    private var numCardFlips = 0
    private var indexOfSingleSelectedCard: Int? = null // initially null because when starting the game there is not flipped over/ selected card


    init {
        if (customImages == null) {
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs()) // get a list of images. needs to be pairs
            val randomizedImage = (chosenImages + chosenImages).shuffled()
            // each card on the screen will represents its own memory card. from a list of randomizedImage, create meory card for each image using map()
            cards = randomizedImage.map { MemoryCard(it) }
        } else {
            val randomizedImage  = (customImages + customImages).shuffled()
            cards = randomizedImage.map { MemoryCard(it.hashCode(),it) }

        }


    }

    fun flipCard(position: Int) : Boolean {
        numCardFlips++
        val card: MemoryCard = cards[position]
        // three case in memory
        // 0 cards previously flipped over => restore cards + flip over the selected cards
        // 1 card previously flipped over => flip over the selected card + check if images match
        // 2 cards previously flipped over => restore cards + flip over the selected cards
        var foundMatch = false
        if(indexOfSingleSelectedCard == null) {
            restoreCard()
            indexOfSingleSelectedCard = position
        } else {
            // one card previously flipped over
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position) // !! tells the compiler to ignore (forcing it to be a non null int)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp =  !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if(cards[position1].identifier != cards[position2].identifier) {
            // no match
            return false
        }
        // match is found. Then update the state of those cards
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCard() {
        for(card in cards) {
            if(!card.isMatched) {
                card.isFaceUp = false;
            }
        }
    }

    /**
     * will return true, if the numpairs found is equal to the number of pairs of that baord
     */
    fun haveWonGame(): Boolean {
        return  numPairsFound == boardSize.getNumPairs()
    }

    /**
     * verify that the card at that position is faced up.
     */
    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    /**
     * get the number of moves the user has made
     */
    fun getNumMoves(): Int {
        return numCardFlips / 2
    }
}