package ch.yass.game.api

import java.util.UUID

data class JoinGameResponse(
    val gameUUID: UUID,
//    val position: Position, // North, South, e.t.c
//    val hand: List<Card>, // List of cards
//    val players: List<Player>, // list of player names and positions with cards (if any played)
)