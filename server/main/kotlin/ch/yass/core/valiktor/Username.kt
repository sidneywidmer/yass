package ch.yass.core.valiktor

import org.valiktor.Constraint
import org.valiktor.Validator

object Username : Constraint

fun <E> Validator<E>.Property<String?>.isUsername(): Validator<E>.Property<String?> = this.validate(Username) {
    it == null || it.all { char -> char.isLetterOrDigit() }
}