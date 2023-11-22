package com.example.api_auth_sample.model

enum class FlowStatus(val flowStatus: String) {
    /**
    * Flow status is fail.
    */
    FAIL_INCOMPLETE("FAIL_INCOMPLETE"),

    /**
     * Flow status is success.
     */
    SUCCESS("SUCCESS"),
}