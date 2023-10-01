package ch.yass.identity.helper

import ch.yass.core.contract.CtxAttributes
import ch.yass.game.dto.db.Player
import io.javalin.http.Context

fun player(ctx: Context): Player {
    return ctx.attribute<Player>(CtxAttributes.PLAYER.name)!!
}