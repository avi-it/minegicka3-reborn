"""Generates the mod's own 16x16 item textures (no third-party art). Run: python tools/gen_textures.py"""
import os, struct, zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "minegicka3", "textures", "item")


def png(path, px):
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *c) for c in row) for row in px)
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) \
        + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(data)


def hexc(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


def staff(shaft, gem, prongs=None, orbit=None):
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    s = hexc(shaft)
    # diagonal shaft from bottom-left (1,14) to (10,5), 2px thick with a dark edge
    for i in range(10):
        x, y = 1 + i, 14 - i
        px[y][x] = shade(s, 1.15)
        if x + 1 < 16:
            px[y][x + 1] = shade(s, 0.75)
    px[15][0] = shade(s, 0.6)
    g = hexc(gem)
    pr = hexc(prongs) if prongs is not None else shade(s, 1.3)
    # prongs cradling the gem
    for (x, y) in [(10, 4), (11, 5), (9, 3), (12, 6)]:
        px[y][x] = pr
    # 3x3 gem with highlight
    for dy in range(3):
        for dx in range(3):
            px[2 + dy][11 + dx] = shade(g, 1.0 - 0.15 * (dx + dy) / 2)
    px[1][12] = shade(g, 0.8)
    px[3][14] = shade(g, 0.7)
    px[2][11] = (255, 255, 255, 230)
    if orbit is not None:
        o = hexc(orbit)
        for (x, y) in [(9, 0), (15, 1), (15, 5), (8, 6)]:
            px[y][x] = o
    return px


def essence(col):
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    c = hexc(col)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 8) ** 2) ** 0.5
            if d < 5.2:
                k = 1.25 - d / 7
                px[y][x] = shade(c, k)
            elif d < 6.2:
                px[y][x] = shade(c, 0.45)
    px[5][6] = (255, 255, 255, 220)
    px[6][5] = (255, 255, 255, 160)
    return px


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    png(os.path.join(OUT, "staff.png"), staff(0x7A5230, 0x3FA7FF))
    png(os.path.join(OUT, "staff_grand.png"), staff(0x5B3A1E, 0xFFD23F, prongs=0xE0B030))
    png(os.path.join(OUT, "staff_super.png"), staff(0x2E2440, 0xD040FF, prongs=0xB0B0C8, orbit=0xF0A0FF))
    png(os.path.join(OUT, "staff_hemmy.png"), staff(0x202020, 0xFF3030, prongs=0xFFD700, orbit=0xFF8080))
    png(os.path.join(OUT, "staff_blessing.png"), staff(0xE8E0C8, 0x40FF60, prongs=0xFFFFFF))
    png(os.path.join(OUT, "staff_destruction.png"), staff(0x3A0A0A, 0xFF5000, prongs=0x202020))
    png(os.path.join(OUT, "staff_telekinesis.png"), staff(0x404858, 0xA0FFF0, prongs=0x8090A0))
    png(os.path.join(OUT, "staff_manipulation.png"), staff(0x503070, 0xFFFFFF, prongs=0xC090FF))
    for name, col in [("arcane", 0xFF0000), ("cold", 0xE8F0FF), ("earth", 0x6B4A26), ("fire", 0xFF4B00),
                      ("ice", 0x90FFFF), ("life", 0x00E000), ("lightning", 0xFF54FD), ("shield", 0xFFF638),
                      ("steam", 0xABABAB), ("water", 0x2529FF)]:
        png(os.path.join(OUT, name + "_essence.png"), essence(col))
    print("ok")
