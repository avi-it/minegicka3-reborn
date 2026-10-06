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


def gem(col):
    """Octahedron-looking diamond gem (Thingies)."""
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    c = hexc(col)
    for y in range(2, 14):
        w = 6 - abs(y - 7.5) * 0.9
        for x in range(16):
            dx = x - 7.5
            if abs(dx) <= w:
                k = 1.25 if dx < 0 and y < 8 else 1.0 if dx < 0 else 0.8 if y < 8 else 0.6
                px[y][x] = shade(c, k)
    px[4][6] = (255, 255, 255, 230)
    return px


def rod(col, tip):
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    c, t = hexc(col), hexc(tip)
    for i in range(12):
        x, y = 2 + i, 13 - i
        px[y][x] = shade(c, 1.1)
        px[y + 1][x] = shade(c, 0.7)
    for (x, y) in [(13, 2), (14, 1), (13, 1), (14, 2)]:
        px[y][x] = t
    return px


def apple(col, leaf=0x3C8C28):
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    c = hexc(col)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + ((y - 9) * 1.1) ** 2) ** 0.5
            if d < 5.5:
                px[y][x] = shade(c, 1.2 - d / 8)
            elif d < 6.3:
                px[y][x] = shade(c, 0.5)
    for y in (2, 3):
        px[y][8] = (90, 60, 30, 255)
    px[2][9] = hexc(leaf)
    px[2][10] = hexc(leaf)
    px[6][5] = (255, 255, 255, 200)
    return px


def cookie(chip):
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    base = hexc(0xC88A48)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 6:
                px[y][x] = shade(base, 1.1 - noise(x, y, 11) * 0.25)
            elif d < 6.8:
                px[y][x] = shade(base, 0.6)
    for (x, y) in [(5, 5), (9, 4), (10, 9), (6, 10), (8, 7)]:
        px[y][x] = hexc(chip)
        px[y][x + 1] = shade(hexc(chip), 0.7)
    return px


def hat(col, band):
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    c, b = hexc(col), hexc(band)
    for y in range(1, 12):
        w = (y - 1) * 0.45 + 0.5
        for x in range(16):
            if abs(x - 7.5 + (11 - y) * 0.15) <= w:
                px[y][x] = shade(c, 1.15 if x < 8 else 0.85)
    for x in range(1, 15):
        px[12][x] = shade(c, 0.9)
        px[13][x] = shade(c, 0.6)
    for x in range(3, 13):
        px[11][x] = b
    px[5][7] = (255, 255, 160, 255)
    return px


def noise(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0


def shield_block():
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            hexline = (x + 2 * y) % 8 == 0 or (x - 2 * y) % 8 == 0
            if edge:
                px[y][x] = (255, 250, 150, 170)
            elif hexline:
                px[y][x] = (255, 246, 120, 120)
            else:
                k = noise(x, y, 7)
                px[y][x] = (255, 255, 160 + int(60 * k), 45 + int(25 * k))
    for (x, y) in [(4, 5), (11, 3), (7, 11), (12, 12)]:
        px[y][x] = (255, 255, 230, 220)
    return px


def wall_block(base, light, dark, seed):
    px = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    b, l, d = hexc(base), hexc(light), hexc(dark)
    for y in range(16):
        for x in range(16):
            k = noise(x, y, seed)
            # vertical spike ridges every 4 px
            ridge = abs((x % 4) - 1.5) / 1.5
            c = shade(b, 0.8 + 0.35 * k - 0.25 * ridge)
            if x % 4 == 3:
                c = d
            elif x % 4 == 1 and k > 0.6:
                c = l
            px[y][x] = c
    return px


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    blocks = os.path.join(OUT, "..", "block")
    os.makedirs(blocks, exist_ok=True)
    png(os.path.join(blocks, "shield.png"), shield_block())
    png(os.path.join(blocks, "wall_earth.png"), wall_block(0x6B4A26, 0x9A7448, 0x3A2812, 3))
    png(os.path.join(blocks, "wall_ice.png"), wall_block(0x8FD8F0, 0xE0FFFF, 0x4A9AC0, 5))
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
    png(os.path.join(OUT, "resistance_essence.png"), essence(0x40C080))
    png(os.path.join(OUT, "thingy.png"), gem(0xFFE040))
    png(os.path.join(OUT, "thingy_good.png"), gem(0x40E8FF))
    png(os.path.join(OUT, "thingy_great.png"), gem(0xC050FF))
    png(os.path.join(OUT, "stick.png"), rod(0x8A6A3A, 0xFFE040))
    png(os.path.join(OUT, "stick_good.png"), rod(0x6A5A8A, 0x40E8FF))
    png(os.path.join(OUT, "stick_great.png"), rod(0x3A2A5A, 0xC050FF))
    png(os.path.join(OUT, "magic_apple.png"), apple(0xD02040))
    png(os.path.join(OUT, "magic_golden_apple.png"), apple(0xF0C030))
    png(os.path.join(OUT, "magic_great_apple.png"), apple(0xC060FF, leaf=0xFFE040))
    png(os.path.join(OUT, "magic_cookie.png"), cookie(0x3060FF))
    png(os.path.join(OUT, "magic_good_cookie.png"), cookie(0x30E0FF))
    png(os.path.join(OUT, "magic_great_cookie.png"), cookie(0xD040FF))
    png(os.path.join(OUT, "hat.png"), hat(0x3040A0, 0xFFD040))
    png(os.path.join(OUT, "hat_risk.png"), hat(0xA02020, 0x202020))
    png(os.path.join(OUT, "hat_resistance.png"), hat(0x208050, 0xE0E0E0))
    png(os.path.join(OUT, "hat_immunity.png"), hat(0xF0F0F0, 0xFFD700))
    print("ok")
