package ch.yass.admin.dsl

import arrow.core.raise.recover
import ch.yass.Yass
import ch.yass.core.helper.toDbJson
import ch.yass.db.tables.references.*
import ch.yass.game.GameRepository
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.GameSettings
import ch.yass.game.dto.WinningConditionType
import org.jooq.DSLContext
import org.kodein.di.direct
import org.kodein.di.instance
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


/**
 * Quick and dirty, convert ad GameStateDSL to a correct GameState
 * by persisting everything in the DB.
 */
fun game(lambda: GameStateBuilder.() -> Unit): GameState {
    // Dependencies
    val db: DSLContext = Yass.container.direct.instance()
    val repo: GameRepository = Yass.container.direct.instance()

    // DSL
    val state = GameStateBuilder().apply(lambda).build()

    // Persist game, players, seats, hands, tricks
    val gameRecord = db.newRecord(GAME).apply {
        uuid = UUID.randomUUID().toString()
        createdAt = LocalDateTime.now(ZoneOffset.UTC)
        updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        code = (1..5).map { ('A'..'Z').random() }.joinToString("")
        settings = toDbJson(GameSettings(false, false, false, false, WinningConditionType.HANDS, 10))
        store()
    }
    val game = recover({ repo.getByUUID(gameRecord.uuid!!) }, { throw Exception("invalid uuid") })

    val playerMap = state.players.map { p ->
        val player = db.newRecord(PLAYER).apply {
            uuid = UUID.randomUUID().toString()
            createdAt = LocalDateTime.now(ZoneOffset.UTC)
            updatedAt = LocalDateTime.now(ZoneOffset.UTC)
            name = p.name
            bot = p.bot
            store()
        }

        db.newRecord(SEAT).apply {
            uuid = UUID.randomUUID().toString()
            createdAt = LocalDateTime.now(ZoneOffset.UTC)
            updatedAt = LocalDateTime.now(ZoneOffset.UTC)
            playerId = player.id
            gameId = game.id
            position = p.position.name
            rejoinedAt = null
            store()
        }

        player.id!! to p.position
    }

    state.hands.map { h ->
        // Either take a flagged start player, otherwise just take anyone
        val startPosition = h.positions().firstOrNull { it.start }
        val startPlayer = state.players.firstOrNull { it.position == startPosition?.position } ?: state.players.first()

        val hand = db.newRecord(HAND).apply {
            uuid = UUID.randomUUID().toString()
            createdAt = LocalDateTime.now(ZoneOffset.UTC)
            updatedAt = LocalDateTime.now(ZoneOffset.UTC)
            startingPlayerId = playerMap.first { pair -> startPlayer.position == pair.second }.first
            gameId = game.id
            trump = h.trump?.name
            gschobe = h.gschobe.name
            north = toDbJson(interpretCards(h.north.cards))
            east = toDbJson(interpretCards(h.east.cards))
            south = toDbJson(interpretCards(h.south.cards))
            west = toDbJson(interpretCards(h.west.cards))
            store()
        }

        // hand without a single trick is invalid state
        if (h.tricks.isEmpty()) {
            db.newRecord(TRICK).apply {
                uuid = UUID.randomUUID().toString()
                createdAt = LocalDateTime.now(ZoneOffset.UTC)
                updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                handId = hand.id
                store()
            }
        }

        h.tricks.forEach { t ->
            db.newRecord(TRICK).apply {
                uuid = UUID.randomUUID().toString()
                createdAt = LocalDateTime.now(ZoneOffset.UTC)
                updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                handId = hand.id
                north = toDbJson(interpretCard(t.north))
                east = toDbJson(interpretCard(t.east))
                south = toDbJson(interpretCard(t.south))
                west = toDbJson(interpretCard(t.west))
                store()
            }
        }
    }

    return repo.getState(game)
}

