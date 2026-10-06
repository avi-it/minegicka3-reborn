package ro.avi.minegicka3.magick;

import java.util.ArrayList;
import java.util.List;
import ro.avi.minegicka3.Element;

/** A named element combination (exact order) that triggers a special effect instead of a spell. */
public record Magick(int id, String name, List<Element> combo, double baseCost, Action action) {
	public interface Action {
		void cast(MagickContext ctx);
	}

	public static final List<Magick> ALL = new ArrayList<>();

	/** Combo letters as in Minegicka: A Arcane, C Cold, D Shield, E Earth, F Fire, H Lightning, I Ice, L Life, S Steam, W Water. */
	static Magick register(String name, String combo, double cost, Action action) {
		List<Element> els = new ArrayList<>();
		for (char ch : combo.toCharArray()) {
			els.add(switch (ch) {
				case 'A' -> Element.ARCANE;
				case 'C' -> Element.COLD;
				case 'D' -> Element.SHIELD;
				case 'E' -> Element.EARTH;
				case 'F' -> Element.FIRE;
				case 'H' -> Element.LIGHTNING;
				case 'I' -> Element.ICE;
				case 'L' -> Element.LIFE;
				case 'S' -> Element.STEAM;
				case 'W' -> Element.WATER;
				default -> throw new IllegalArgumentException("bad combo letter " + ch);
			});
		}
		Magick m = new Magick(ALL.size(), name, List.copyOf(els), cost, action);
		ALL.add(m);
		return m;
	}

	public static Magick match(List<Element> queue) {
		for (Magick m : ALL) if (m.combo.equals(queue)) return m;
		return null;
	}
}
