package ch.yass.identity

import sh.ory.api.FrontendApi

/**
 * Wrapper around the ory api classes.
 */
data class OryClient(val frontend: FrontendApi)