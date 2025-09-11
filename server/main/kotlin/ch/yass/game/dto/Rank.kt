package ch.yass.game.dto

// Good to know: minBy works because Enums implement Comparable and each item has on ordinal prop
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