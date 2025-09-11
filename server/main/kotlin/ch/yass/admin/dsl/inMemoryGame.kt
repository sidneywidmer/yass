package ch.yass.admin.dsl

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private object InMemoryIdGenerator {
    private val gameIds = AtomicInteger(100)
    private val playerIds = AtomicInteger(100)
    private val seatIds = AtomicInteger(100)
    private val handIds = AtomicInteger(100)
    private val trickIds = AtomicInteger(100)

    fun nextGameId() = gameIds.getAndIncrement()
    fun nextPlayerId() = playerIds.getAndIncrement()
    fun nextSeatId() = seatIds.getAndIncrement()
    fun nextHandId() = handIds.getAndIncrement()
    fun nextTrickId() = trickIds.getAndIncrement()
}

fun inMemoryGame(lambda: GameStateBuilder.() -> Unit): GameState {
    val state = GameStateBuilder().apply(lambda).build()
    val now = LocalDateTime.now(ZoneOffset.UTC)

    val game = Game(
        id = InMemoryIdGenerator.nextGameId(),
        uuid = UUID.fromString(UUID.randomUUID().toString()),
        createdAt = now,
        updatedAt = now,
        code = (1..5).map { ('A'..'Z').random() }.joinToString(""),
        settings = GameSettings(false, false, false, false, state.settings.wcType, state.settings.wcValue),
        status = GameStatus.RUNNING
    )

    val playersAndSeats = state.players.map { playerDSL ->
        val player = InternalPlayer(
            id = InMemoryIdGenerator.nextPlayerId(),
            uuid = UUID.fromString(UUID.randomUUID().toString()),
            oryUuid = null,
            name = playerDSL.name,
            bot = playerDSL.bot,
            anonToken = null,
            createdAt = now,
            updatedAt = now
        )

        val seat = Seat(
            id = InMemoryIdGenerator.nextSeatId(),
            uuid = UUID.fromString(UUID.randomUUID().toString()),
            createdAt = now,
            updatedAt = now,
            playerId = player.id,
            gameId = game.id,
            position = playerDSL.position,
            status = if (playerDSL.bot) SeatStatus.BOT else SeatStatus.CONNECTED,
            rejoinedAt = null,
            playerPing = now
        )

        player to seat
    }

    val allPlayers = playersAndSeats.map { it.first }
    val seats = playersAndSeats.map { it.second }

    val hands = state.hands.map { handDSL ->
        val startingPosition = handDSL.positions().firstOrNull { it.start }?.position ?: Position.NORTH

        Hand(
            id = InMemoryIdGenerator.nextHandId(),
            uuid = UUID.fromString(UUID.randomUUID().toString()),
            createdAt = now,
            updatedAt = now,
            startingPosition = startingPosition,
            gameId = game.id,
            trump = handDSL.trump,
            gschobe = handDSL.gschobe,
            north = interpretCards(handDSL.north?.cards),
            east = interpretCards(handDSL.east?.cards),
            south = interpretCards(handDSL.south?.cards),
            west = interpretCards(handDSL.west?.cards),
            northWeise = emptyList(),
            eastWeise = emptyList(),
            southWeise = emptyList(),
            westWeise = emptyList()
        )
    }

    val tricks = mutableListOf<Trick>()
    state.hands.forEachIndexed { handIndex, handDSL ->
        val handId = hands[handIndex].id
        if (handDSL.tricks.isEmpty()) {
            tricks.add(
                Trick(
                    id = InMemoryIdGenerator.nextTrickId(),
                    uuid = UUID.fromString(UUID.randomUUID().toString()),
                    createdAt = now,
                    updatedAt = now,
                    handId = handId,
                    north = null,
                    east = null,
                    south = null,
                    west = null
                )
            )
        }

        handDSL.tricks.forEach { trickDSL ->
            tricks.add(
                Trick(
                    id = InMemoryIdGenerator.nextTrickId(),
                    uuid = UUID.fromString(UUID.randomUUID().toString()),
                    createdAt = now,
                    updatedAt = now,
                    handId = handId,
                    north = interpretCard(trickDSL.north),
                    east = interpretCard(trickDSL.east),
                    south = interpretCard(trickDSL.south),
                    west = interpretCard(trickDSL.west)
                )
            )
        }
    }

    return GameState(game, allPlayers, seats, hands.reversed(), tricks.reversed(), inMemory = true)
}