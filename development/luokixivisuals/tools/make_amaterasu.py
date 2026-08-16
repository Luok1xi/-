#!/usr/bin/env python3
import math, random, struct, zlib
from pathlib import Path

W, H, FRAMES = 64, 64, 12
OUT = Path('src/main/resources/assets/luokixivisuals/textures/effect/amaterasu.png')
OUT.parent.mkdir(parents=True, exist_ok=True)
random.seed(1040)

def px(frame, x, y):
    cx = (W - 1) / 2
    nx = abs((x - cx) / cx)
    fy = y / (H - 1)
    wobble = math.sin((x * .32) + frame * .85 + y * .11) * .10
    flame = 1.0 - nx * (1.18 + fy * .35) + wobble
    tip = 1.0 - fy
    noise = (random.Random(frame * 100000 + y * 100 + x).random() - .5) * .22
    strength = flame + noise + tip * .22
    if strength < .08 or (fy < .18 and strength < .38):
        return (0,0,0,0)
    edge = max(0.0, min(1.0, (0.42 - strength) * 3.4 + .35))
    a = int(max(0, min(255, 130 + strength * 125)))
    r = int(18 + edge * 105)
    g = int(2 + edge * 25)
    b = int(26 + edge * 150)
    if strength > .72:
        r, g, b = 8, 0, 14
    return (r,g,b,a)

rows = []
for f in range(FRAMES):
    for y in range(H):
        row = bytearray([0])
        for x in range(W): row.extend(px(f,x,y))
        rows.append(bytes(row))
raw = b''.join(rows)

def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

png = b'\x89PNG\r\n\x1a\n'
png += chunk(b'IHDR', struct.pack('>IIBBBBB', W, H * FRAMES, 8, 6, 0, 0, 0))
png += chunk(b'IDAT', zlib.compress(raw, 9))
png += chunk(b'IEND', b'')
OUT.write_bytes(png)
print(f'generated {OUT} {W}x{H*FRAMES}')
