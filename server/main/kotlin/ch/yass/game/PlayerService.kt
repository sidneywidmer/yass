package ch.yass.game

import arrow.core.raise.Raise
import ch.yass.core.error.OryIdentityWithoutName
import ch.yass.core.error.StringNoValidUUID
import ch.yass.core.helper.toUUID
import ch.yass.db.tables.references.PLAYER
import ch.yass.game.api.internal.NewAnonPlayer
import ch.yass.game.api.internal.NewBotPlayer
import ch.yass.game.api.internal.NewOryPlayer
import ch.yass.game.api.internal.UpdatePlayer
import ch.yass.game.dto.db.Player
import com.google.gson.internal.LinkedTreeMap
import org.jooq.DSLContext
import sh.ory.model.Identity
import sh.ory.model.Session
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


class PlayerService(private val db: DSLContext) {
    /**
     * Get a User dto from the ory session. If the user does not exist or changed its name - e.g.
     * via ory self-service - update it in our DB.
     */
    context(Raise<StringNoValidUUID>, Raise<OryIdentityWithoutName>)
    fun fromSession(session: Session): Player {
        val oryUuid = session.identity.id.toUUID()
        val name = getNameFromIdentity(session.identity)
        var player = getOrCreateByOry(oryUuid, NewOryPlayer(oryUuid, name))

        if (player.name != name) {
            player = update(player.uuid, UpdatePlayer(name))
        }

        return player
    }

    fun getByAnonToken(token: String): Player? {
        return db.selectFrom(PLAYER)
            .where(PLAYER.ANON_TOKEN.eq(token))
            .fetchOneInto(Player::class.java)
    }

    fun getByOryUuid(oryUuid: UUID): Player? {
        return db.selectFrom(PLAYER)
            .where(PLAYER.ORY_UUID.eq(oryUuid.toString()))
            .fetchOneInto(Player::class.java)
    }

    fun create(player: NewOryPlayer): Player {
        return createInternal(
            uuid = UUID.randomUUID(),
            oryUuid = player.oryUuid,
            name = player.name,
            bot = false,
            anonToken = null
        )
    }

    fun create(player: NewBotPlayer): Player {
        return createInternal(
            uuid = UUID.randomUUID(),
            oryUuid = null,
            name = player.name,
            bot = true,
            anonToken = null
        )
    }

    fun create(player: NewAnonPlayer): Player {
        return createInternal(
            uuid = UUID.randomUUID(),
            oryUuid = null,
            name = player.name,
            bot = false,
            anonToken = player.anonToken
        )
    }

    private fun createInternal(uuid: UUID, oryUuid: UUID?, name: String, bot: Boolean, anonToken: String?): Player {
        return db.insertInto(
            PLAYER,
            PLAYER.UUID,
            PLAYER.ORY_UUID,
            PLAYER.NAME,
            PLAYER.BOT,
            PLAYER.ANON_TOKEN,
            PLAYER.CREATED_AT,
            PLAYER.UPDATED_AT
        )
            .values(
                uuid.toString(),
                oryUuid.toString(),
                name,
                bot,
                anonToken,
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
            )
            .returningResult(PLAYER)
            .fetchOneInto(Player::class.java)!!
    }

    private fun update(uuid: UUID, updatePlayer: UpdatePlayer): Player {
        return db.update(PLAYER)
            .set(PLAYER.NAME, updatePlayer.name)
            .set(PLAYER.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(PLAYER.UUID.eq(uuid.toString()))
            .returningResult(PLAYER)
            .fetchOneInto(Player::class.java)!!
    }

    private fun getOrCreateByOry(oryUuid: UUID, newOryPlayer: NewOryPlayer): Player {
        return getByOryUuid(oryUuid) ?: create(newOryPlayer)
    }

    context(Raise<OryIdentityWithoutName>)
    private fun getNameFromIdentity(identity: Identity): String {
        val email = with(identity.traits as LinkedTreeMap<*, *>) { get("name") as String? }

        return email ?: raise(OryIdentityWithoutName(identity))
    }
}

