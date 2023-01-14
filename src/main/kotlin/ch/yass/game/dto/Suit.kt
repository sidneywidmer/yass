package ch.yass.game.dto


enum class SuitType {
    SPECIAL,
    NORMAL
}

enum class Suit(type: SuitType) {
    CLUBS(SuitType.NORMAL),
    DIAMONDS(SuitType.NORMAL),
    HEARTS(SuitType.NORMAL),
    SPADES(SuitType.NORMAL),

    WELCOME(SuitType.SPECIAL);
}
