package ch.yass.game.dto

enum class State {
    WAITING_FOR_PLAYERS,
    PLAY_CARD,
    PLAY_CARD_BOT,
    SCHIEBE,
    SCHIEBE_BOT,
    WEISEN,
    TRUMP,
    TRUMP_BOT,
    NEW_TRICK,
    NEW_HAND,
    FINISHED,
}