package ch.yass.core.valiktor

import ch.yass.game.dto.Rank
import org.valiktor.Constraint
import org.valiktor.Validator

object Rank : Constraint

fun <E> Validator<E>.Property<String?>.isRank(): Validator<E>.Property<String?> = this.validate(Suit) {
    it == null || Rank.entries.any { rank -> rank.name == it }
}