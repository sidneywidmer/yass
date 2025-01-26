package ch.yass.identity.helper

import ch.yass.core.contract.CtxAttributes
import ch.yass.core.helper.config
import ch.yass.game.dto.db.InternalPlayer
import io.javalin.http.Context

fun player(ctx: Context): InternalPlayer = ctx.attribute<InternalPlayer>(CtxAttributes.PLAYER.name)!!

fun isAdmin(player: InternalPlayer): Boolean = config().getStringList("admins").contains(player.uuid.toString())
