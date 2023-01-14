package ch.yass.game.dto

enum class RankType {
    SPECIAL,
    NORMAL
}

enum class Rank(type: RankType) {
    SIX(RankType.NORMAL),
    SEVEN(RankType.NORMAL),
    EIGHT(RankType.NORMAL),
    NINE(RankType.NORMAL),
    TEN(RankType.NORMAL),
    JACK(RankType.NORMAL),
    QUEEN(RankType.NORMAL),
    KING(RankType.NORMAL),
    ACE(RankType.NORMAL),

    HELLO(RankType.SPECIAL)
}