package ro.avi.minegicka3.client;

import java.util.ArrayList;
import java.util.List;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.Minegicka;

/** The client's queued elements, with Minegicka's break-down / cancel / combine rules. */
public final class ElementQueue {
	public static final List<Element> QUEUE = new ArrayList<>();
	/** Recently removed elements with the time they left, for the fade-out on the HUD. */
	public static final List<Removed> REMOVED = new ArrayList<>();

	public record Removed(Element element, int slot, long time) {
	}

	private ElementQueue() {
	}

	public static void push(Element e) {
		Element.pushToQueue(QUEUE, e, maxSize(), (old, slot) -> REMOVED.add(new Removed(old, slot, System.currentTimeMillis())));
	}

	/** Queue length allowed by the held staff (4 basic, 5 grand, 6 super, 13 Hemmy's Might). */
	public static int maxSize() {
		var p = net.minecraft.client.Minecraft.getInstance().player;
		if (p != null && p.getMainHandItem().getItem() instanceof ro.avi.minegicka3.item.StaffItem s) return s.stats.queue();
		return Minegicka.MAX_ELEMENTS;
	}

	public static void clear() {
		for (int i = 0; i < QUEUE.size(); i++) REMOVED.add(new Removed(QUEUE.get(i), i, System.currentTimeMillis()));
		QUEUE.clear();
	}

	public static byte[] bytes() {
		byte[] b = new byte[QUEUE.size()];
		for (int i = 0; i < b.length; i++) b[i] = (byte)QUEUE.get(i).ordinal();
		return b;
	}
}
