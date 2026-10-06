"""3D item models in the style of Minegicka III: the original drew its staves, sticks, gems and hats in code from
coloured cylinders, octahedra and rings. This rebuilds those shapes as JSON cuboids (26.3 allows free x/y/z element
rotation). Units are the original's (1 = 1/16 block). Run after gen_textures.py: python tools/gen_models.py"""
import json
import math
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "minegicka3")
COLDIR = os.path.join(ROOT, "textures", "item", "color")

YELLOW, CYAN, PURPLE, RED = (255, 255, 100), (29, 177, 255), (197, 0, 204), (200, 0, 0)
ELEMENT_COLORS = {"arcane": (255, 0, 0), "cold": (255, 255, 255), "earth": (56, 39, 19), "fire": (255, 75, 0),
                  "ice": (144, 255, 255), "life": (0, 255, 0), "lightning": (255, 84, 253), "shield": (255, 246, 56),
                  "steam": (171, 171, 171), "water": (37, 41, 255)}


def png(path, rgb):
    raw = b"".join(b"\x00" + bytes(rgb + (255,)) * 4 for _ in range(4))
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 4, 4, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


class Model:
    def __init__(self, offset=(8, 7, 8), scale=1.0):
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

    def cylinder(self, a, b, r, rgb, r2=None):
        """Box from a to b with square section 2r (tapered cylinders become a few stepped boxes)."""
        if r2 is not None and abs(r2 - r) > 1e-6:
            n = 3
            for i in range(n):
                t0, t1 = i / n, (i + 1) / n
                p0 = [a[k] + (b[k] - a[k]) * t0 for k in range(3)]
                p1 = [a[k] + (b[k] - a[k]) * t1 for k in range(3)]
                self.cylinder(p0, p1, max(r + (r2 - r) * (t0 + t1) / 2, 0.04), rgb)
            return
        d = [b[i] - a[i] for i in range(3)]
        length = math.sqrt(sum(x * x for x in d))
        if length < 1e-6:
            return
        dx, dy, dz = (x / length for x in d)
        # rotationZYX(z=0, y, x): Rx first maps +Y to (0, cos a, sin a), then Ry spins it about Y.
        ax = math.degrees(math.acos(max(-1.0, min(1.0, dy))))
        ay = math.degrees(math.atan2(dx, dz)) if abs(dx) + abs(dz) > 1e-9 else 0.0
        mid = [(a[i] + b[i]) / 2 for i in range(3)]
        self.box(mid, (2 * r, length, 2 * r), rgb, (ax, ay, 0))

    def octa(self, c, r, rgb):
        """Minegicka's low-poly 'Sphere(.., 2, 4)': a diamond, i.e. a cube stood on a corner."""
        s = 2 * r / math.sqrt(3) * 1.15
        self.box(c, (s, s, s), rgb, (45, 0, 35.264))

    def sphere(self, c, r, rgb):
        s = r * 1.6
        self.box(c, (s, s, s), rgb)
        self.box(c, (s * 0.9, s * 0.9, s * 0.9), rgb, (45, 45, 0))

    def path(self, pts, r, rgb):
        for i in range(len(pts) - 1):
            self.cylinder(pts[i], pts[i + 1], r, rgb)
        for p in pts[1:-1]:
            self.box(p, (2 * r, 2 * r, 2 * r), rgb)

    def ring(self, c, radius, normal, r, rgb, n=12):
        nx, ny, nz = normal
        # two unit vectors perpendicular to the normal
        u = (0, 0, 1) if abs(nz) < 0.9 else (1, 0, 0)
        v1 = (ny * u[2] - nz * u[1], nz * u[0] - nx * u[2], nx * u[1] - ny * u[0])
        l1 = math.sqrt(sum(x * x for x in v1))
        v1 = tuple(x / l1 for x in v1)
        v2 = (ny * v1[2] - nz * v1[1], nz * v1[0] - nx * v1[2], nx * v1[1] - ny * v1[0])
        pts = []
        for i in range(n + 1):
            t = 2 * math.pi * i / n
            pts.append([c[k] + radius * (math.cos(t) * v1[k] + math.sin(t) * v2[k]) for k in range(3)])
        for i in range(n):
            self.cylinder(pts[i], pts[i + 1], r, rgb)

    def write(self, name, display):
        os.makedirs(COLDIR, exist_ok=True)
        for cname, rgb in self.colors.items():
            png(os.path.join(COLDIR, cname + ".png"), rgb)
        tex = {"c" + k: "minegicka3:item/color/" + k for k in self.colors}
        tex["particle"] = "minegicka3:item/color/" + next(iter(self.colors))
        with open(os.path.join(ROOT, "models", "item", name + ".json"), "w") as f:
            json.dump({"textures": tex, "elements": self.elements, "display": display}, f, indent="\t")


STAFF_DISPLAY = {
    "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.78, 0.78, 0.78]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
    "thirdperson_righthand": {"rotation": [0, 90, 20], "translation": [0, 4, 1], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [0, -90, -20], "translation": [0, 4, 1], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -70, 20], "translation": [1, 2, 0], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [0, 70, -20], "translation": [1, 2, 0], "scale": [0.7, 0.7, 0.7]},
}
GEM_DISPLAY = {
    "gui": {"rotation": [20, 30, 0], "translation": [0, 0, 0], "scale": [1.4, 1.4, 1.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.8, 0.8, 0.8]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.4, 0.4, 0.4]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.4, 0.4, 0.4]},
    "firstperson_righthand": {"rotation": [0, 30, 0], "translation": [1, 3, 0], "scale": [0.35, 0.35, 0.35]},
    "firstperson_lefthand": {"rotation": [0, -30, 0], "translation": [1, 3, 0], "scale": [0.35, 0.35, 0.35]},
}
HAT_DISPLAY = {
    "gui": {"rotation": [25, 30, 0], "translation": [0, -1, 0], "scale": [0.85, 0.85, 0.85]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 6, 0], "scale": [1.3, 1.3, 1.3]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, 30, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, -30, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
}


def shaft(m, top, rgb=YELLOW, knob=None, knob_r=0.75):
    m.cylinder((0, -6, 0), (0, top, 0), 0.4685, rgb)
    m.octa((0, -6.25, 0), knob_r, knob or rgb)


def staves():
    orb = (204, 0, 230)
    m = Model()  # Staff: golden shaft, ring and a purple orb
    shaft(m, 8, YELLOW, (255, 255, 70))
    m.ring((0, 9.5, 0), 1.5, (1, 0, 0), 0.4685, (255, 255, 130))
    m.octa((0, 11, 0), 1.0, orb)
    m.write("staff", STAFF_DISPLAY)

    m = Model()  # Grand: cyan spiral and a horn pointing back
    shaft(m, 8, YELLOW, CYAN)
    m.ring((0, 9.5, 0), 1.5, (1, 0, 0), 0.4685, CYAN)
    m.cylinder((0, 11, -1.25), (0, 11, -2.875), 0.0625, CYAN, 0.4685)
    m.cylinder((0, 11, -2.875), (0, 11, -4.5), 0.4685, CYAN, 0.0625)
    m.write("staff_grand", STAFF_DISPLAY)

    m = Model()  # Super: red heart-shaped head
    shaft(m, 8, YELLOW, RED)
    m.path([(0, 12.5, 0), (0, 12.5, 2.5), (0, 11.5, 3.2), (0, 8, 0.6), (0, 8, -0.6), (0, 11.5, -3.2), (0, 12.5, -2.5), (0, 12.5, 0)], 0.4685, RED)
    m.path([(0, 10, -2), (0, 10.7, -2.3), (0, 12, -1.25), (0, 12, 1.25), (0, 10.9, 2.1), (0, 10.3, 1.7), (0, 9.7, -1.1),
            (0, 9.5, -1.2), (0, 8.8, -0.6), (0, 8.8, 0.6), (0, 9.3, 1.1), (0, 9.3, 2)], 0.234, RED)
    m.write("staff_super", STAFF_DISPLAY)

    m = Model()  # Hemmy's Might: black with a white orb
    shaft(m, 8, (10, 10, 10), (240, 240, 240))
    m.ring((0, 9.5, 0), 1.5, (1, 0, 0), 0.4685, (15, 15, 15))
    m.octa((0, 11, 0), 1.0, (240, 240, 240))
    m.write("staff_hemmy", STAFF_DISPLAY)

    m = Model(offset=(8, 5, 8))  # Blessing: green globe in a golden cage, cross on top
    shaft(m, 7, YELLOW, (100, 255, 100))
    m.sphere((0, 10, 0), 3.0, (100, 255, 100))
    m.ring((0, 10, 0), 3.0, (0, 1, 0), 0.35, YELLOW, 16)
    m.ring((0, 10, 0), 3.0, (0, 0, 1), 0.35, YELLOW, 16)
    m.ring((0, 10, 0), 3.0, (1, 0, 0), 0.35, YELLOW, 16)
    m.cylinder((0, 13, 0), (0, 16, 0), 0.35, YELLOW)
    m.cylinder((0, 14.5, -1.5), (0, 14.5, 1.5), 0.35, YELLOW)
    m.write("staff_blessing", STAFF_DISPLAY)

    m = Model()  # Destruction: dark stacked diamonds, dark red pommel
    m.cylinder((0, -6, 0), (0, 8, 0), 0.4685, YELLOW)
    m.octa((0, 10, 0), 2.75, (40, 40, 40))
    m.octa((0, 8, 0), 2.0, (60, 0, 0))
    m.octa((0, 6.5, 0), 1.5, (120, 0, 0))
    m.sphere((0, -6, 0), 1.5, (40, 0, 0))
    m.write("staff_destruction", STAFF_DISPLAY)

    m = Model()  # Telekinesis: grey globe with a green ring
    shaft(m, 7, YELLOW, (100, 100, 100))
    m.octa((0, 7, 0), 0.4685, (0, 200, 0))
    m.sphere((0, 11, 0), 2.0, (100, 100, 100))
    m.ring((0, 11, 0), 2.95, (0, 0, 1), 0.25, (0, 200, 0), 16)
    m.write("staff_telekinesis", STAFF_DISPLAY)

    m = Model()  # Manipulation: purple hook
    shaft(m, 5, YELLOW, PURPLE)
    m.path([(0, 9, 3), (0, 10.75, 3), (0, 11.75, 3), (0, 13.25, 2), (0, 14, 0), (0, 13, -2), (0, 11, -3), (0, 9.75, -2.25),
            (0, 9.25, -1), (0, 8, 0), (0, 7, 0), (0, 2, 0)], 0.5, PURPLE)
    m.sphere((0, 6, 0), 0.5, PURPLE)
    m.write("staff_manipulation", STAFF_DISPLAY)


def gems():
    for name, rgb in [("thingy", YELLOW), ("thingy_good", CYAN), ("thingy_great", PURPLE), ("resistance_essence", (60, 200, 120))]:
        m = Model(offset=(8, 8, 8))
        m.octa((0, 0, 0), 4.5, rgb)
        m.write(name, GEM_DISPLAY)
    for el, rgb in ELEMENT_COLORS.items():
        m = Model(offset=(8, 8, 8))
        m.octa((0, 0, 0), 3.5, rgb)
        m.write(el + "_essence", GEM_DISPLAY)
    for name, rgb in [("stick", YELLOW), ("stick_good", CYAN), ("stick_great", PURPLE)]:
        m = Model(offset=(8, 1, 8), scale=2.0)
        m.cylinder((0, 0, 0), (0, 7, 0), 0.4685, rgb)
        m.write(name, STAFF_DISPLAY)


def hats():
    def hat(name, body, band, brim_band=True):
        m = Model(offset=(8, -4.8, 8), scale=12.0)
        m.cylinder((0, 0.4, 0), (0, 0.51, 0), 0.8, body, 0.3)
        m.cylinder((0, 0.51, 0), (0, 0.8, 0), 0.3, body, 0.2)
        m.cylinder((0, 0.8, 0), (0.1, 1.21, -0.1), 0.2, body, 0.08)
        m.cylinder((0.1, 1.21, -0.1), (0.2, 1.38, -0.2), 0.08, body, 0.02)
        m.cylinder((0, 0.5, 0), (0, 0.53, 0), 0.33, band)
        if brim_band:
            m.cylinder((0, 0.395, 0), (0, 0.405, 0), 0.8, band)
        m.write(name, HAT_DISPLAY)
    hat("hat", (10, 10, 10), YELLOW)
    hat("hat_risk", (10, 10, 10), RED)
    hat("hat_resistance", (40, 120, 70), YELLOW, False)
    hat("hat_immunity", (255, 255, 255), (0, 255, 0))


def vanilla_icons():
    """The original used the vanilla apple / golden apple / cookie icons for its food."""
    for name, tex in [("magic_apple", "apple"), ("magic_golden_apple", "golden_apple"), ("magic_great_apple", "golden_apple"),
                      ("magic_cookie", "cookie"), ("magic_good_cookie", "cookie"), ("magic_great_cookie", "cookie")]:
        with open(os.path.join(ROOT, "models", "item", name + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/" + tex}}, f, indent="\t")


if __name__ == "__main__":
    staves()
    gems()
    hats()
    vanilla_icons()
    print("ok", len(os.listdir(COLDIR)), "colours")
