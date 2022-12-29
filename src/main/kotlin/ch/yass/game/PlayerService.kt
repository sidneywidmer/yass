package ch.yass.game

import arrow.core.*
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.db.tables.references.PLAYER
import ch.yass.game.dto.NewPlayer
import ch.yass.game.dto.Player
import ch.yass.game.dto.UpdatePlayer
import com.google.gson.internal.LinkedTreeMap
import org.jooq.DSLContext
import sh.ory.model.Identity
import sh.ory.model.Session
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID


class PlayerService(private val db: DSLContext) {
    fun playerFromSession(session: Session): Either<DomainError, Player> = either.eager {
        val name = getNameFormIdentity(session.identity).bind()
        var player = getPlayer(UUID.fromString(session.identity.id)).getOrElse {
            createPlayer(NewPlayer(session.identity.id, name)).bind()
        }

        if (player.name != name) {
            player = updatePlayer(player.uuid, UpdatePlayer(name)).bind()
        }

        player
    }

    fun updatePlayer(uuid: UUID, updatePlayer: UpdatePlayer): Either<DomainError.DbError, Player> {
        val updatedPlayer = db.update(PLAYER)
            .set(PLAYER.NAME, updatePlayer.name)
            .set(PLAYER.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(PLAYER.UUID.eq(uuid.toString()))
            .returningResult(PLAYER.asterisk())
            .fetchOneInto(Player::class.java)

        return updatedPlayer?.right() ?: DomainError.DbError("user.update.empty").left()
    }

    fun getPlayer(uuid: UUID): Either<DomainError.DbError, Player> {
        val player = db.select()
            .from(PLAYER)
            .where(PLAYER.UUID.eq(uuid.toString()))
            .fetchOneInto(Player::class.java)

        return player?.right() ?: DomainError.DbError("user.get.empty").left()
    }


    fun createPlayer(player: NewPlayer): Either<DomainError.DbError, Player> {
        val newPlayer = db.insertInto(PLAYER, PLAYER.UUID, PLAYER.NAME, PLAYER.CREATED_AT, PLAYER.UPDATED_AT)
            .values(player.uuid, player.name, LocalDateTime.now(ZoneOffset.UTC), LocalDateTime.now(ZoneOffset.UTC))
            .returningResult(PLAYER.asterisk())
            .fetchOneInto(Player::class.java)

        return newPlayer?.right() ?: DomainError.DbError("user.create.empty").left()
    }

    private fun getNameFormIdentity(identity: Identity): Either<DomainError.UnexpectedError, String> {
        val email = with(identity.traits as LinkedTreeMap<*, *>) { get("name") as String? }

        return email?.right() ?: DomainError.UnexpectedError("ory.identity.name.missing").left()
    }
}