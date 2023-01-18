package ch.yass.helper

import ch.yass.Yass
import ch.yass.core.helper.toDbJson
import ch.yass.db.tables.references.*
import ch.yass.game.dto.db.*
import org.jooq.DSLContext
import org.kodein.di.direct
import org.kodein.di.instance

fun persistGame(game: Game) {
    val db: DSLContext = Yass.container.direct.instance()

    db.insertInto(GAME, GAME.ID, GAME.UUID, GAME.CREATED_AT, GAME.UPDATED_AT, GAME.CODE)
        .values(game.id, game.uuid.toString(), game.createdAt, game.updatedAt, game.code).execute()
}

fun persistPlayers(players: List<Player>) {
    val db: DSLContext = Yass.container.direct.instance()
    players.forEach {
        db.insertInto(PLAYER, PLAYER.ID, PLAYER.UUID, PLAYER.CREATED_AT, PLAYER.UPDATED_AT, PLAYER.NAME)
            .values(it.id, it.uuid.toString(), it.createdAt, it.updatedAt, it.name).execute()
    }
}

fun persistSeats(seats: List<Seat>) {
    val db: DSLContext = Yass.container.direct.instance()
    seats.forEach {
        db.insertInto(
            SEAT,
            SEAT.ID,
            SEAT.UUID,
            SEAT.UPDATED_AT,
            SEAT.CREATED_AT,
            SEAT.PLAYER_ID,
            SEAT.GAME_ID,
            SEAT.POSITION,
            SEAT.REJOINED_AT
        ).values(
            it.id,
            it.uuid.toString(),
            it.updatedAt,
            it.createdAt,
            it.playerId,
            it.gameId,
            it.position.name,
            it.rejoinedAt
        ).execute()
    }
}

fun persistHands(hands: List<Hand>) {
    val db: DSLContext = Yass.container.direct.instance()
    hands.forEach {
        db.insertInto(
            HAND,
            HAND.ID,
            HAND.UUID,
            HAND.CREATED_AT,
            HAND.UPDATED_AT,
            HAND.GAME_ID,
            HAND.STARTING_PLAYER_ID,
            HAND.TRUMP,
            HAND.GSCHOBE,
            HAND.POINTS,
            HAND.NORTH,
            HAND.EAST,
            HAND.SOUTH,
            HAND.WEST
        ).values(
            it.id,
            it.uuid.toString(),
            it.createdAt,
            it.updatedAt,
            it.gameId,
            it.startingPlayerId,
            it.trump?.name,
            it.gschobe,
            it.points,
            toDbJson(it.north),
            toDbJson(it.south),
            toDbJson(it.east),
            toDbJson(it.west)
        ).execute()
    }
}

fun persistTricks(tricks: List<Trick>) {
    val db: DSLContext = Yass.container.direct.instance()
    tricks.forEach {
        db.insertInto(
            TRICK,
            TRICK.ID,
            TRICK.UUID,
            TRICK.CREATED_AT,
            TRICK.UPDATED_AT,
            TRICK.HAND_ID,
            TRICK.WINNER_ID,
            TRICK.POINTS,
            TRICK.NORTH,
            TRICK.EAST,
            TRICK.SOUTH,
            TRICK.WEST
        ).values(
            it.id,
            it.uuid.toString(),
            it.createdAt,
            it.updatedAt,
            it.handId,
            it.winnerId,
            it.points,
            toDbJson(it.north),
            toDbJson(it.south),
            toDbJson(it.east),
            toDbJson(it.west)
        ).execute()
    }
}