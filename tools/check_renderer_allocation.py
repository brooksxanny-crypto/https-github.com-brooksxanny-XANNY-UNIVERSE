#!/usr/bin/env python3
"""Static guard against per-frame direct buffer allocation in the GPU skinning draw path."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/java/universe/xanny/app/render/UniverseRenderer.kt'
s=p.read_text()
start=s.index('    private fun drawScene(')
end=s.index('    private fun drawPlanarReflection()',start)
draw=s[start:end]
assert 'gpuBindPositions' in s[:start]
assert 'gpuBindPositions.position(0)' in draw or 'gpuBindPositions!!.position(0)' in draw
assert 'val bindPositions=ByteBuffer.allocateDirect' not in draw
assert 'gpuFixture.first.positions.size*4' not in draw
print('PASS: GPU bind-pose vertex buffer allocated outside drawScene')
