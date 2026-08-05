package ch.yass.game

import arrow.core.raise.recover
import ch.yass.core.helper.logger
import ch.yass.core.helper.toUUID
import ch.yass.db.tables.references.GAME
import ch.yass.db.tables.references.PLAYER
import ch.yass.db.tables.references.SEAT
import ch.yass.game.dto.GameKind
import ch.yass.game.dto.GameStatus
import ch.yass.game.dto.SeatStatus
import ch.yass.game.engine.zurich
import com.typesafe.config.Config
import org.jobrunr.scheduling.JobScheduler
import org.jobrunr.scheduling.RecurringJobBuilder.aRecurringJob
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset


class JobsService(
    scheduler: JobScheduler,
    private val db: DSLContext,
    private val gameService: GameService,
    private val config: Config
) {
    init {
        // Funny Bug: If I run this locally I never had a problem. As soon es I integrated the CI pipeline
        // I got some strange flaky tests with centrifugo. Turns out since the GitHub runner is not as fast
        // the tests take longer than 5 seconds and since to polling from any clients is happening the player
        // gets "disconnected" - and disconnected players don't receive centrifugo pushes :)
        if (config.getString("environment") != "test") {
            val playerPing = aRecurringJob()
                .withId("PLAYER_PING_JOB")
                .withInterval(Duration.ofSeconds(10))
                .withJobLambda { playerPing() }

            val cancelStaleGames = aRecurringJob()
                .withId("CANCEL_STALE_GAMES_JOB")
                .withInterval(Duration.ofHours(1))
                .withJobLambda { cancelStaleGames() }

            val cancelDailyGames = aRecurringJob()
                .withId("CANCEL_DAILY_GAMES_JOB")
                .withCron("0 0 * * *")
                .withZoneId(zurich)
                .withJobLambda { cancelDailyGames() }

            scheduler.createRecurrently(playerPing)
            scheduler.createRecurrently(cancelStaleGames)
            scheduler.createRecurrently(cancelDailyGames)
        }
    }

    /**
     * Needs to be public for JobRunr to pick it up
     */
    fun playerPing() {
        logger().info("Job [Player Ping]: Start checking player pings")

        val sevenSecondsAgo = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(7)  // Player ping is every 5

        val dcRecords: Result<Record> = db
            .select()
            .from(SEAT)
            .join(GAME).on(SEAT.GAME_ID.eq(GAME.ID))
            .join(PLAYER).on(SEAT.PLAYER_ID.eq(PLAYER.ID))
            .where(SEAT.PLAYER_PING.lt(sevenSecondsAgo))
            .and(GAME.STATUS.eq(GameStatus.RUNNING.name))
            .and(PLAYER.BOT.eq(false))
            .and(SEAT.STATUS.eq(SeatStatus.CONNECTED.name))
            .fetch()

        dcRecords.forEach { dc ->
            recover(
                { gameService.disconnectSeat(dc.get(SEAT.UUID)!!.toUUID()) },
                { throw Exception("Could not disconnect Seat ${dc.get(SEAT.UUID)}") }
            )
        }

        logger().info("Job [Player Ping]: Done! Dispatched PlayerDisconnected events to ${dcRecords.size} seats.")
    }

    /**
     * Needs to be public for JobRunr to pick it up
     */
    fun cancelStaleGames() {
        logger().info("Job [Cancel Stale Games]: Start canceling abandoned games")

        val fortyEightHoursAgo = LocalDateTime.now(ZoneOffset.UTC).minusHours(48)
        val canceled: Int = db
            .update(GAME)
            .set(GAME.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(GAME.STATUS, GameStatus.CANCELED.name)
            .whereNotExists(
                db
                    .selectOne()
                    .from(SEAT)
                    .where(SEAT.GAME_ID.eq(GAME.ID))
                    .and(SEAT.PLAYER_PING.ge(fortyEightHoursAgo))
            )
            .and(GAME.STATUS.eq(GameStatus.RUNNING.name))
            .execute()

        logger().info("Job [Cancel Stale Games]: Done! Canceled $canceled abandoned games.")
    }

    /**
     * Needs to be public for JobRunr to pick it up
     */
    fun cancelDailyGames() {
        logger().info("Job [Cancel Daily Games]: Start canceling unfinished daily challenges")

        val canceled: Int = db
            .update(GAME)
            .set(GAME.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(GAME.STATUS, GameStatus.CANCELED.name)
            .where(GAME.KIND.eq(GameKind.DAILY.name))
            .and(GAME.STATUS.eq(GameStatus.RUNNING.name))
            .execute()

        logger().info("Job [Cancel Daily Games]: Done! Canceled $canceled unfinished daily challenges.")
    }
}