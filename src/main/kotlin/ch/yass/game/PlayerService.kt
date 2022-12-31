package ch.yass.game

import arrow.core.*
import arrow.core.continuations.either
import ch.yass.core.error.DomainError.*
import ch.yass.core.error.DomainError
import ch.yass.db.tables.references.PLAYER
import ch.yass.game.api.internal.NewPlayer
import ch.yass.game.dto.Player
import ch.yass.game.api.internal.UpdatePlayer
import com.google.gson.internal.LinkedTreeMap
import org.jooq.DSLContext
import sh.ory.model.Identity
import sh.ory.model.Session
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID


class PlayerService(private val db: DSLContext) {
    fun fromSession(session: Session): Either<DomainError, Player> = either.eager {
        val name = getNameFromIdentity(session.identity).bind()
        var player = get(UUID.fromString(session.identity.id)).getOrElse {
            create(NewPlayer(session.identity.id, name)).bind()
        }.

        if (player.name != name) {
            player = update(player.uuid, UpdatePlayer(name)).bind()
        }

        player
    }

    fun update(uuid: UUID, updatePlayer: UpdatePlayer): Either<DbError, Player> {
        val updatedPlayer = db.update(PLAYER)
            .set(PLAYER.NAME, updatePlayer.name)
            .set(PLAYER.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(PLAYER.UUID.eq(uuid.toString()))
            .returningResult(PLAYER.asterisk())
            .fetchOneInto(Player::class.java)

        return updatedPlayer?.right() ?: DbError("user.update.empty").left()
    }

    fun get(uuid: UUID): Either<DbError, Option<Player>> {
        return try {
            db.select()
                .from(PLAYER)
                .where(PLAYER.UUID.eq(uuid.toString()))
                .fetchOneInto(Player::class.java)
                .toOption()
                .right()
        } catch (exception: Exception) {
            DbError("user.get.error").left()
        }
    }


    fun create(player: NewPlayer): Either<DbError, Player> {
        val newPlayer = db.insertInto(PLAYER, PLAYER.UUID, PLAYER.NAME, PLAYER.CREATED_AT, PLAYER.UPDATED_AT)
            .values(player.uuid, player.name, LocalDateTime.now(ZoneOffset.UTC), LocalDateTime.now(ZoneOffset.UTC))
            .returningResult(PLAYER.asterisk())
            .fetchOneInto(Player::class.java)

        return newPlayer?.right() ?: DbError("user.create.empty").left()
    }

    private fun getNameFromIdentity(identity: Identity): Either<UnexpectedError, String> {
        val email = with(identity.traits as LinkedTreeMap<*, *>) { get("name") as String? }

        return email?.right() ?: UnexpectedError("ory.identity.name.missing").left()
    }
}