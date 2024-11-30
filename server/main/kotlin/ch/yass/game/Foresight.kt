package ch.yass.game

import ch.yass.game.dto.Card

class Foresight {
    var decks: MutableList<List<Card>> = mutableListOf()

    fun nextDeck(): List<Card>? {
        return if (decks.isNotEmpty()) decks.removeAt(0) else null
    }

    fun pushDeck(deck: List<Card>) {
        decks.add(deck)
    }
}