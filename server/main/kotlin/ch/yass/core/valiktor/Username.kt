package ch.yass.core.valiktor

import org.valiktor.Constraint
import org.valiktor.Validator

object Username : Constraint

/**
 * Ascii only, a guest name has to stay valid for the ory identity schema (`^[a-zA-Z0-9_-]+$`)
 * when the guest upgrades to a real account.
 */
fun <E> Validator<E>.Property<String?>.isUsername(): Validator<E>.Property<String?> = this.validate(Username) {
    it == null || it.all { char -> char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' }
}