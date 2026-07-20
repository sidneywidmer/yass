package ch.yass.game.dto


enum class Suit {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES;

    companion object {
        fun regular(): List<Suit> = listOf(CLUBS, DIAMONDS, HEARTS, SPADES)
    }
}
