package ch.yass.game.dto

enum class Rank {
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING,
    ACE;

    companion object {
        fun regular(): List<Rank> = listOf(SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE)
    }
}