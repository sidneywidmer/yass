package ch.yass.game.dto

enum class State {
    WAITING_FOR_PLAYERS,
    PLAY_CARD,
    PLAY_CARD_BOT,
    SCHIEBE,
    SCHIEBE_BOT,
    WEISEN_FIRST, // Players bid their highest Weis
    WEISEN_SECOND, // Team with higher WEISEN_FIRST can add additional Weise
    TRUMP,
    TRUMP_BOT,
    NEW_TRICK,
    NEW_HAND,
    FINISHED,
}