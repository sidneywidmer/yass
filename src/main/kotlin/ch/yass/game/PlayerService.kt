package ch.yass.game

import arrow.core.Either
import ch.yass.core.DomainError
import ch.yass.game.dto.Player
import sh.ory.model.Session
import java.time.LocalDateTime
import java.util.*

class PlayerService {
    fun playerFromSession(session: Session): Either<DomainError, Player> {
        // See if we have a matching player in the db with oryIdentityId = session.identity.id
        // -> If yes, see if da identity.updatedAt is > then player.updatedAt
        // --> If yes, updated player.name and player.updatedAt
        // -> If no, create a new player in the db
        // -> Return a Player DTO
        return Either.Right(
            Player(
                1,
                UUID.fromString(session.identity.id),
                "Messi",
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        )
        //return Either.Left(DomainError.TbdError("foo"))
    }
}