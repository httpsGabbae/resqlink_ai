package com.resqlink.ai.domain.model

/**
 * # CallPriority
 *
 * Dictates which phone number is automatically dialed first when an emergency is confirmed.
 *
 * The user can configure this preference in the app's Settings screen:
 * - [LOCAL_HOTLINE]: Calls the closest municipal DRRMO, police precinct, or hospital within 5km.
 * - [UNIVERSAL_911]: Calls the nationwide emergency 911 dispatch.
 * - [FAMILY_FIRST]: Calls the user's primary trusted family member or emergency contact.
 */
enum class CallPriority(val id: String, val title: String, val description: String) {
    LOCAL_HOTLINE("LOCAL_HOTLINE", "Nearest Local Station First", "Direct auto-call to nearest 5km DRRMO, police, or rescue station"),
    UNIVERSAL_911("UNIVERSAL_911", "Universal 911 First", "Directly dials national 911 emergency dispatch"),
    FAMILY_FIRST("FAMILY_FIRST", "Primary Contact First", "Immediately auto-dials your designated primary emergency contact")
}

/**
 * # SimPreference
 *
 * For dual-SIM phones (very common in the Philippines with Smart, Globe, and DITO SIMs).
 *
 * ResQLink allows the user to pin which physical SIM card is used for emergency calls and SMS
 * so that outgoing alerts are not stalled by an interactive "Choose SIM" system dialog during a crisis.
 */
enum class SimPreference(val id: String, val title: String, val description: String, val slotIndex: Int?) {
    SYSTEM_DEFAULT("DEFAULT", "System Default / Prompt", "Use phone's default voice SIM or prompt every time", null),
    SIM_1("SIM_1", "SIM 1", "Always route emergency calls and alerts through SIM Slot 1", 0),
    SIM_2("SIM_2", "SIM 2", "Always route emergency calls and alerts through SIM Slot 2", 1)
}

/**
 * # UserProfile
 *
 * Holds the local identity and medical profile of the phone's owner.
 *
 * All fields default to empty strings to avoid dangerous mock medical data.
 * The user populates these in the Rescue Card / Medical Profile screen.
 *
 * @property id Unique user ID string.
 * @property displayName Full name of the user.
 * @property email User's email address if signed in.
 * @property photoUrl URL or URI to the user's avatar image, if available.
 * @property bloodType ABO and Rh blood classification (e.g. "O+", "A-", "B+").
 * @property allergies Known drug or food allergies (e.g. "Penicillin", "Peanuts").
 * @property medicalNotes Critical medical conditions or instructions (e.g. "Diabetic", "Asthma inhaler in pocket").
 * @property primaryContactName Name of the primary In-Case-of-Emergency (ICE) contact.
 * @property primaryContactPhone Mobile number of the primary ICE contact.
 * @property callPriority The user's preferred emergency dialing priority.
 * @property simPreference Preferred dual-SIM slot for emergency calls and SMS.
 * @property isLoggedIn True if the user completed Google Sign-In or completed setup.
 * @property isGuest True if running in local offline guest mode without an account.
 */
data class UserProfile(
    val id: String = "user_default",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val bloodType: String = "",
    val allergies: String = "",
    val medicalNotes: String = "",
    val primaryContactName: String = "",
    val primaryContactPhone: String = "",
    val callPriority: CallPriority = CallPriority.LOCAL_HOTLINE,
    val simPreference: SimPreference = SimPreference.SYSTEM_DEFAULT,
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = false
)
