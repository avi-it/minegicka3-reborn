"""Crafting-table recipes. The original used a custom Click-Craft station with big stacks (e.g. 64 dirt);
these are scaled-down shapeless versions that fit a 3x3 grid. Run: python tools/gen_recipes.py"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "minegicka3", "recipe")


def m(n):
    return n if ":" in n else "minegicka3:" + n


def shapeless(name, result, ingredients, count=1):
    flat = []
    for i in ingredients:
        if isinstance(i, tuple):
            flat += [m(i[0])] * i[1]
        else:
            flat.append(m(i))
    assert len(flat) <= 9, (name, len(flat))
    return name, {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": flat,
                  "result": {"id": m(result), "count": count}}


V = "minecraft:"
RECIPES = [
    shapeless("thingy", "thingy", [V + "iron_ingot", V + "gold_ingot"]),
    shapeless("thingy_good", "thingy_good", [("thingy", 9)]),
    shapeless("thingy_great", "thingy_great", [("thingy_good", 9)]),
    shapeless("stick", "stick", [("thingy", 2), V + "stick"]),
    shapeless("stick_good", "stick_good", [("thingy_good", 2), "stick"]),
    shapeless("stick_great", "stick_great", [("thingy_great", 2), "stick_good"]),
    shapeless("magic_apple", "magic_apple", [V + "apple", "thingy"]),
    shapeless("magic_golden_apple", "magic_golden_apple", [V + "golden_apple", "thingy_good"]),
    shapeless("magic_great_apple", "magic_great_apple", [V + "enchanted_golden_apple", "thingy_great"]),
    shapeless("magic_cookie", "magic_cookie", [(V + "cookie", 4), "thingy"], 4),
    shapeless("magic_good_cookie", "magic_good_cookie", [(V + "cookie", 4), "thingy_good"], 4),
    shapeless("magic_great_cookie", "magic_great_cookie", [(V + "cookie", 4), "thingy_great"], 4),
    shapeless("arcane_essence", "arcane_essence", ["thingy", V + "blaze_rod", (V + "fermented_spider_eye", 2), (V + "nether_wart", 2)]),
    shapeless("cold_essence", "cold_essence", ["thingy", (V + "snowball", 8)]),
    shapeless("earth_essence", "earth_essence", ["thingy", (V + "dirt", 3), (V + "cobblestone", 3), V + "obsidian"]),
    shapeless("fire_essence", "fire_essence", ["thingy", V + "flint_and_steel", V + "blaze_rod", (V + "magma_cream", 2)]),
    shapeless("ice_essence", "ice_essence", ["thingy", (V + "ice", 3), (V + "arrow", 3)]),
    shapeless("life_essence", "life_essence", ["thingy", (V + "bone", 3), (V + "wheat_seeds", 3), V + "cake"]),
    shapeless("lightning_essence", "lightning_essence", ["thingy", (V + "iron_ingot", 4), (V + "flint", 2)]),
    shapeless("shield_essence", "shield_essence", ["thingy", (V + "glowstone_dust", 4), V + "golden_apple", V + "iron_door"]),
    shapeless("steam_essence", "steam_essence", ["thingy", (V + "flint_and_steel", 2), (V + "snowball", 4)]),
    shapeless("water_essence", "water_essence", ["thingy", (V + "glass_bottle", 3), V + "water_bucket"]),
    shapeless("resistance_essence", "resistance_essence", ["thingy_great", "arcane_essence", "cold_essence", "earth_essence",
                                                            "fire_essence", "life_essence", "lightning_essence", "shield_essence",
                                                            "water_essence"]),
    shapeless("staff", "staff", [("stick", 3), "thingy", V + "magenta_dye"]),
    shapeless("staff_grand", "staff_grand", [("stick", 2), ("stick_good", 2), "thingy_good"]),
    shapeless("staff_super", "staff_super", [("stick", 2), ("stick_great", 3), "thingy_great", (V + "red_dye", 3)]),
    shapeless("staff_blessing", "staff_blessing", ["staff", ("life_essence", 2), V + "ghast_tear", V + "glistering_melon_slice",
                                                    V + "golden_carrot", V + "glowstone_dust", V + "redstone", V + "pufferfish"]),
    shapeless("staff_destruction", "staff_destruction", ["staff", "arcane_essence", "fire_essence", "earth_essence",
                                                          (V + "gunpowder", 3), V + "flint_and_steel", V + "blaze_powder"]),
    shapeless("staff_telekinesis", "staff_telekinesis", ["staff", "steam_essence", (V + "arrow", 2), (V + "feather", 2), V + "sticky_piston"]),
    shapeless("staff_manipulation", "staff_manipulation", ["staff", "arcane_essence", "life_essence", V + "golden_carrot",
                                                            V + "fermented_spider_eye", V + "redstone", (V + "rotten_flesh", 2)]),
    shapeless("hat", "hat", [(V + "leather", 8), "thingy"]),
    shapeless("hat_risk", "hat_risk", ["hat", "life_essence", (V + "red_dye", 3)]),
    shapeless("hat_resistance", "hat_resistance", ["hat", "resistance_essence"]),
]

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, r in RECIPES:
        with open(os.path.join(OUT, name + ".json"), "w") as f:
            json.dump(r, f, indent="\t")
    print("ok", len(RECIPES), "recipes")
