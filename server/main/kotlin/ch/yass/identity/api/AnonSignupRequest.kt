package ch.yass.identity.api

import ch.yass.core.valiktor.isUsername
import org.valiktor.functions.hasSize
import org.valiktor.functions.isNotNull
import org.valiktor.functions.startsWith
import org.valiktor.validate

data class AnonSignupRequest(val name: String) {
    init {
        validate(this) {
            validate(AnonSignupRequest::name)
                .isNotNull()
                .hasSize(3, 15)
                .isUsername()
        }
    }
}