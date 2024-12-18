package ch.yass.identity.helper

import ch.yass.core.contract.CtxAttributes
import ch.yass.core.helper.config
import ch.yass.game.dto.db.Player
import io.javalin.http.Context

fun player(ctx: Context): Player = ctx.attribute<Player>(CtxAttributes.PLAYER.name)!!

fun isAdmin(player: Player): Boolean = config().getStringList("admins").contains(player.uuid.toString())