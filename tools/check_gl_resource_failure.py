#!/usr/bin/env python3
"""Static safety contract; not a substitute for driver shader compilation."""
from pathlib import Path
import re
s=(Path(__file__).resolve().parents[1]/"app/src/main/java/universe/xanny/app/render/UniverseRenderer.kt").read_text()
assert 'if (status[0] != GLES20.GL_TRUE)' in s
assert 'GLES20.glDeleteShader(s)' in s
for program in ('program','gpuProgram','volumeProgram','sceneCopyProgram','planarProgram'):
    assert re.search(r'GLES20\.glDeleteProgram\('+program+r'\)',s),program
assert 'shader compilation failed' in s
print('PASS: shader failure and link failure cleanup source contract')
