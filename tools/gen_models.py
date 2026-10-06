"""3D item models for the staves, crystals, sticks and hats, built from flat-coloured cuboids (26.3 allows free
x/y/z element rotation). The designs are this project's own: chunky, readable wizard gear in the spirit of Magicka's
look. Units are model pixels (16 = one block), centred on the model's offset.
Run after gen_textures.py: python tools/gen_models.py"""
import json
import math
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "minegicka3")
COLDIR = os.path.join(ROOT, "textures", "item", "color")

# palette
WOOD, WOOD_DARK, WOOD_PALE = (122, 82, 48), (74, 48, 28), (214, 196, 160)
GOLD, GOLD_DARK, SILVER, IRON = (232, 186, 64), (168, 124, 30), (196, 204, 214), (70, 74, 82)
BLACK, WHITE = (22, 20, 24), (240, 240, 236)

ELEMENT_COLORS = {"arcane": (214, 24, 52), "cold": (220, 236, 255), "earth": (122, 86, 50), "fire": (255, 106, 20),
                  "ice": (130, 226, 245), "life": (60, 210, 70), "lightning": (190, 90, 245), "shield": (250, 214, 60),
                  "steam": (176, 182, 188), "water": (44, 86, 236)}


def png(path, rgb):
    raw = b"".join(b"\x00" + bytes(rgb + (255,)) * 4 for _ in range(4))
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 4, 4, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


def lighter(rgb, k):
    return tuple(min(255, int(c + (255 - c) * k)) for c in rgb)


def darker(rgb, k):
    return tuple(int(c * (1 - k)) for c in rgb)


class Model:
    def __init__(self, offset=(8, 8, 8), scale=1.0):
        self.elements, self.colors, self.offset, self.scale = [], {}, offset, scale

    def _tex(self, rgb):
        name = "%02x%02x%02x" % rgb
        self.colors[name] = rgb
        return "#c" + name

    def _p(self, v):
        return [round(self.offset[i] + v[i] * self.scale, 4) for i in range(3)]

    def box(self, center, size, rgb, rot=(0, 0, 0)):
        c = self._p(center)
        h = [s * self.scale / 2 for s in size]
        tex = self._tex(rgb)
        e = {"from": [round(c[i] - h[i], 4) for i in range(3)], "to": [round(c[i] + h[i], 4) for i in range(3)],
             "faces": {d: {"texture": tex, "uv": [0, 0, 4, 4]} for d in ("north", "south", "east", "west", "up", "down")}}
        if any(rot):
            e["rotation"] = {"origin": c, "x": round(rot[0], 3), "y": round(rot[1], 3), "z": round(rot[2], 3)}
        self.elements.append(e)

    def rod(self, a, b, w, rgb):
        """Square rod of width w from point a to point b."""
        d = [b[i] - a[i] for i in range(3)]
        length = math.sqrt(sum(x * x for x in d))
        if length < 1e-6:
            return
        dx, dy, dz = (x / length for x in d)
        # tilt +Y by ax about X, then turn about Y by ay
        ax = math.degrees(math.acos(max(-1.0, min(1.0, dy))))
        ay = math.degrees(math.atan2(dx, dz)) if abs(dx) + abs(dz) > 1e-9 else 0.0
        mid = [(a[i] + b[i]) / 2 for i in range(3)]
        self.box(mid, (w, length, w), rgb, (ax, ay, 0))

    def polyline(self, pts, w, rgb):
        for p, q in zip(pts, pts[1:]):
            self.rod(p, q, w, rgb)
        for p in pts[1:-1]:
            self.box(p, (w, w, w), rgb)

    def crystal(self, c, h, w, rgb, turn=0.0):
        """Faceted bipyramid crystal of height h and width w: a twisted core plus stepped pointed caps."""
        core = lighter(rgb, 0.25)
        self.box(c, (w, h * 0.4, w), rgb, (0, 45 + turn, 0))
        self.box(c, (w * 0.75, h * 0.46, w * 0.75), core, (0, turn, 0))
        for sgn in (1, -1):
            for i, k in enumerate((0.8, 0.6, 0.4, 0.2)):
                y = c[1] + sgn * h * (0.2 + 0.075 * (i + 0.5))
                self.box((c[0], y, c[2]), (w * k, h * 0.075, w * k), rgb if i % 2 == 0 else core, (0, 45 + turn + 12 * i, 0))

    def orb(self, c, r, rgb):
        """Round-ish orb: three crossed cubes."""
        s = r * 1.7
        self.box(c, (s, s, s), rgb)
        self.box(c, (s * 0.92, s * 0.92, s * 0.92), lighter(rgb, 0.15), (45, 45, 0))
        self.box(c, (s * 0.92, s * 0.92, s * 0.92), darker(rgb, 0.1), (0, 45, 45))

    def hoop(self, c, radius, axis, w, rgb, n=14, tilt=0.0):
        """Ring of n rod segments around c. axis 'x', 'y' or 'z' is the ring's normal; tilt leans it about Z."""
        pts = []
        for i in range(n + 1):
            t = 2 * math.pi * i / n
            u, v = radius * math.cos(t), radius * math.sin(t)
            p = {"y": (u, 0, v), "x": (0, u, v), "z": (u, v, 0)}[axis]
            if tilt:
                a = math.radians(tilt)
                p = (p[0] * math.cos(a) - p[1] * math.sin(a), p[0] * math.sin(a) + p[1] * math.cos(a), p[2])
            pts.append(tuple(c[k] + p[k] for k in range(3)))
        for p, q in zip(pts, pts[1:]):
            self.rod(p, q, w, rgb)

    def write(self, name, display):
        os.makedirs(COLDIR, exist_ok=True)
        for cname, rgb in self.colors.items():
            png(os.path.join(COLDIR, cname + ".png"), rgb)
        tex = {"c" + k: "minegicka3:item/color/" + k for k in self.colors}
        tex["particle"] = "minegicka3:item/color/" + next(iter(self.colors))
        with open(os.path.join(ROOT, "models", "item", name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"textures": tex, "elements": self.elements, "display": display}, f, indent="\t")


STAFF_DISPLAY = {
    "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.62, 0.62, 0.62]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "fixed": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_righthand": {"rotation": [0, 90, 15], "translation": [0, 5, 1], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_lefthand": {"rotation": [0, -90, -15], "translation": [0, 5, 1], "scale": [0.8, 0.8, 0.8]},
    "firstperson_righthand": {"rotation": [0, -70, 20], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 70, -20], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
}
GEM_DISPLAY = {
    "gui": {"rotation": [15, 30, 0], "translation": [0, 0, 0], "scale": [1.25, 1.25, 1.25]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.1, 1.1, 1.1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.4, 0.4, 0.4]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.4, 0.4, 0.4]},
    "firstperson_righthand": {"rotation": [0, 30, 0], "translation": [1, 3, 0], "scale": [0.35, 0.35, 0.35]},
    "firstperson_lefthand": {"rotation": [0, -30, 0], "translation": [1, 3, 0], "scale": [0.35, 0.35, 0.35]},
}
HAT_DISPLAY = {
    "gui": {"rotation": [25, 30, 0], "translation": [0, -1, 0], "scale": [0.8, 0.8, 0.8]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 7, 0], "scale": [1.25, 1.25, 1.25]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, 30, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, -30, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
}

# Staves stand on y = -12 .. about +13 around the model centre.
FOOT, NECK = -12.0, 7.0


def shaft(m, wood=WOOD, band=GOLD, top=NECK, w=1.3):
    m.rod((0, FOOT, 0), (0, top, 0), w, wood)
    m.box((0, FOOT + 0.6, 0), (w + 0.5, 1.4, w + 0.5), band)                      # metal foot cap
    for y in (FOOT + 6.5, top - 4.5):                                          # grip bands
        m.box((0, y, 0), (w + 0.4, 0.8, w + 0.4), band)
    m.box((0, top, 0), (w + 0.9, 1.2, w + 0.9), band)                           # collar under the head


def prongs(m, base_y, tip_y, spread, rgb, n=3, w=0.7):
    for i in range(n):
        a = 2 * math.pi * i / n
        x, z = spread * math.cos(a), spread * math.sin(a)
        m.polyline([(0, base_y, 0), (x * 0.9, base_y + 1.5, z * 0.9), (x, tip_y - 1, z), (x * 0.55, tip_y, z * 0.55)], w, rgb)


def taper(m, pts, w0, w1, rgb):
    """Polyline whose width shrinks from w0 to w1 (horns, tails)."""
    n = len(pts) - 1
    for i, (p, q) in enumerate(zip(pts, pts[1:])):
        m.rod(p, q, w0 + (w1 - w0) * (i + 0.5) / n, rgb)


def staves():
    # Each staff keeps the idea players remember from Minegicka III (a heart, a horn, a cage, a hook...)
    # but is drawn in this project's own chunky, banded style.
    m = Model()  # Staff: golden shaft, violet crystal resting in a gold ring
    shaft(m, GOLD, GOLD_DARK)
    prongs(m, NECK, NECK + 6.5, 2.4, GOLD_DARK)
    m.hoop((0, NECK + 2.2, 0), 2.6, "y", 0.6, GOLD_DARK, n=12)
    m.crystal((0, NECK + 4.2, 0), 7.0, 3.0, (150, 70, 230))
    m.write("staff", STAFF_DISPLAY)

    m = Model()  # Grand: golden shaft, cyan crystal wrapped in a cyan spiral, a horn sweeping back
    cyan = (40, 190, 250)
    shaft(m, GOLD, cyan)
    m.crystal((0, NECK + 4.5, 0), 7.5, 3.0, lighter(cyan, 0.2), turn=20)
    spiral = [(2.4 * math.cos(t / 3), NECK + 0.8 + t * 0.45, 2.4 * math.sin(t / 3)) for t in range(0, 19)]
    m.polyline(spiral, 0.55, cyan)
    taper(m, [(0, NECK + 4, -1.4), (0, NECK + 4.6, -3.0), (0, NECK + 5.8, -4.4), (0, NECK + 7.6, -5.2), (0, NECK + 9.2, -5.0)],
          1.5, 0.35, cyan)
    m.write("staff_grand", STAFF_DISPLAY)

    m = Model()  # Super: golden shaft topped by a big red heart
    red = (225, 25, 40)
    shaft(m, GOLD, red)
    for s in (1, -1):
        m.orb((0, NECK + 5.2, s * 1.5), 1.8, red)
    m.box((0, NECK + 3.4, 0), (2.6, 3.6, 3.6), red, (45, 0, 0))          # the point
    m.box((0, NECK + 5.6, 1.6), (3.2, 0.8, 0.8), lighter(red, 0.45))      # shine
    m.write("staff_super", STAFF_DISPLAY)

    m = Model()  # Hemmy's Might: black iron staff, white orb with a red band
    shaft(m, BLACK, (180, 30, 30))
    prongs(m, NECK, NECK + 4, 2.4, IRON, n=4)
    m.orb((0, NECK + 4.2, 0), 2.6, WHITE)
    m.hoop((0, NECK + 4.2, 0), 2.45, "y", 0.5, (200, 30, 30), n=12)
    m.write("staff_hemmy", STAFF_DISPLAY)

    m = Model()  # Blessing: golden shaft, green orb inside a gold cage, a small cross on top
    shaft(m, GOLD, GOLD_DARK, top=NECK - 1)
    green = (80, 230, 100)
    m.orb((0, NECK + 3, 0), 2.4, green)
    for t in (0, 60, 120):  # three meridian bars make the cage
        a = math.radians(t)
        arc = [(3.1 * math.sin(k / 6 * math.pi) * math.cos(a), NECK + 3 - 3.1 * math.cos(k / 6 * math.pi),
                3.1 * math.sin(k / 6 * math.pi) * math.sin(a)) for k in range(1, 6)]
        m.polyline([(0, NECK - 0.5, 0)] + arc + [(0, NECK + 6.4, 0)], 0.45, GOLD_DARK)
    m.rod((0, NECK + 6.2, 0), (0, NECK + 9.5, 0), 0.7, GOLD)
    m.rod((-1.3, NECK + 8.3, 0), (1.3, NECK + 8.3, 0), 0.7, GOLD)
    m.write("staff_blessing", STAFF_DISPLAY)

    m = Model()  # Destruction: golden shaft, a stack of dark crystals reddening downwards, dark red pommel
    shaft(m, GOLD, (90, 10, 10))
    m.crystal((0, NECK + 1.2, 0), 3.4, 2.2, (150, 20, 20), turn=30)
    m.crystal((0, NECK + 3.4, 0), 4.6, 3.0, (75, 12, 16), turn=10)
    m.crystal((0, NECK + 6.6, 0), 6.5, 4.0, (38, 34, 40))
    m.orb((0, FOOT - 0.6, 0), 1.3, (70, 8, 8))
    m.write("staff_destruction", STAFF_DISPLAY)

    m = Model()  # Telekinesis: golden shaft, grey stone globe circled by a green ring
    shaft(m, GOLD, (40, 170, 90))
    m.crystal((0, NECK + 1.0, 0), 2.2, 1.2, (40, 200, 100))
    m.orb((0, NECK + 4.4, 0), 2.3, (120, 124, 130))
    m.hoop((0, NECK + 4.4, 0), 3.5, "z", 0.55, (50, 210, 110), n=16, tilt=20)
    m.write("staff_telekinesis", STAFF_DISPLAY)

    m = Model()  # Manipulation: golden shaft ending in a purple crescent hook with a white bead
    purple = (150, 60, 210)
    shaft(m, GOLD, purple)
    hook = [(0, NECK, 0)]
    for k in range(1, 10):
        t = k / 9 * math.pi * 1.3
        hook.append((0, NECK + 3.2 - 3.2 * math.cos(t), 3.2 * math.sin(t) * (1 - k / 30)))
    m.polyline(hook, 1.0, purple)
    m.orb(hook[-1], 0.8, WHITE)
    m.write("staff_manipulation", STAFF_DISPLAY)


def gems():
    for name, rgb in [("thingy", (250, 210, 60)), ("thingy_good", (60, 200, 250)), ("thingy_great", (190, 70, 240)),
                      ("resistance_essence", (60, 200, 130))]:
        m = Model()
        m.crystal((0, 0, 0), 11, 5.5, rgb)
        m.write(name, GEM_DISPLAY)
    for el, rgb in ELEMENT_COLORS.items():
        m = Model()
        m.crystal((0, 0, 0), 10, 4.5, rgb, turn=10)
        m.box((0, 0, 0), (1.6, 3, 1.6), lighter(rgb, 0.6), (0, 45, 0))  # glowing heart
        m.write(el + "_essence", GEM_DISPLAY)
    for name, wood, tip in [("stick", WOOD, (250, 210, 60)), ("stick_good", WOOD_DARK, (60, 200, 250)),
                            ("stick_great", (60, 40, 80), (190, 70, 240))]:
        m = Model()
        m.rod((0, -9, 0), (0, 7, 0), 1.2, wood)
        m.box((0, 6.2, 0), (1.7, 0.8, 1.7), GOLD)
        m.crystal((0, 9.0, 0), 4.2, 2.0, tip)
        m.write(name, STAFF_DISPLAY)


def hats():
    def hat(name, body, band, star):
        m = Model(offset=(8, 0, 8))
        m.box((0, 0.5, 0), (15, 1, 15), darker(body, 0.15), (0, 0, 0))    # wide brim
        m.box((0, 0.5, 0), (13.5, 1, 13.5), darker(body, 0.15), (0, 45, 0))
        # cone: tiers shrink and drift backwards, the tip flops over
        y, w, dz = 1.0, 9.0, 0.0
        tiers = []
        for i in range(6):
            h = 2.2
            tiers.append((y + h / 2, w, dz))
            m.box((0, y + h / 2, dz), (w, h, w), body if i % 2 == 0 else lighter(body, 0.06))
            y, w, dz = y + h, w * 0.78, dz + 0.25 * i  # drift backwards (+z) as it narrows
        m.polyline([(0, y, dz), (0, y + 1.6, dz + 0.6), (0, y + 2.2, dz + 2.4)], 1.1, body)
        m.box((0, 2.2, 0), (9.4, 1.4, 9.4), band)                           # hat band
        m.box((0, 2.2, -4.75), (1.6, 1.6, 0.4), star)                       # buckle on the front (north)
        ey, ew, edz = tiers[2]
        m.box((0, ey, edz - ew / 2 - 0.1), (1.3, 1.3, 0.3), star, (0, 0, 45))  # star emblem
        m.write(name, HAT_DISPLAY)
    hat("hat", (24, 22, 28), GOLD, (255, 230, 120))                    # black with a gold band
    hat("hat_risk", (24, 22, 28), (200, 30, 30), (255, 90, 80))         # black with a red band
    hat("hat_resistance", (40, 120, 70), GOLD, (230, 230, 230))         # green
    hat("hat_immunity", (236, 236, 230), (60, 200, 80), (60, 200, 80))  # white with a green band


def vanilla_icons():
    """Mana food reuses the vanilla apple / golden apple / cookie icons."""
    for name, tex in [("magic_apple", "apple"), ("magic_golden_apple", "golden_apple"), ("magic_great_apple", "golden_apple"),
                      ("magic_cookie", "cookie"), ("magic_good_cookie", "cookie"), ("magic_great_cookie", "cookie")]:
        with open(os.path.join(ROOT, "models", "item", name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/" + tex}}, f, indent="\t")


if __name__ == "__main__":
    for f in os.listdir(COLDIR) if os.path.isdir(COLDIR) else []:  # drop colours no model uses any more
        os.remove(os.path.join(COLDIR, f))
    staves()
    gems()
    hats()
    vanilla_icons()
    print("ok", len(os.listdir(COLDIR)), "colours")
