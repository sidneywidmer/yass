package ch.yass.dsl

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.db.*
import ch.yass.helper.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


/**
 * Quick and dirty, convert ad GameStateDSL to a correct GameState
 * with "db" id's, dates, uuids, e.t.c
 */
fun game(lambda: GameStateBuilder.() -> Unit): GameState {
    val state = GameStateBuilder().apply(lambda).build()
    val game = Game(
        1,
        UUID.randomUUID(),
        LocalDateTime.now(ZoneOffset.UTC),
        LocalDateTime.now(ZoneOffset.UTC),
        "ABCDE"
    )
    val players = state.players.mapIndexed { i, p ->
        Player(
            i + 1,
            UUID.randomUUID(),
            p.name,
            LocalDateTime.now(ZoneOffset.UTC),
            LocalDateTime.now(ZoneOffset.UTC)
        )
    }
    val seats = state.players.mapIndexed { i, p ->
        Seat(
            i + 1,
            UUID.randomUUID(),
            LocalDateTime.now(ZoneOffset.UTC),
            LocalDateTime.now(ZoneOffset.UTC),
            i + 1,
            1,
            p.position,
            null
        )
    }
    val tricks = mutableListOf<Trick>()
    val hands = state.hands.mapIndexed { i, h ->
        val startPosition = h.positions().first { it.start }
        val startPlayer = state.players.first { it.position == startPosition.position }
        val hand = Hand(
            i + 1,
            UUID.randomUUID(),
            LocalDateTime.now(ZoneOffset.UTC),
            LocalDateTime.now(ZoneOffset.UTC),
            state.players.indexOf(startPlayer) + 1,
            1,
            h.trump,
            false, // TODO, maybe add to same as start?
            0,
            interpretCards(h.north.cards),
            interpretCards(h.east.cards),
            interpretCards(h.south.cards),
            interpretCards(h.west.cards),
        )

        h.tricks.forEachIndexed { ti, t ->
            tricks.add(
                Trick(
                    ti + 1,
                    UUID.randomUUID(),
                    LocalDateTime.now(ZoneOffset.UTC),
                    LocalDateTime.now(ZoneOffset.UTC),
                    hand.id,
                    null,
                    0,
                    interpretCard(t.north),
                    interpretCard(t.east),
                    interpretCard(t.south),
                    interpretCard(t.west)
                )
            )
        }


        hand
    }

    persistGame(game)
    persistPlayers(players)
    persistSeats(seats)
    persistHands(hands)
    persistTricks(tricks)

    return GameState(game, players, seats, hands, emptyList())
}

