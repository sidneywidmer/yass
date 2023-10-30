package ch.yass.identity.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.valiktor.functions.isNotNull
import org.valiktor.functions.matches
import org.valiktor.validate
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImpersonateData(val impersonate: String) {
    init {
        validate(this) {
            validate(ImpersonateData::impersonate)
                .isNotNull()
                .matches(Regex("([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImpersonateRequest(val data: ImpersonateData)