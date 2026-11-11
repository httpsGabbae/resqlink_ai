package com.resqlink.ai.domain.model

enum class CallPriority(val id: String, val title: String, val description: String) {
    LOCAL_HOTLINE("LOCAL_HOTLINE", "Nearest Local Station First", "Direct auto-call to nearest 5km DRRMO, police, or rescue station"),
    UNIVERSAL_911("UNIVERSAL_911", "Universal 911 First", "Directly dials national 911 emergency dispatch"),
    FAMILY_FIRST("FAMILY_FIRST", "Family / ICE Contact First", "Immediately auto-dials your primary trusted emergency contact")
}

enum class SimPreference(val id: String, val title: String, val description: String, val slotIndex: Int?) {
    SYSTEM_DEFAULT("DEFAULT", "System Default / Prompt", "Use phone's default voice SIM or prompt every time", null),
    SIM_1("SIM_1", "SIM 1", "Always route emergency calls and alerts through SIM Slot 1", 0),
    SIM_2("SIM_2", "SIM 2", "Always route emergency calls and alerts through SIM Slot 2", 1)
}

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
