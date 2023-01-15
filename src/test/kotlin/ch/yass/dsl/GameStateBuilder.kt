package ch.yass.dsl

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.welcomeHand
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class GameStateDSL(val players: List<PlayerDSL>, val hands: List<HandDSL>)
data class PlayerDSL(val name: String, val bot: Boolean, val position: Position)
data class TrickDSL(val north: String?, val east: String?, val south: String?, val west: String?)
data class HandPositionDSL(val cards: String, val start: Boolean = false, val position: Position)
data class HandDSL(
    val trump: Trump?,
    val north: HandPositionDSL,
    val east: HandPositionDSL,
    val south: HandPositionDSL,
    val west: HandPositionDSL,
    val tricks: List<TrickDSL>
) {
    fun positions(): List<HandPositionDSL> {
        return listOfNotNull(north, east, south, west)
    }
}

class GameStateBuilder(
    private var players: List<PlayerDSL> = emptyList(),
    private var hands: List<HandDSL> = emptyList()
) {
    fun players(lambda: PlayersBuilder.() -> Unit) {
        players = PlayersBuilder().apply(lambda).build()
    }

    fun hands(lambda: HandsBuilder.() -> Unit) {
        hands = HandsBuilder().apply(lambda).build()
    }

    fun build() = GameStateDSL(players, hands)
}

class HandsBuilder(
    private var hands: MutableList<HandDSL> = mutableListOf()
) {
    fun hand(lambda: HandBuilder.() -> Unit) {
        val hand = HandBuilder().apply(lambda).build()
        hands.add(hand)
    }

    fun build(): List<HandDSL> = hands
}

class HandBuilder(
    private var trump: Trump? = null,
    private var north: HandPositionDSL? = null,
    private var east: HandPositionDSL? = null,
    private var south: HandPositionDSL? = null,
    private var west: HandPositionDSL? = null,
    private var tricks: MutableList<TrickDSL> = mutableListOf(),
) {
    fun trump(trump: Trump?) {
        this.trump = trump
    }

    fun north(cards: String, start: Boolean = false) {
        this.north = HandPositionDSL(cards, start, Position.NORTH)
    }

    fun east(cards: String, start: Boolean = false) {
        this.east = HandPositionDSL(cards, start, Position.EAST)
    }

    fun south(cards: String, start: Boolean = false) {
        this.south = HandPositionDSL(cards, start, Position.SOUTH)
    }

    fun west(cards: String, start: Boolean = false) {
        this.west = HandPositionDSL(cards, start, Position.WEST)
    }

    fun tricks(lambda: TricksBuilder.() -> Unit) {
        val trick = TricksBuilder().apply(lambda).build()
        this.tricks.add(trick)
    }

    fun build(): HandDSL {
        return HandDSL(trump, north!!, east!!, south!!, west!!, tricks)
    }

}

class TricksBuilder(
    private var trick: TrickDSL? = null
) {
    fun trick(north: String?, east: String?, south: String?, west: String?) {
        this.trick = TrickDSL(north, east, south, west)
    }

    fun build(): TrickDSL {
        return trick!!
    }
}


class PlayersBuilder(
    private var players: MutableList<PlayerDSL> = mutableListOf()
) {
    fun north(name: String, bot: Boolean): Boolean = players.add(PlayerDSL(name, bot, Position.NORTH))
    fun east(name: String, bot: Boolean): Boolean = players.add(PlayerDSL(name, bot, Position.EAST))
    fun south(name: String, bot: Boolean): Boolean = players.add(PlayerDSL(name, bot, Position.SOUTH))
    fun west(name: String, bot: Boolean): Boolean = players.add(PlayerDSL(name, bot, Position.WEST))
    fun build(): List<PlayerDSL> = players
}

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
    val hands = state.hands.mapIndexed { i, h ->
        val startPosition = h.positions().first { it.start }
        val startPlayer = state.players.first { it.position == startPosition.position }
        Hand(
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
    }

    return GameState(game, players, seats, hands, emptyList())
}

fun interpretCards(cards: String?): List<Card> {
    if (cards == null) {
        return emptyList()
    }

    return listOf(
        Card(Suit.WELCOME, Rank.HELLO, "french"),
        Card(Suit.WELCOME, Rank.HELLO, "french"),
        Card(Suit.WELCOME, Rank.HELLO, "french"),
        Card(Suit.WELCOME, Rank.HELLO, "french")
    )
}