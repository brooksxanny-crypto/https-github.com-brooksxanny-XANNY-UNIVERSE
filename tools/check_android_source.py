#!/usr/bin/env python3
"""Limited, explicit static project checks; not an Android compile."""
from pathlib import Path
import sys,json,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
checks={}
try:
 manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
 checks['manifest_parses']=manifest.tag=='manifest'
except Exception:checks['manifest_parses']=False
app=(root/'app/build.gradle.kts').read_text()
checks['android_namespace_present']='namespace = "universe.xanny.app"' in app
checks['android_35_configured']='compileSdk = 35' in app
checks['required_assets_present']=all((root/'app/src/main/assets'/n).exists() for n in ['v7_triangle.glb','v10_skinned_triangle.glb'])
checks['android_source_present']=(root/'app/src/main/java/universe/xanny/app/MainActivity.kt').exists()
checks['reflection_depth_source_present']=(root/'app/src/main/java/universe/xanny/app/render/DepthSceneCapture.kt').exists()
print(json.dumps({'static_checks':checks,'all_passed':all(checks.values()),'android_compiled':False},indent=2))
sys.exit(0 if all(checks.values()) else 1)
