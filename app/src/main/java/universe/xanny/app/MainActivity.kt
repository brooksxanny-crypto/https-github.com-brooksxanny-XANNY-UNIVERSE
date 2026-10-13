package universe.xanny.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import universe.xanny.app.render.UniverseSurface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import universe.xanny.app.core.PrimaryDestination
import universe.xanny.app.core.NavigationPolicy
import universe.xanny.app.core.LocalSearch

/** Honest accessible Android shell, not a substitute for the mandated real 3D world. */
class MainActivity: ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { XannyFoundation() } }
}

private val Obsidian=Color(0xFF07080B)
private val Chrome=Color(0xFFDCE0E6)

@Composable
fun XannyFoundation() {
  var selected by remember { mutableStateOf(PrimaryDestination.HOME) }
  MaterialTheme(colorScheme=darkColorScheme(background=Obsidian, surface=Color(0xFF171A21), primary=Chrome, onPrimary=Obsidian, onBackground=Color.White)) {
    Scaffold(containerColor=Obsidian, bottomBar={
      NavigationBar(containerColor=Color(0xFF13151B)) {
        NavigationPolicy.primary.forEach { destination ->
          NavigationBarItem(
            selected=selected==destination, onClick={selected=destination},
            icon={ Text(text=when(destination) { PrimaryDestination.HOME->"⌂"; PrimaryDestination.SEARCH->"⌕"; PrimaryDestination.MUSIC_UNIVERSE->"♫"; PrimaryDestination.YOUR_LIBRARY->"▤" }) },
            label={Text(destination.label)}, alwaysShowLabel=true
          )
        }
      }
    }) { padding ->
      Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("XANNY UNIVERSE", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold)
        Text(selected.label, style=MaterialTheme.typography.titleLarge, color=Chrome)
        HorizontalDivider(color=Chrome.copy(alpha=.35f))
        Text("ANDROID FOUNDATION · NOT A COMPLETED UNIVERSE", style=MaterialTheme.typography.labelMedium)
        Text("The approved 3D worlds, H3 volumetric atmosphere, chrome reflections, rigged Xanny and Tokyo, media, Reels and Together communications require their real engines and assets. Nothing here claims those features are live.")
        Text("Status: foundation navigation implemented in source; Android runtime not verified.", color=Chrome)
        if(selected==PrimaryDestination.SEARCH) {
          var searchQuery by rememberSaveable { mutableStateOf("") }
          OutlinedTextField(
            value=searchQuery, onValueChange={searchQuery=it},
            label={Text("Search app destinations")},
            singleLine=true, modifier=Modifier.fillMaxWidth()
          )
          Text("Local navigation index only. No live media or private data is searched.", style=MaterialTheme.typography.bodySmall)
          val matches=LocalSearch.find(searchQuery)
          if(matches.isEmpty()) Text("No matching app destinations.")
          matches.forEach { result ->
            Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
              Text(result.label, fontWeight=FontWeight.SemiBold)
              Text(result.detail, style=MaterialTheme.typography.bodySmall)
              if(result.available && result.label=="Home") {
                TextButton(onClick={selected=PrimaryDestination.HOME}) { Text("Open Home") }
              } else if (!result.available) {
                Text("Not yet implemented", style=MaterialTheme.typography.labelSmall, color=Chrome)
              }
            }
            HorizontalDivider(color=Chrome.copy(alpha=.16f))
          }
        }
        if(selected==PrimaryDestination.HOME) {
          Text("OpenGL ES celestial scene preview · drag to orbit camera · device unverified", style=MaterialTheme.typography.labelLarge)
          key(selected) {
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            val surface = remember { mutableStateOf<UniverseSurface?>(null) }
            DisposableEffect(lifecycleOwner) {
              val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                  androidx.lifecycle.Lifecycle.Event.ON_RESUME -> surface.value?.onResume()
                  androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> surface.value?.onPause()
                  else -> Unit
                }
              }
              lifecycleOwner.lifecycle.addObserver(observer)
              onDispose { lifecycleOwner.lifecycle.removeObserver(observer); surface.value?.onPause(); surface.value = null }
            }
            AndroidView(factory={ context -> UniverseSurface(context).also { surface.value=it; if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) it.onResume() } }, modifier=Modifier.fillMaxWidth().height(240.dp), onRelease = { view -> view.onPause(); if (surface.value === view) surface.value = null })
          }

          Text("Together", style=MaterialTheme.typography.titleMedium)
          Text("The protected physical 3D celestial entrance is not yet implemented. No substitute direct-entry button is offered.")
        }
      }
    }
  }
}
