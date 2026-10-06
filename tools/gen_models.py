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


def staves():
    m = Model()  # Staff: plain wood, three gold prongs holding a violet crystal
    shaft(m)
    prongs(m, NECK, NECK + 6.5, 2.4, GOLD)
    m.crystal((0, NECK + 4.2, 0), 7.0, 3.0, (150, 70, 230))
    m.write("staff", STAFF_DISPLAY)

    m = Model()  # Grand: dark wood, tall cyan crystal circled by a tilted gold halo
    shaft(m, WOOD_DARK, GOLD)
    prongs(m, NECK, NECK + 7, 2.7, GOLD, n=4)
    m.crystal((0, NECK + 5, 0), 9.0, 3.4, (60, 210, 240), turn=20)
    m.hoop((0, NECK + 5, 0), 4.4, "y", 0.6, GOLD, n=16, tilt=18)
    m.write("staff_grand", STAFF_DISPLAY)

    m = Model()  # Super: silver staff, red crystal between two swept wings
    shaft(m, SILVER, GOLD)
    red = (230, 30, 40)
    m.crystal((0, NECK + 4.4, 0), 7.5, 3.2, red)
    for s in (1, -1):
        m.polyline([(0, NECK + 0.5, 0), (0, NECK + 1.5, s * 2.2), (0, NECK + 4.5, s * 3.6), (0, NECK + 7.5, s * 3.0)], 0.8, SILVER)
        m.polyline([(0, NECK + 1.5, s * 2.2), (0, NECK + 3.0, s * 4.2), (0, NECK + 5.5, s * 4.8)], 0.6, lighter(SILVER, 0.3))
    m.write("staff_super", STAFF_DISPLAY)

    m = Model()  # Hemmy's Might: black iron staff, white orb with a red band
    shaft(m, BLACK, (180, 30, 30))
    prongs(m, NECK, NECK + 4, 2.4, IRON, n=4)
    m.orb((0, NECK + 4.2, 0), 2.6, WHITE)
    m.hoop((0, NECK + 4.2, 0), 2.45, "y", 0.5, (200, 30, 30), n=12)
    m.write("staff_hemmy", STAFF_DISPLAY)

    m = Model()  # Blessing: pale wood, green orb inside a gold cage, a small cross on top
    shaft(m, WOOD_PALE, GOLD, top=NECK - 1)
    green = (80, 230, 100)
    m.orb((0, NECK + 3, 0), 2.4, green)
    for t in (0, 60, 120):  # three meridian bars make the cage
        a = math.radians(t)
        arc = [(3.1 * math.sin(k / 6 * math.pi) * math.cos(a), NECK + 3 - 3.1 * math.cos(k / 6 * math.pi),
                3.1 * math.sin(k / 6 * math.pi) * math.sin(a)) for k in range(1, 6)]
        m.polyline([(0, NECK - 0.5, 0)] + arc + [(0, NECK + 6.4, 0)], 0.45, GOLD)
    m.rod((0, NECK + 6.2, 0), (0, NECK + 9.5, 0), 0.7, GOLD)
    m.rod((-1.3, NECK + 8.3, 0), (1.3, NECK + 8.3, 0), 0.7, GOLD)
    m.write("staff_blessing", STAFF_DISPLAY)

    m = Model()  # Destruction: black shaft, jagged shard crown around an ember core
    shaft(m, BLACK, (120, 20, 20))
    m.orb((0, NECK + 3, 0), 1.4, (255, 120, 20))
    for i in range(5):
        a = 2 * math.pi * i / 5
        x, z = math.cos(a), math.sin(a)
        tip = (2.8 * x, NECK + 6.5 + (i % 2) * 1.5, 2.8 * z)
        m.polyline([(0.9 * x, NECK + 0.5, 0.9 * z), (2.2 * x, NECK + 3, 2.2 * z), tip], 0.9, (50, 18, 22))
    m.crystal((0, NECK + 6.5, 0), 3.5, 1.4, (200, 30, 20))
    m.write("staff_destruction", STAFF_DISPLAY)

    m = Model()  # Telekinesis: steel staff, floating cyan orb inside two crossed rings
    shaft(m, IRON, SILVER)
    m.rod((0, NECK, 0), (0, NECK + 1.2, 0), 0.6, SILVER)
    m.orb((0, NECK + 4.2, 0), 2.1, (120, 240, 230))
    m.hoop((0, NECK + 4.2, 0), 3.4, "x", 0.5, SILVER, n=14, tilt=25)
    m.hoop((0, NECK + 4.2, 0), 3.4, "z", 0.5, (60, 200, 120), n=14, tilt=-25)
    m.write("staff_telekinesis", STAFF_DISPLAY)

    m = Model()  # Manipulation: purple staff ending in a crescent hook with a white bead
    shaft(m, (90, 50, 130), SILVER)
    hook = [(0, NECK, 0)]
    for k in range(1, 10):
        t = k / 9 * math.pi * 1.3
        hook.append((0, NECK + 3.2 - 3.2 * math.cos(t), 3.2 * math.sin(t) * (1 - k / 30)))
    m.polyline(hook, 1.0, (150, 80, 210))
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
    hat("hat", (40, 50, 140), GOLD, (255, 230, 120))
    hat("hat_risk", (150, 24, 30), BLACK, (255, 200, 60))
    hat("hat_resistance", (40, 120, 70), WOOD_PALE, (230, 230, 230))
    hat("hat_immunity", (236, 236, 230), (60, 200, 80), GOLD)


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
