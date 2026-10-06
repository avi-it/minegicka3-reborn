package ro.avi.minegicka3;

import net.minecraft.ChatFormatting;

/**
 * The ten elements. Eight are bound to keys; Ice and Steam only appear by combining
 * (Cold+Water, Fire+Water). Ordinals are part of the network format.
 */
public enum Element {
	ARCANE(0xFF0000, ChatFormatting.DARK_RED),
	COLD(0xFFFFFF, ChatFormatting.WHITE),
	EARTH(0x382713, ChatFormatting.GOLD),
	FIRE(0xFF4B00, ChatFormatting.RED),
	ICE(0x90FFFF, ChatFormatting.AQUA),
	LIFE(0x00FF00, ChatFormatting.GREEN),
	LIGHTNING(0xFF54FD, ChatFormatting.LIGHT_PURPLE),
	SHIELD(0xFFF638, ChatFormatting.YELLOW),
	STEAM(0xABABAB, ChatFormatting.DARK_GRAY),
	WATER(0x2529FF, ChatFormatting.BLUE);

	public final int color;
	public final ChatFormatting chat;

	Element(int color, ChatFormatting chat) {
		this.color = color;
		this.chat = chat;
	}

	public boolean isOpposite(Element e) {
		return switch (this) {
			case ARCANE -> e == LIFE;
			case COLD -> e == FIRE;
			case EARTH -> e == LIGHTNING;
			case FIRE -> e == COLD;
			case LIFE -> e == ARCANE;
			case LIGHTNING -> e == EARTH || e == WATER;
			case SHIELD -> e == SHIELD;
			case WATER -> e == LIGHTNING;
			default -> false;
		};
	}

	/** Cold+Water=Ice, Fire+Water=Steam. */
	public Element combineWith(Element e) {
		return switch (this) {
			case COLD -> e == WATER ? ICE : null;
			case FIRE -> e == WATER ? STEAM : null;
			case WATER -> e == COLD ? ICE : e == FIRE ? STEAM : null;
			default -> null;
		};
	}

	/** Cold breaks Steam into Water, Fire melts Ice into Water. */
	public Element breakDown(Element e) {
		return switch (this) {
			case COLD -> e == STEAM ? WATER : null;
			case FIRE -> e == ICE ? WATER : null;
			default -> null;
		};
	}

	public String shortName() {
		String n = name();
		return n.charAt(0) + n.substring(1).toLowerCase();
	}

	/**
	 * Adds e to a queue with Minegicka's rules, first match wins: break-down (slot becomes Water), opposites cancel,
	 * combine (Water+Cold=Ice, Water+Fire=Steam), else append if there is room. Replaced or removed elements are reported to onRemoved with their slot.
	 */
	public static void pushToQueue(java.util.List<Element> q, Element e, int max, java.util.function.ObjIntConsumer<Element> onRemoved) {
		for (int i = 0; i < q.size(); i++) {
			if (e.breakDown(q.get(i)) != null) {
				onRemoved.accept(q.get(i), i);
				q.set(i, WATER);
				return;
			}
		}
		for (int i = 0; i < q.size(); i++) {
			if (e.isOpposite(q.get(i))) {
				onRemoved.accept(q.get(i), i);
				q.remove(i);
				return;
			}
		}
		for (int i = 0; i < q.size(); i++) {
			Element c = e.combineWith(q.get(i));
			if (c != null) {
				onRemoved.accept(q.get(i), i);
				q.set(i, c);
				return;
			}
		}
		if (q.size() < max) q.add(e);
	}

	public static Element byId(int id) {
		Element[] v = values();
		return id >= 0 && id < v.length ? v[id] : null;
	}
}
