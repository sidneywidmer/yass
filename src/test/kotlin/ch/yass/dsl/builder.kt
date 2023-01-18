package ch.yass.dsl

import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump

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