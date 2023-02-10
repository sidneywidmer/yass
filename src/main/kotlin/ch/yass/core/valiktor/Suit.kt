package ch.yass.core.valiktor

import org.valiktor.Constraint
import org.valiktor.Validator

object Suit : Constraint

fun <E> Validator<E>.Property<String?>.isSuit(): Validator<E>.Property<String?> = this.validate(Suit) {
    it == null || ch.yass.game.dto.Suit.values().any { suit -> suit.name == it }
}

fun <E> Validator<E>.Property<String?>.isTrump(): Validator<E>.Property<String?> = this.validate(Suit) {
    it == null || ch.yass.game.dto.Trump.values().any { trump -> trump.name == it }
}
