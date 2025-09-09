package ch.yass.unit

import ch.yass.core.helper.createToken
import ch.yass.core.helper.hashToken
import ch.yass.core.helper.verifyToken
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenTest {

    @Test
    fun testCreateHashAndVerifyToken() {
        val token = createToken()
        val hash = hashToken(token)

        assertTrue(verifyToken(token, hash))
    }
}
