package ch.yass.admin.dsl

import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.WinningConditionType

class GameStateBuilder(
    private var players: List<PlayerDSL> = emptyList(),
    private var hands: List<HandDSL> = emptyList(),
    private var settings: GameSettingsDSL = GameSettingsDSL(WinningConditionType.HANDS, 10)
) {
    fun players(lambda: PlayersBuilder.() -> Unit) {
        players =PlayersBuilder().apply(lambda).build()
    }

    fun hands(lambda: HandsBuilder.() -> Unit) {
        hands = HandsBuilder().apply(lambda).build()
    }

    fun settings(lambda: GameSettingsBuilder.() -> Unit) {
        settings = GameSettingsBuilder().apply(lambda).build()
    }

    private fun defaultPlayers(): List<PlayerDSL> = listOf(
        PlayerDSL("ueli-north", false, Position.NORTH),
        PlayerDSL("doris-east", false, Position.EAST),
        PlayerDSL("christoph-south", false, Position.SOUTH),
        PlayerDSL("daniela-west", false, Position.WEST)
    )

    private fun defaultWelcomeHand(): HandDSL = HandDSL(
        trump = Trump.FREESTYLE,
        gschobe = Gschobe.NO,
        north = HandPositionDSL("welcome", true, Position.NORTH),
        east = HandPositionDSL("welcome", false, Position.EAST),
        south = HandPositionDSL("welcome", false, Position.SOUTH),
        west = HandPositionDSL("welcome", false, Position.WEST),
        tricks = listOf(TrickDSL("W6", "W6", "W6", "W6"))
    )

    /**
     * Before building we'll automatically inject some default players and the welcome hand
     * if not explicitly provided. This saves us some boilerplate in our tests.
     */
    fun build(): GameStateDSL {
        val finalPlayers = players.ifEmpty { defaultPlayers() }
        
        val finalHands = when {
            hands.isEmpty() -> listOf(defaultWelcomeHand())
            hands.first().trump != Trump.FREESTYLE -> listOf(defaultWelcomeHand()) + hands
            else -> hands
        }
        
        return GameStateDSL(finalPlayers, finalHands, settings)
    }
}

class HandsBuilder(private var hands: MutableList<HandDSL> = mutableListOf()) {
    fun hand(lambda: HandBuilder.() -> Unit) = hands.add(HandBuilder().apply(lambda).build())

    fun build(): List<HandDSL> = hands
}

class HandBuilder(
    private var trump: Trump = Trump.NONE,
    private var gschobe: Gschobe = Gschobe.NOT_YET,
    private var north: HandPositionDSL? = null,
    private var east: HandPositionDSL? = null,
    private var south: HandPositionDSL? = null,
    private var west: HandPositionDSL? = null,
    private var tricks: MutableList<TrickDSL> = mutableListOf(),
) {
    fun trump(trump: Trump) {
        this.trump = trump
    }

    fun gschobe(gschobe: Gschobe) {
        this.gschobe = gschobe
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
        val tricks = TricksBuilder().apply(lambda).build()
        this.tricks = tricks
    }

    fun build(): HandDSL = HandDSL(trump, gschobe, north, east, south, west, tricks)
}

class TricksBuilder(
    private var tricks: MutableList<TrickDSL> = mutableListOf()
) {
    fun trick(north: String?, east: String?, south: String?, west: String?) {
        this.tricks.add(TrickDSL(north, east, south, west))
    }

    fun build(): MutableList<TrickDSL> = tricks
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

class GameSettingsBuilder(
    private var wcType: WinningConditionType = WinningConditionType.HANDS,
    private var wcValue: Int = 10
) {
    fun wcType(type: WinningConditionType) {
        this.wcType = type
    }

    fun wcValue(value: Int) {
        this.wcValue = value
    }

    fun build() = GameSettingsDSL(wcType, wcValue)
}