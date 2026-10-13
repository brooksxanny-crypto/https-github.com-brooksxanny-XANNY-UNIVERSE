#!/usr/bin/env python3
"""Compile pure Kotlin modules once and execute all standalone JVM test mains.
Runs entirely locally; it does NOT build or validate the Android APK or shaders.
"""
from pathlib import Path
import subprocess, tempfile, json, time, shutil, sys, argparse
parser=argparse.ArgumentParser(description='Run JVM-only Kotlin regressions; never an APK test')
parser.add_argument('--output', type=Path, help='Optional JSON report path')
opts=parser.parse_args()
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/universe/xanny/app'
excluded={'MainActivity.kt','UniverseRenderer.kt','DepthSceneCapture.kt','ReflectionCapture.kt'}
source=[str(p) for p in sorted(src.rglob('*.kt')) if p.name not in excluded]
tests=sorted((root/'tests').glob('*Test.kt'))
fixtures={'AnimatedMeshPreviewTest':'v10_skinned_triangle.glb', 'GltfSkinImportTest':'v10_skinned_triangle.glb', 'GpuSkinningContractTest':'v10_skinned_triangle.glb', 'GltfAnimationDecodeTest':'v11_translation_animation.glb', 'GltfExpandedMeshTest':'v8_normals_materials.glb', 'GltfMeshDecoderTest':'v8_normals_materials.glb'}
result={'mode':'JVM-only','android_apk_built':False,'android_gpu_tested':False,'tests':[]}
with tempfile.TemporaryDirectory(prefix='xanny_jvm_') as work:
 jar=str(Path(work)/'suite.jar')
 cmd=['kotlinc',*source,*map(str,tests),'-d',jar]
 try:
  compile_run=subprocess.run(cmd,capture_output=True,text=True,timeout=180)
 except subprocess.TimeoutExpired as exc:
  result['compile']={'passed':False,'reason':'compile_timeout','timeout_seconds':180}
  report=json.dumps(result,indent=2);print(report)
  if opts.output: opts.output.write_text(report+'\n')
  sys.exit(3)
 result['compile']={'passed':compile_run.returncode==0,'stderr':compile_run.stderr[-2500:]}
 if compile_run.returncode!=0:
  report=json.dumps(result,indent=2);print(report)
  if opts.output: opts.output.write_text(report+'\n')
  sys.exit(1)
 for test in tests:
  stem=test.stem
  args=[]
  if stem in fixtures:args=[str(root/'tests'/fixtures[stem])]
  try:
   run=subprocess.run(['kotlin','-classpath',jar,stem+'Kt',*args],capture_output=True,text=True,timeout=12,cwd=root)
   entry={'name':stem,'passed':run.returncode==0,'output':(run.stdout+'\n'+run.stderr)[-1000:]}
  except subprocess.TimeoutExpired:entry={'name':stem,'passed':False,'output':'TIMEOUT'}
  result['tests'].append(entry)
result['passed']=sum(t['passed'] for t in result['tests'])
result['total']=len(result['tests'])
report=json.dumps(result,indent=2)
print(report)
if opts.output: opts.output.write_text(report+'\n')
sys.exit(0 if result['passed']==result['total'] else 2)
