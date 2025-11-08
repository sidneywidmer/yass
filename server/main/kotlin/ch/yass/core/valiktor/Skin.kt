package ch.yass.core.valiktor

import org.valiktor.Constraint
import org.valiktor.Validator

object Skin : Constraint

fun <E> Validator<E>.Property<String?>.isSkin(): Validator<E>.Property<String?> = this.validate(Skin) {
    it == null || ch.yass.game.dto.Skin.entries.any { rank -> rank.name == it }
}