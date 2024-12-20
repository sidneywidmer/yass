package ch.yass.game.dto


enum class Suit {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES,

    WELCOME;

    companion object {
        fun regular(): List<Suit> = listOf(CLUBS, DIAMONDS, HEARTS, SPADES)
    }
}
