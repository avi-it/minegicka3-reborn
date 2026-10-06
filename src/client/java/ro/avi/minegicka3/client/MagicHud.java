package ro.avi.minegicka3.client;

import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import ro.avi.minegicka3.Minegicka;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.item.StaffItem;
import ro.avi.minegicka3.spell.Mana;

/**
 * Bottom-right wizard HUD (Minegicka layout, mirrored for a bottom anchor): element hotkey grid, mana bar,
 * then the queued elements. Element icons come from textures/gui/elements.png (a resource pack can swap in the original).
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

		// Hotkey grid 4x2 (bottom-most) on a dark plate
		int gx = left + (W - KEY * 4) / 2;
		y -= KEY * 2;
		g.fill(gx - 2, y - 2, gx + KEY * 4 + 2, y + KEY * 2 + 2, argb(0.45f * f, 0x0C0A12));
		g.fill(gx - 2, y - 2, gx + KEY * 4 + 2, y - 1, argb(0.6f * f, 0x8A7040));
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
		double rate = Math.max(0, Math.min(1, Mana.get(p) / Mana.max(p)));
		g.fill(bx, y, bx + 80, y + 5, argb(ba, 0x6A5530));
		g.fill(bx + 1, y + 1, bx + 79, y + 4, argb(ba, 0x08060E));
		int fw = (int)Math.round(78 * rate);
		int cx = bx + 40;
		boolean low = Mana.get(p) < MinegickaClient.MIN_MANA;
		// sky blue when full, shifting to violet as it drains; grey when too low to cast
		int col = low ? 0x8A8A8A : rgb(0.35 + 0.35 * (1 - rate), 0.3 + 0.35 * rate, 0.95);
		g.fill(cx - fw / 2, y + 1, cx - fw / 2 + fw, y + 4, argb(ba, col));
		g.fill(cx - fw / 2, y + 1, cx - fw / 2 + fw, y + 2, argb(ba * 0.6f, 0xFFFFFF)); // sheen
		if (staff) {
			String t = (int)Math.ceil(Mana.get(p)) + "/" + (int)Math.ceil(Mana.max(p));
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
		ro.avi.minegicka3.magick.Magick m = ro.avi.minegicka3.magick.Magick.match(ElementQueue.QUEUE);
		if (m != null && staff) {
			String name = m.name() + " (R)";
			g.text(font, name, right - font.width(name), y - 10, argb(f, 0xFFE070), true);
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

	/** Icon sheet: 4 x 5 cells, index = ordinal*2 (+1 greyed). UVs are in 1024x1280 units so any resolution works. */
	private static final Identifier ICONS = Minegicka.id("textures/gui/elements.png");

	private static void tile(GuiGraphicsExtractor g, Font font, Element e, int x, int y, int size, float alpha) {
		if (alpha <= 0.02f) return;
		int idx = e.ordinal() * 2;
		g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, (idx % 4) * 256f, (idx / 4) * 256f, size, size, 256, 256, 1024, 1280,
			argb(alpha, 0xFFFFFF));
	}

	private static int argb(float a, int rgb) {
		return ((int)(Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static int rgb(double r, double g, double b) {
		return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
	}
}
