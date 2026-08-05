package ch.yass.game.dto

enum class GameStatus {
    RUNNING,
    FINISHED, // Don't confuse with State.FINISHED. This one is just to set the status on the GAME in the DB
    CANCELED
}
