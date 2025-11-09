package ch.yass.admin

import arrow.core.raise.Raise
import ch.yass.admin.api.internal.CreateMessage
import ch.yass.admin.dto.db.MessageMeta
import ch.yass.core.error.GameNotFound
import ch.yass.core.helper.toDbJson
import ch.yass.db.tables.references.MESSAGE
import ch.yass.game.GameRepository
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.time.ZoneOffset

class MessageService(
    private val gameRepository: GameRepository,
    private val db: DSLContext
) {


    context(Raise<GameNotFound>)
    fun createMessage(message: CreateMessage) {
        val game = message.gameUuid?.let { gameRepository.getByUUID(message.gameUuid) }
        val meta = MessageMeta(
            message.path,
            message.player.uuid,
            message.serverVersion,
            game?.uuid
        )

        db.insertInto(MESSAGE, MESSAGE.MESSAGE_, MESSAGE.META, MESSAGE.CREATED_AT, MESSAGE.UPDATED_AT)
            .values(
                message.message,
                toDbJson(meta),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
            ).execute()
    }

}
