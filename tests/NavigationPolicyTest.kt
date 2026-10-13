import universe.xanny.app.core.*

fun main() {
    check(NavigationPolicy.primary.size == 4)
    check(NavigationPolicy.primary.map { it.label } == listOf("Home", "Search", "Music Universe", "Your Library"))
    check(NavigationPolicy.primary.distinct().size == 4)
    check(!NavigationPolicy.canEnterTogether(physicalEntryActivated = false, authorized = true))
    check(!NavigationPolicy.canEnterTogether(physicalEntryActivated = true, authorized = false))
    check(NavigationPolicy.canEnterTogether(physicalEntryActivated = true, authorized = true))
    check(SecondaryExperience.TOGETHER.name !in PrimaryDestination.entries.map { it.name })
    println("PASS: protected four routes, labels, uniqueness, and Together physical-entry authorization policy")
}
