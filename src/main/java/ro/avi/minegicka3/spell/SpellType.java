package ro.avi.minegicka3.spell;

import java.util.List;
import ro.avi.minegicka3.Element;

/** The element with the highest priority decides the spell's shape. */
public enum SpellType {
	GROUNDED, PROJECTILE, BEAM, LIGHTNING, SPRAY;

	public static SpellType of(List<Element> els) {
		if (els.contains(Element.SHIELD)) return GROUNDED;
		if (els.contains(Element.EARTH) || els.contains(Element.ICE)) return PROJECTILE;
		if (els.contains(Element.ARCANE) || els.contains(Element.LIFE)) return BEAM;
		if (els.contains(Element.LIGHTNING)) return LIGHTNING;
		return SPRAY;
	}
}
