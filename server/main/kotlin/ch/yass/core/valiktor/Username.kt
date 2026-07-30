package ch.yass.core.valiktor

import org.valiktor.Constraint
import org.valiktor.Validator

object Username : Constraint

/**
 * Latin letters incl. the usual german and french accents, digits, `_` and `-`. Everything else
 * (emojis, symbols, whitespace) is out, an anon name has to stay valid for the ory identity schema
 * (`.docker/kratos/email-password/identity.schema.json`) when the anon upgrades to a real account.
 */
private val usernamePattern = Regex("^[a-zA-Z0-9À-ÖØ-öø-ÿŒœ_-]+$")

fun <E> Validator<E>.Property<String?>.isUsername(): Validator<E>.Property<String?> = this.validate(Username) {
    it == null || usernamePattern.matches(it)
}
