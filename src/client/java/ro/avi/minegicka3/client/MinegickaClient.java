package ro.avi.minegicka3.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.Minegicka;
import ro.avi.minegicka3.entity.ModEntities;
import ro.avi.minegicka3.item.StaffItem;
import ro.avi.minegicka3.net.CastPayload;
import ro.avi.minegicka3.net.LineFxPayload;
import ro.avi.minegicka3.net.SprayFxPayload;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Mana;

public class MinegickaClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Minegicka.id("minegicka3"));
	/** Element keys in HUD order (row 1: Water Life Shield Cold, row 2: Lightning Arcane Earth Fire). */
	public static final Map<Element, KeyMapping> ELEMENT_KEYS = new LinkedHashMap<>();
	public static KeyMapping keyUtility;

	/** Minimum mana to start a spell. */
	static final double MIN_MANA = 20;

	static boolean casting;
	static boolean selfCasting;

	@Override
	public void onInitializeClient() {
		key(Element.WATER, InputConstants.KEY_Y);
		key(Element.LIFE, InputConstants.KEY_U);
		key(Element.SHIELD, InputConstants.KEY_I);
		key(Element.COLD, InputConstants.KEY_N); // O is Friends in 26.3
		key(Element.LIGHTNING, InputConstants.KEY_H);
		key(Element.ARCANE, InputConstants.KEY_J);
		key(Element.EARTH, InputConstants.KEY_K);
		key(Element.FIRE, InputConstants.KEY_M); // L is Advancements in 26.3
		keyUtility = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.minegicka3.utility", InputConstants.KEY_R, CATEGORY));

		ClientPlayNetworking.registerGlobalReceiver(SprayFxPayload.TYPE, (p, ctx) -> ClientFx.onSpray(p));
		ClientPlayNetworking.registerGlobalReceiver(LineFxPayload.TYPE, (p, ctx) -> ClientFx.onLines(p));
		ClientTickEvents.END_CLIENT_TICK.register(MinegickaClient::tick);
		// Holding a staff, left click is a self-cast instead of an attack.
		ClientPreAttackCallback.EVENT.register((mc, player, clicks) -> holdsStaff(player));
		HudElementRegistry.addLast(Minegicka.id("hud"), new MagicHud());
		EntityRendererRegistry.register(ModEntities.BOULDER, MagicEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.ICICLE, MagicEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.MINE, MagicEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.STORM, MagicEntityRenderer::new);
	}

	private static void key(Element e, int code) {
		ELEMENT_KEYS.put(e, KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.minegicka3.element." + e.name().toLowerCase(), code, CATEGORY)));
	}

	static boolean holdsStaff(LocalPlayer p) {
		return p != null && p.getMainHandItem().getItem() instanceof StaffItem;
	}

	private static void tick(Minecraft mc) {
		ClientFx.tick(mc);
		LocalPlayer p = mc.player;
		if (p == null) {
			casting = selfCasting = false;
			ElementQueue.QUEUE.clear();
			return;
		}
		boolean staff = holdsStaff(p);
		boolean guiOpen = mc.gui.screen() != null;

		// Stop: right button released (no longer using the staff), or left button released for self-cast.
		if (casting) {
			boolean still = selfCasting ? mc.options.keyAttack.isDown() && !guiOpen && staff : p.isUsingItem() && staff;
			if (!still) stopCast(p);
		}
		if (!casting && staff) {
			for (Map.Entry<Element, KeyMapping> en : ELEMENT_KEYS.entrySet()) {
				while (en.getValue().consumeClick()) {
					if (!p.isUsingItem()) ElementQueue.push(en.getKey());
				}
			}
			while (keyUtility.consumeClick()) {
				if (p.isShiftKeyDown()) {
					ElementQueue.clear();
				} else if (!ElementQueue.QUEUE.isEmpty()) {
					ClientPlayNetworking.send(new CastPayload(CastPayload.MAGICK, ElementQueue.bytes(), 0));
					ElementQueue.clear();
				}
			}
			while (ElementQueue.QUEUE.size() > ElementQueue.maxSize()) ElementQueue.QUEUE.removeLast();
			if (p.isUsingItem() && ElementQueue.QUEUE.isEmpty()) {
				// empty queue: the staff's own active ability (if any)
				casting = true;
				ClientPlayNetworking.send(new CastPayload(CastPayload.START, new byte[0], 0));
			} else if (p.isUsingItem()) {
				startCast(p, p.isShiftKeyDown() ? CastType.AREA : CastType.SINGLE);
			} else if (!guiOpen && mc.options.keyAttack.isDown() && !ElementQueue.QUEUE.isEmpty()) {
				selfCasting = true;
				startCast(p, CastType.SELF);
			}
		}
		// Drain presses so they don't buffer while casting or without a staff.
		ELEMENT_KEYS.values().forEach(k -> {
			while (k.consumeClick()) {
			}
		});
		while (keyUtility.consumeClick()) {
		}
	}

	private static void startCast(LocalPlayer p, CastType type) {
		casting = true;
		if (Mana.get(p) < MIN_MANA && !p.getAbilities().instabuild) return;
		ClientPlayNetworking.send(new CastPayload(CastPayload.START, ElementQueue.bytes(), type.ordinal()));
	}

	private static void stopCast(LocalPlayer p) {
		ClientPlayNetworking.send(new CastPayload(CastPayload.STOP, new byte[0], 0));
		casting = selfCasting = false;
		ElementQueue.clear();
	}
}
