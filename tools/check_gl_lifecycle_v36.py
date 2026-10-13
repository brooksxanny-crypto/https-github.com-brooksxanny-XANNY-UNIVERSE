#!/usr/bin/env python3
"""Static lifecycle-contract guard, NOT an Android device test."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=(root/'app/src/main/java/universe/xanny/app/MainActivity.kt').read_text()
renderer=(root/'app/src/main/java/universe/xanny/app/render/UniverseRenderer.kt').read_text()
checks={
    'egl_context_preservation_requested':'preserveEGLContextOnPause = true' in renderer,
    'surface_reference_cleared_on_dispose':'surface.value = null' in main,
    'android_view_on_release':'onRelease = { view -> view.onPause(); if (surface.value === view) surface.value = null }' in main,
    'lifecycle_observer_removed':'removeObserver(observer)' in main,
    'no_fifth_navigation_tab':'NavigationPolicy.primary.forEach' in main,
}
import json
print(json.dumps(checks,indent=2))
raise SystemExit(0 if all(checks.values()) else 1)
