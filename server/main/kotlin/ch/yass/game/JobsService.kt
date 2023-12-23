package ch.yass.game

import org.jobrunr.scheduling.JobScheduler
import java.time.Duration


class JobsService(private val scheduler: JobScheduler) {
    init {
        // TODO: Select all seats where player_ping < 7s ago and join game where status == running
        //       if any found, send PlayerDisconnected events to all other seats
        scheduler.scheduleRecurrently(Duration.parse("PT10S")) { println("check ping") }

        -> Wieso laufen nicht beide?
        // TODO: Select all games where last player ping of all seats < 15min ago and update
        //       the status. When a player joins make sure to set game state to running
        scheduler.scheduleRecurrently(Duration.parse("PT30S")) { println("check game status") }
    }
}