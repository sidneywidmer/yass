package ch.yass.core.valiktor

import org.valiktor.Constraint
import org.valiktor.Validator

object Rank : Constraint

fun <E> Validator<E>.Property<String?>.isRank(): Validator<E>.Property<String?> = this.validate(Suit) {
    it == null || ch.yass.game.dto.Rank.values().any { rank -> rank.name == it }
}