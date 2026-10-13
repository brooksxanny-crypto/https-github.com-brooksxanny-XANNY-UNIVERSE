import universe.xanny.app.core.LocalSearch
fun main() {
  check(LocalSearch.find("  ").size==8)
  check(LocalSearch.find("MuSiC").any { it.label=="Music Universe" && !it.available })
  check(LocalSearch.find("HOME").single().available)
  check(LocalSearch.find("nonexistent-xyz").isEmpty())
  check(LocalSearch.find("AI").any { it.label=="Xanny AI" })
  check(LocalSearch.find("Together").single().available.not())
  println("PASS LocalSearchTest (6 assertions)")
}
