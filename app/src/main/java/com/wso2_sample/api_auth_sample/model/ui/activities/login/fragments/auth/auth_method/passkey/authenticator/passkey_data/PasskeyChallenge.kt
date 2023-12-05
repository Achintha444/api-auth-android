package com.wso2_sample.api_auth_sample.model.ui.activities.login.fragments.auth.auth_method.passkey.authenticator.passkey_data

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.wso2_sample.api_auth_sample.util.Util

data class PasskeyChallenge(
    val challenge: String,
    val allowCredentials: List<String>,
    val timeout: Long,
    val userVerification: String,
    val rpId: String
) {
    override fun toString(): String {
        return Util.getJsonString(this)
    }

}
