package com.resqlink.ai.domain.model

data class TrustedContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String = "Family",
    val isAuthorizedForSafetyCheck: Boolean = true
)
