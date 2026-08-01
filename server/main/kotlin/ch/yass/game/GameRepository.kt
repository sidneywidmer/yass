package ch.yass.game

import arrow.core.raise.Raise
import ch.yass.core.error.GameAlreadyFull
import ch.yass.core.error.GameNotFound
import ch.yass.core.error.GameWithCodeNotFound
import ch.yass.core.error.SeatNotFound
import ch.yass.core.helper.toDbJson
import ch.yass.db.tables.references.*
import ch.yass.game.api.internal.DailyGame
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.api.internal.NewSeat
import ch.yass.game.dto.*
import ch.yass.game.dto.db.*
import ch.yass.game.dto.db.Game
import ch.yass.game.engine.botId
import ch.yass.game.engine.botName
import ch.yass.game.engine.randomFreePosition
import org.jooq.DSLContext
import org.jooq.Records.mapping
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random

class GameRepository(private val db: DSLContext) {

    fun createGame(settings: GameSettings, kind: GameKind, seed: Long = Random.nextLong()): Game {
        return db.insertInto(
            GAME,
            GAME.UUID,
            GAME.CODE,
            GAME.CREATED_AT,
            GAME.UPDATED_AT,
            GAME.SEED,
            GAME.SETTINGS,
            GAME.STATUS,
            GAME.KIND
        )
            .values(
                UUID.randomUUID().toString(),
                (1..5).map { ('A'..'Z').random() }.joinToString(""), // TODO: Handle collisions
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                seed,
                toDbJson(settings),
                GameStatus.RUNNING.name,
                kind.name
            )
            .returningResult(GAME)
            .fetchOne(mapping(Game::fromRecord))!!
    }

    fun updateSettings(game: Game, settings: GameSettings): Game =
        db.update(GAME)
            .set(GAME.SETTINGS, toDbJson(settings))
            .set(GAME.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(GAME.ID.eq(game.id))
            .returningResult(GAME)
            .fetchOne(mapping(Game::fromRecord))!!

    context(_: Raise<GameAlreadyFull>)
    fun takeASeat(game: Game, player: InternalPlayer, position: Position? = null): Seat {
        val seats = getSeats(game)
        val maybeAlreadySeated = seats.firstOrNull { it.playerId == player.id }
        return maybeAlreadySeated?.let { rejoinSeat(it) }
            ?: run {
                val p = position ?: randomFreePosition(seats)
                createSeat(NewSeat(game, player, p))
            }
    }

    /**
     * Collect all the info we have about a game and pass it to the engine which is responsible
     * to interpret the given data.
     */
    fun getState(game: Game): GameState {
        // Not a lot of changes on the game normally, BUT it could be finished so we need to re-fetch
        val refreshedGame = refresh(game)

        // Fake an id if this seat belongs to a bot
        val seats = getSeats(refreshedGame)
            .map { seat -> seat.takeIf { it.status == SeatStatus.BOT }?.copy(playerId = botId(seat.position)) ?: seat }

        val hands = getHands(refreshedGame)
        val tricks = getTricks(hands.map { it.id })
        val players = getPlayers(seats.filter { it.status != SeatStatus.BOT }) // Exclude Bots, you won't find anything
        val bots = seats
            .filter { it.status == SeatStatus.BOT }
            .map {
                InternalPlayer(
                    id = botId(it.position),
                    uuid = UUID.randomUUID(),
                    oryUuid = null,
                    name = botName(it.position),
                    bot = true,
                    anonToken = null,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }

        return GameState(refreshedGame, players + bots, seats, hands, tricks)
    }

    context(r: Raise<GameWithCodeNotFound>)
    fun getByCode(code: String): Game =
        db.selectFrom(GAME)
            .where(GAME.CODE.eq(code))
            .fetchOneInto(Game::class.java) ?: r.raise(GameWithCodeNotFound(code))

    context(r: Raise<GameNotFound>)
    fun getByUUID(uuid: String): Game {
        val game = db.selectFrom(GAME)
            .where(GAME.UUID.eq(uuid))
            .fetchOneInto(Game::class.java)

        return game ?: r.raise(GameNotFound(uuid))
    }

    /**
     * Games the player is seated at and could still rejoin. STALE ones count as well, they are only
     * waiting for someone to come back (see [rejoinSeat]).
     */
    fun getRunningGamesForPlayer(player: InternalPlayer): List<Game> =
        db.select(GAME)
            .from(SEAT)
            .join(GAME).on(SEAT.GAME_ID.eq(GAME.ID))
            .where(SEAT.PLAYER_ID.eq(player.id))
            .and(GAME.STATUS.`in`(GameStatus.RUNNING.name, GameStatus.STALE.name))
            .orderBy(GAME.CREATED_AT.desc())
            .fetch(mapping(Game::fromRecord))

    fun getDailyGameForPlayer(player: InternalPlayer, start: LocalDateTime, end: LocalDateTime): Game? =
        db.select(GAME)
            .from(SEAT)
            .join(GAME).on(SEAT.GAME_ID.eq(GAME.ID))
            .where(GAME.KIND.eq(GameKind.DAILY.name))
            .and(GAME.CREATED_AT.ge(start))
            .and(GAME.CREATED_AT.lt(end))
            .and(SEAT.PLAYER_ID.eq(player.id))
            .fetchOne(mapping(Game::fromRecord))

    /**
     * Every daily challenge that was played to the end within the given window, abandoned runs never make it
     * onto the leaderboard. Only the seat of the human player is looked up, the bots around them are
     * irrelevant. Hands and tricks come back in the same order as in [getState] since the engine expects the
     * newest one first.
     */
    fun getFinishedDailyGames(start: LocalDateTime, end: LocalDateTime): List<DailyGame> {
        val games = db.selectFrom(GAME)
            .where(GAME.KIND.eq(GameKind.DAILY.name))
            .and(GAME.STATUS.eq(GameStatus.FINISHED.name))
            .and(GAME.CREATED_AT.ge(start))
            .and(GAME.CREATED_AT.lt(end))
            .fetch(Game::fromRecord)

        if (games.isEmpty()) return emptyList()

        val gameIds = games.map { it.id }

        // Bot seats have no player id, so the join already leaves us with one row per game
        val seated = db.select(SEAT.GAME_ID, SEAT.POSITION, PLAYER.NAME)
            .from(SEAT)
            .join(PLAYER).on(SEAT.PLAYER_ID.eq(PLAYER.ID))
            .where(SEAT.GAME_ID.`in`(gameIds))
            .fetch()
            .associateBy { it[SEAT.GAME_ID] }

        val handsByGame = db.selectFrom(HAND)
            .where(HAND.GAME_ID.`in`(gameIds))
            .orderBy(HAND.CREATED_AT.desc())
            .fetch(Hand::fromRecord)
            .groupBy { it.gameId }

        val tricksByHand = getTricks(handsByGame.values.flatten().map { it.id }).groupBy { it.handId }

        return games.mapNotNull { game ->
            val seat = seated[game.id] ?: return@mapNotNull null
            val hands = handsByGame[game.id].orEmpty()

            DailyGame(
                game,
                seat[PLAYER.NAME]!!,
                Position.valueOf(seat[SEAT.POSITION]!!),
                hands,
                hands.flatMap { tricksByHand[it.id].orEmpty() }
            )
        }
    }

    fun refresh(game: Game): Game {
        return db.selectFrom(GAME)
            .where(GAME.UUID.eq(game.uuid.toString()))
            .fetchOneInto(Game::class.java)!!
    }

    context(r: Raise<SeatNotFound>)
    fun getBySeatUUID(uuid: String): Game {
        val game = db.select()
            .from(SEAT)
            .join(GAME).on(SEAT.GAME_ID.eq(GAME.ID))
            .where(SEAT.UUID.eq(uuid))
            .fetchOneInto(Game::class.java)

        return game ?: r.raise(SeatNotFound(uuid))
    }

    fun chooseTrump(trump: Trump, hand: Hand): Hand =
        db.update(HAND)
            .set(HAND.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(HAND.TRUMP, trump.name)
            .where(HAND.ID.eq(hand.id))
            .returningResult(HAND)
            .fetchOneInto(Hand::class.java)!!

    fun updateSeatStatus(seat: Seat, status: SeatStatus): Seat =
        db.update(SEAT)
            .set(SEAT.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.STATUS, status.name)
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!

    fun schiebe(gschobe: Gschobe, hand: Hand): Hand =
        db.update(HAND)
            .set(HAND.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(HAND.GSCHOBE, gschobe.name)
            .where(HAND.ID.eq(hand.id))
            .returningResult(HAND)
            .fetchOneInto(Hand::class.java)!!

    fun playCard(card: Card, trick: Trick, seat: Seat): Trick =
        db.update(TRICK)
            .set(TRICK.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(seat.trickColumn(), toDbJson(card))
            .where(TRICK.ID.eq(trick.id))
            .returningResult(TRICK)
            .fetchOneInto(Trick::class.java)!!

    fun createTrick(hand: Hand): Trick =
        db.insertInto(TRICK, TRICK.UUID, TRICK.CREATED_AT, TRICK.UPDATED_AT, TRICK.HAND_ID)
            .values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                hand.id
            )
            .returningResult(TRICK)
            .fetchOneInto(Trick::class.java)!!

    fun createHand(hand: NewHand): Hand =
        db.insertInto(
            HAND,
            HAND.UUID,
            HAND.CREATED_AT,
            HAND.UPDATED_AT,
            HAND.GAME_ID,
            HAND.STARTING_POSITION,
            HAND.TRUMP,
            HAND.GSCHOBE,
            HAND.NORTH,
            HAND.EAST,
            HAND.SOUTH,
            HAND.WEST
        )
            .values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                hand.game.id,
                hand.startingPosition.name,
                hand.trump.name,
                hand.gschobe.name,
                toDbJson(hand.positions[Position.NORTH]),
                toDbJson(hand.positions[Position.EAST]),
                toDbJson(hand.positions[Position.SOUTH]),
                toDbJson(hand.positions[Position.WEST]),
            )
            .returningResult(HAND)
            .fetchOne(mapping(Hand::fromRecord))!!

    fun pingSeat(seat: Seat, ping: LocalDateTime): Seat {
        return db.update(SEAT)
            .set(SEAT.PLAYER_PING, ping)
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!
    }

    fun finishGame(game: Game): Game {
        return db.update(GAME)
            .set(GAME.STATUS, GameStatus.FINISHED.name)
            .where(GAME.ID.eq(game.id))
            .returningResult(GAME)
            .fetchOneInto(Game::class.java)!!
    }

    fun getOrCreateDailyChallengeForDay(day: LocalDate): DailyChallenge {
        getDailyChallenge(day)?.let { return it }

        return db.insertInto(
            DAILY_CHALLENGE,
            DAILY_CHALLENGE.UUID,
            DAILY_CHALLENGE.CREATED_AT,
            DAILY_CHALLENGE.UPDATED_AT,
            DAILY_CHALLENGE.DAY,
            DAILY_CHALLENGE.SEED
        )
            .values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                day,
                Random.nextLong()
            )
            .returningResult(DAILY_CHALLENGE)
            .fetchOne(mapping(DailyChallenge::fromRecord))!!
    }

    private fun getDailyChallenge(day: LocalDate): DailyChallenge? =
        db.selectFrom(DAILY_CHALLENGE)
            .where(DAILY_CHALLENGE.DAY.eq(day))
            .fetchOne(DailyChallenge::fromRecord)

    private fun createSeat(seat: NewSeat): Seat {
        return db
            .insertInto(
                SEAT,
                SEAT.UUID,
                SEAT.PLAYER_ID,
                SEAT.GAME_ID,
                SEAT.POSITION,
                SEAT.PLAYER_PING,
                SEAT.STATUS,
                SEAT.CREATED_AT,
                SEAT.UPDATED_AT
            )
            .values(
                UUID.randomUUID().toString(),
                if (seat.player.bot) null else seat.player.id,
                seat.game.id,
                seat.position.toString(),
                LocalDateTime.now(ZoneOffset.UTC), // Everyone gets one free ping, just to avoid false positive PlayerDisconnected events
                if (seat.player.bot) SeatStatus.BOT.name else SeatStatus.CONNECTED.name,
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC)
            )
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!
    }

    private fun getPlayers(seats: List<Seat>): List<InternalPlayer> =
        db.selectFrom(PLAYER)
            .where(PLAYER.ID.`in`(seats.map { it.playerId }))
            .fetchInto(InternalPlayer::class.java)

    private fun getSeats(game: Game): List<Seat> =
        db.selectFrom(SEAT)
            .where(SEAT.GAME_ID.eq(game.id))
            .fetchInto(Seat::class.java)

    private fun getTricks(handIds: List<Int>): List<Trick> =
        db.selectFrom(TRICK)
            .where(TRICK.HAND_ID.`in`(handIds))
            .orderBy(TRICK.CREATED_AT.desc())
            .fetch(Trick::fromRecord)

    private fun getHands(game: Game): List<Hand> =
        db.selectFrom(HAND)
            .where(HAND.GAME_ID.eq(game.id))
            .orderBy(HAND.CREATED_AT.desc())
            .fetch(Hand::fromRecord)

    private fun rejoinSeat(seat: Seat): Seat {
        // In case the game was set to STALE in the meantime...
        db.update(GAME)
            .set(GAME.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(GAME.STATUS, GameStatus.RUNNING.name)
            .where(GAME.ID.eq(seat.gameId))
            .execute()

        return db.update(SEAT)
            .set(SEAT.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.REJOINED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.PLAYER_PING, LocalDateTime.now(ZoneOffset.UTC)) // Avoids race condition with playerPing job
            .set(SEAT.STATUS, SeatStatus.CONNECTED.name)
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!
    }

    fun updateWeise(seat: Seat, hand: Hand, weise: List<Weis>): Hand =
        db.update(HAND)
            .set(HAND.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(seat.weisColumn(), toDbJson(weise))
            .where(HAND.ID.eq(hand.id))
            .returningResult(HAND)
            .fetchOneInto(Hand::class.java)!!

}