package ch.yass.admin.api

/**
 * Helper to generate random hands. Can be used to generate data for
 * unittests.
 */
data class GenerateHandResponse(
    val north: String,
    val east: String,
    val south: String,
    val west: String
)
