package ch.yass.game.dto

import ch.yass.game.engine.regularSuits

/**
 * A list of possible trumps. This includes all suites, special jass trumps as well as some only used by the game
 * engine like "FREESTYLE". You should NOT use trump.entries but instead use the regularTrumps() helper function
 * to check if a given trump is valid to be played by a player/bot.
 */
enum class Trump {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES,

    OBEABE,
    UNEUFE,

    FREESTYLE;

    fun equalsSuit(suit: Suit): Boolean {
        return this.toString() == suit.toString()
    }

    fun toSuit(): Suit? = regularSuits().firstOrNull { it.toString() == this.toString() }
}