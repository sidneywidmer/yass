package ch.yass.game.dto

/**
 * A list of possible trumps. This includes all suites, special jass trumps as well as some only used by the game
 * engine like "FREESTYLE". You should NOT use trump.entries but instead use the playable() helper function
 * to check if a given trump is valid to be played by a player/bot.
 */
enum class Trump {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES,

    OBEABE,
    UNEUFE,

    FREESTYLE,
    NONE;

    companion object {
        fun playable() = listOf(CLUBS, SPADES, HEARTS, DIAMONDS, UNEUFE, OBEABE)
    }

    fun equalsSuit(suit: Suit): Boolean = this.toString() == suit.toString()

    fun toSuit(): Suit? = Suit.regular().firstOrNull { it.toString() == this.toString() }
}