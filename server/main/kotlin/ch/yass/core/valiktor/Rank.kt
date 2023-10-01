package ch.yass.core.valiktor

import org.valiktor.Constraint
import org.valiktor.Validator

object Rank : Constraint

fun <E> Validator<E>.Property<String?>.isRank(): Validator<E>.Property<String?> = this.validate(Rank) {
    it == null || ch.yass.game.dto.Rank.entries.any { rank -> rank.name == it }
}