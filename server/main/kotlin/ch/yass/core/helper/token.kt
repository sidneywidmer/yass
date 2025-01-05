package ch.yass.core.helper

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

fun createToken(): String {
    val token = ByteArray(64)
    SecureRandom().nextBytes(token)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token)
}

fun hashToken(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(token.toByteArray())
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
}

fun verifyToken(token: String, storedHash: String): Boolean {
    try {
        val computedHash = hashToken(token)
        return MessageDigest.isEqual(
            Base64.getUrlDecoder().decode(computedHash),
            Base64.getUrlDecoder().decode(storedHash)
        )
    } catch (_: Exception) {
        return false
    }
}
