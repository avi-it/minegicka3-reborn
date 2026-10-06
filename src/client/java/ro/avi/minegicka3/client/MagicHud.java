package ro.avi.minegicka3.client;

import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.item.StaffItem;
import ro.avi.minegicka3.spell.Mana;

/**
 * Bottom-right wizard HUD (Minegicka layout, mirrored for a bottom anchor): element hotkey grid, mana bar,
 * then the queued elements. Icons are drawn as coloured tiles with the element's initial.
 */
public class MagicHud implements HudElement {
	private static final int W = 83, MARGIN = 2, KEY = 20, Q = 15, GAP = 2;
	private int fade;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null) return;
		boolean staff = MinegickaClient.holdsStaff(p);
		fade = Math.max(0, Math.min(100, fade + (staff ? 20 : -20)));
		if (!staff && fade == 0 && !hasStaffAnywhere(p)) return;
		float f = fade / 100f;
		Font font = mc.font;

		int right = g.guiWidth() - MARGIN;
		int left = right - W;
		int y = g.guiHeight() - MARGIN;

		// Hotkey grid 4x2 (bottom-most)
		int gx = left + (W - KEY * 4) / 2;
		y -= KEY * 2;
		int i = 0;
		for (Map.Entry<Element, KeyMapping> en : MinegickaClient.ELEMENT_KEYS.entrySet()) {
			int x = gx + (i % 4) * KEY, yy = y + (i / 4) * KEY;
			tile(g, font, en.getKey(), x, yy, KEY, 0.25f + 0.75f * f);
			if (staff) {
				String k = en.getValue().getTranslatedKeyMessage().getString();
				if (k.length() > 2) k = k.substring(0, 2);
				g.text(font, k, x + KEY - 1 - font.width(k), yy + KEY - 9, argb(1f, 0xFFFFFF), true);
			}
			i++;
		}

		// Mana bar 80x5
		y -= 5 + 2;
		int bx = left + (W - 80) / 2;
		float ba = 0.4f + 0.6f * f;
		double rate = Mana.get(p) / Mana.MAX;
		g.fill(bx, y, bx + 80, y + 5, argb(ba, 0x333333));
		g.fill(bx + 1, y + 1, bx + 79, y + 4, argb(ba, 0x000000));
		int fw = (int)Math.round(78 * rate);
		int cx = bx + 40;
		int col = Mana.get(p) < MinegickaClient.MIN_MANA ? 0xCCCCCC
			: rgb(0.7, 0.7 * Math.max(0, 0.7 - rate), 0.8 * rate);
		g.fill(cx - fw / 2, y + 1, cx - fw / 2 + fw, y + 4, argb(ba, col));
		if (staff) {
			String t = (int)Math.ceil(Mana.get(p)) + "/" + (int)Mana.MAX;
			g.text(font, t, cx - font.width(t) / 2, y - 9, argb(1f, 0xFFFFFF), true);
		}

		// Queue rows (up to 5 per row), growing upwards
		int n = ElementQueue.QUEUE.size();
		int rows = Math.max(1, (n + 4) / 5);
		y -= (Q + GAP) * rows + 9;
		long now = System.currentTimeMillis();
		ElementQueue.REMOVED.removeIf(r -> now - r.time() > 500);
		for (ElementQueue.Removed r : ElementQueue.REMOVED) {
			float t = (now - r.time()) / 500f;
			int size = Math.max(1, (int)(Q * 0.95f * (1 - t)));
			int qx = left + (r.slot() % 5) * (Q + GAP) + (Q - size) / 2;
			int qy = y + (r.slot() / 5) * (Q + GAP) - (int)(Q * t) + (Q - size) / 2;
			tile(g, null, r.element(), qx, qy, size, f * (1 - t));
		}
		for (int k = 0; k < n; k++) {
			tile(g, font, ElementQueue.QUEUE.get(k), left + (k % 5) * (Q + GAP), y + (k / 5) * (Q + GAP), Q, f);
		}
	}

	private static boolean hasStaffAnywhere(LocalPlayer p) {
		var inv = p.getInventory();
		for (int s = 0; s < inv.getContainerSize(); s++) if (inv.getItem(s).getItem() instanceof StaffItem) return true;
		return false;
	}

	private static void tile(GuiGraphicsExtractor g, Font font, Element e, int x, int y, int size, float alpha) {
		if (alpha <= 0.02f) return;
		g.fill(x, y, x + size, y + size, argb(alpha, 0x101010));
		g.fill(x + 1, y + 1, x + size - 1, y + size - 1, argb(alpha, e.color));
		g.fill(x + 1, y + 1, x + size - 1, y + 3, argb(alpha * 0.35f, 0xFFFFFF));
		if (font != null && size >= 12) {
			String s = e.name().substring(0, 1);
			int txt = (e == Element.COLD || e == Element.SHIELD || e == Element.ICE || e == Element.LIFE) ? 0x202020 : 0xFFFFFF;
			g.text(font, s, x + (size - font.width(s)) / 2 + 1, y + (size - 8) / 2 + 1, argb(alpha, txt), false);
		}
	}

	private static int argb(float a, int rgb) {
		return ((int)(Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static int rgb(double r, double g, double b) {
		return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
	}
}
