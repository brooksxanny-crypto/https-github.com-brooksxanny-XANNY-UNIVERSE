package universe.xanny.app.core

/** The complete and immutable owner-approved primary-destination set (G05). */
enum class PrimaryDestination(val label: String) {
    HOME("Home"), SEARCH("Search"), MUSIC_UNIVERSE("Music Universe"), YOUR_LIBRARY("Your Library")
}

/** Together is a secondary physical-world entrance, NOT a fifth primary tab. */
enum class SecondaryExperience { TOGETHER, CINEMA, REELS, WORLD }

/** No false representation of missing immersive functionality as a working feature. */
enum class DeliveryState { PLANNED, IMPLEMENTED, TESTED, VERIFIED, BLOCKED }

object NavigationPolicy {
    val primary: List<PrimaryDestination> = PrimaryDestination.entries.toList()
    fun canEnterTogether(physicalEntryActivated: Boolean, authorized: Boolean): Boolean =
        physicalEntryActivated && authorized
}
