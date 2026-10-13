package universe.xanny.app.core

/** Actual local destinations only: no fabricated media, users or search results. */
data class SearchDestination(val label: String, val detail: String, val available: Boolean)
object LocalSearch {
    val destinations = listOf(
        SearchDestination("Home", "Celestial preview and foundation status", true),
        SearchDestination("Search", "Local destination search", true),
        SearchDestination("Music Universe", "Music provider integration pending", false),
        SearchDestination("Your Library", "Authorized user library integration pending", false),
        SearchDestination("Together", "Physical celestial entrance and communications pending", false),
        SearchDestination("Xanny AI", "Real embodied AI integration pending", false),
        SearchDestination("Cinema", "Licensed provider integration pending", false),
        SearchDestination("Reels", "Authorized Reels integration pending", false)
    )
    fun find(query: String): List<SearchDestination> {
        val q=query.trim()
        if(q.isEmpty()) return destinations
        return destinations.filter { it.label.contains(q, ignoreCase=true) || it.detail.contains(q,ignoreCase=true) }
    }
}
