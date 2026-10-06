"""Builds a personal resource pack that puts the original Minegicka III element icons back on the HUD.
The icons are read from YOUR copy of the original 1.7.10 jar and are not part of this mod (we can't redistribute them).

    python tools/make_original_pack.py "<path to minegicka3-1.7.10-1.0.0.jar>" "<output .zip>"

Then put the zip in .minecraft/resourcepacks and enable it (above the mod's own resources)."""
import json
import sys
import zipfile

MAPPING = {
    # original path in the 1.7.10 jar -> path the reborn mod loads
    "assets/minegicka3/textures/drawables/elements.png": "assets/minegicka3/textures/gui/elements.png",
}

PACK_FORMAT = 97  # Minecraft 26.3 resource pack format


def main(src, out):
    with zipfile.ZipFile(src) as jar, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as pack:
        meta = {"pack": {"description": "Minegicka III - original look (from your own jar)",
                         "min_format": PACK_FORMAT, "max_format": PACK_FORMAT}}
        pack.writestr("pack.mcmeta", json.dumps(meta, indent=2))
        for a, b in MAPPING.items():
            pack.writestr(b, jar.read(a))
            print("copied", a, "->", b)
    print("wrote", out)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
