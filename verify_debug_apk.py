#!/usr/bin/env python3
"""Report facts about an already-built APK; never builds or runtime-verifies it."""
import hashlib,json,sys,zipfile
from pathlib import Path
apk=Path(sys.argv[1]) if len(sys.argv)>1 else Path('app/build/outputs/apk/debug/app-debug.apk')
report=Path(sys.argv[2]) if len(sys.argv)>2 else Path('APK_BUILD_REPORT.json')
r={'apk_path':str(apk),'apk_exists':apk.is_file(),'archive_valid':False,
   'android_manifest_present':False,'classes_dex_present':False,
   'compiled_apk_artifact_verified':False,'device_install_verified':False,
   'gpu_output_verified':False}
if apk.is_file():
    r['apk_bytes']=apk.stat().st_size
    r['apk_sha256']=hashlib.sha256(apk.read_bytes()).hexdigest()
    try:
        with zipfile.ZipFile(apk) as z:
            r['archive_valid']=z.testzip() is None
            names=set(z.namelist())
            r['android_manifest_present']='AndroidManifest.xml' in names
            r['classes_dex_present']='classes.dex' in names
    except (zipfile.BadZipFile,OSError) as e:
        r['archive_error']=str(e)
r['compiled_apk_artifact_verified']=all((r['apk_exists'],r['archive_valid'],r['android_manifest_present'],r['classes_dex_present']))
report.write_text(json.dumps(r,indent=2)+'\n')
print(json.dumps(r,indent=2))
sys.exit(0 if r['compiled_apk_artifact_verified'] else 1)
