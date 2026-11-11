package com.resqlink.ai.domain.model

/**
 * # TrustedContact
 *
 * Represents an In-Case-of-Emergency (ICE) person designated by the user.
 *
 * During an emergency:
 * 1. An automated SMS with live GPS coordinates and Google Maps links is dispatched to each contact.
 * 2. If the user configured "Family First" calling priority, the phone automatically dials the
 *    primary trusted contact's phone number first.
 *
 * @property id The unique database row ID assigned by the local SQLite database.
 * @property name The full name of the emergency contact (e.g. "Primary Contact", "Mom", "Doctor Cruz").
 * @property phoneNumber The mobile number formatted for cellular calling/SMS (e.g. "09171234567").
 * @property relationship The user's relationship to this person (e.g. "Spouse", "Parent", "Sibling", "Friend").
 * @property isAuthorizedForSafetyCheck When true, this contact is permitted to receive automatic
 *   check-in alerts if the user misses a scheduled Safety Journey arrival deadline.
 */
data class TrustedContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String = "Family",
    val isAuthorizedForSafetyCheck: Boolean = true
)
