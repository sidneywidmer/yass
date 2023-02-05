package ch.yass.game

import arrow.core.*
import arrow.core.continuations.either
import ch.yass.core.error.DomainError.*
import ch.yass.core.error.DomainError
import ch.yass.core.helper.toUUID
import ch.yass.db.tables.references.PLAYER
import ch.yass.game.api.internal.NewPlayer
import ch.yass.game.dto.db.Player
import ch.yass.game.api.internal.UpdatePlayer
import com.google.gson.internal.LinkedTreeMap
import org.jooq.DSLContext
import sh.ory.model.Identity
import sh.ory.model.Session
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID


class PlayerService(private val db: DSLContext) {
    /**
     * Get a User dto from the ory session. If the user does not exist or changed its name - e.g.
     * via ory self-service - update it in our DB.
     */
    fun fromSession(session: Session): Either<DomainError, Player> = either.eager {
        val uuid = session.identity.id.toUUID().bind()
        val name = getNameFromIdentity(session.identity).bind()
        var player = getOrCreate(uuid, NewPlayer(uuid, name)).bind()

        if (player.name != name) {
            player = update(player.uuid, UpdatePlayer(name)).bind()
        }

        player
    }

    fun update(uuid: UUID, updatePlayer: UpdatePlayer): Either<DbError, Player> = Either.catch {
        val updatedPlayer = db.update(PLAYER)
            .set(PLAYER.NAME, updatePlayer.name)
            .set(PLAYER.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(PLAYER.UUID.eq(uuid.toString()))
            .returningResult(PLAYER)
            .fetchOneInto(Player::class.java)

        return updatedPlayer?.right() ?: DbError().left()
    }.mapLeft { DbError( it) }

    fun getOrCreate(uuid: UUID, newPlayer: NewPlayer): Either<DbError, Player> = either.eager {
        get(uuid).bind().getOrElse { create(newPlayer).bind() }
    }

    fun get(uuid: UUID): Either<DbError, Option<Player>> = Either.catch {
        db.selectFrom(PLAYER)
            .where(PLAYER.UUID.eq(uuid.toString()))
            .fetchOneInto(Player::class.java)
            .toOption()
    }.mapLeft { DbError( it) }


    fun create(player: NewPlayer): Either<DbError, Player> = Either.catch {
        val createdPlayer = db.insertInto(PLAYER, PLAYER.UUID, PLAYER.NAME, PLAYER.CREATED_AT, PLAYER.UPDATED_AT)
            .values(
                player.uuid.toString(),
                player.name,
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC)
            )
            .returningResult(PLAYER)
            .fetchOneInto(Player::class.java)

        return createdPlayer?.right() ?: DbError().left()
    }.mapLeft { DbError( it) }

    private fun getNameFromIdentity(identity: Identity): Either<UnexpectedError, String> {
        val email = with(identity.traits as LinkedTreeMap<*, *>) { get("name") as String? }

        return email?.right() ?: UnexpectedError("ori identity $identity did not contain `name`").left()
    }
}

