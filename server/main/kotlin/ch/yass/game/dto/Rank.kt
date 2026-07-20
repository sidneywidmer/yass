package ch.yass.game.dto

// Good to know: don't use minBy/maxBy over these and always the cardValue function
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