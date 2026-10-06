"""Writes item model JSON for every item texture, and the English names. Run after gen_textures.py."""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "minegicka3")
TEX = os.path.join(ROOT, "textures", "item")

NAMES = {
    "staff": "Staff", "staff_grand": "Grand Staff", "staff_super": "Super Staff", "staff_hemmy": "Hemmy's Might",
    "staff_blessing": "Blessing Staff", "staff_destruction": "Destruction Staff", "staff_telekinesis": "Telekinesis Staff",
    "staff_manipulation": "Manipulation Staff",
    "thingy": "Thingy", "thingy_good": "Good Thingy", "thingy_great": "Great Thingy",
    "stick": "The Stick", "stick_good": "The Good Stick", "stick_great": "The Great Stick",
    "magic_apple": "Magic Apple", "magic_golden_apple": "Magic Golden Apple", "magic_great_apple": "Great Magic Golden Apple",
    "magic_cookie": "Magic Cookie", "magic_good_cookie": "Good Magic Cookie", "magic_great_cookie": "Great Magic Cookie",
    "resistance_essence": "Essence of Resistance",
    "hat": "Wizard Hat", "hat_risk": "Hat of Risk", "hat_resistance": "Hat of Resistance", "hat_immunity": "Hat of Immunity",
}
for el in ["arcane", "cold", "earth", "fire", "ice", "life", "lightning", "shield", "steam", "water"]:
    NAMES[el + "_essence"] = el.capitalize() + " Essence"

os.makedirs(os.path.join(ROOT, "items"), exist_ok=True)
os.makedirs(os.path.join(ROOT, "models", "item"), exist_ok=True)
for f in sorted(x for x in os.listdir(TEX) if x.endswith(".png")):
    n = f[:-4]
    handheld = n.startswith("staff") or n.startswith("stick")
    with open(os.path.join(ROOT, "items", n + ".json"), "w") as o:
        json.dump({"model": {"type": "minecraft:model", "model": "minegicka3:item/" + n}}, o, indent="\t")
    with open(os.path.join(ROOT, "models", "item", n + ".json"), "w") as o:
        json.dump({"parent": "minecraft:item/" + ("handheld" if handheld else "generated"),
                   "textures": {"layer0": "minegicka3:item/" + n}}, o, indent="\t")

lang_path = os.path.join(ROOT, "lang", "en_us.json")
with open(lang_path, encoding="utf-8") as f:
    lang = json.load(f)
for k, v in NAMES.items():
    lang["item.minegicka3." + k] = v
with open(lang_path, "w", encoding="utf-8") as f:
    json.dump(lang, f, indent="\t", ensure_ascii=False)
print("ok", len(os.listdir(TEX)), "items")
