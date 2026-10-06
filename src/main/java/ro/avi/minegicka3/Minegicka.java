package ro.avi.minegicka3;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.avi.minegicka3.item.ModItems;
import ro.avi.minegicka3.net.CastPayload;
import ro.avi.minegicka3.net.LineFxPayload;
import ro.avi.minegicka3.net.SprayFxPayload;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Mana;
import ro.avi.minegicka3.spell.SpellManager;

public class Minegicka implements ModInitializer {
	public static final String MOD_ID = "minegicka3";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
	/** Most elements a spell can hold. */
	public static final int MAX_ELEMENTS = 5;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Mana.init();
		ro.avi.minegicka3.spell.ModEffects.init();
		ro.avi.minegicka3.block.ModBlocks.init();
		ro.avi.minegicka3.entity.ModEntities.init();
		ModItems.init();
		ro.avi.minegicka3.magick.Magicks.init();

		PayloadTypeRegistry.serverboundPlay().register(CastPayload.TYPE, CastPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SprayFxPayload.TYPE, SprayFxPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LineFxPayload.TYPE, LineFxPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CastPayload.TYPE, (msg, ctx) -> onCast(ctx.player(), msg));

		ServerTickEvents.END_SERVER_TICK.register(SpellManager::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SpellManager.stop(handler.player);
			forget(handler.player);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SpellManager.clear());
		LOG.info("Minegicka III Reborn loaded");
	}

	/** Ticks between two staff abilities fired through the network (one per right click on a real client). */
	static final int ABILITY_COOLDOWN = 5;
	private static final java.util.Map<java.util.UUID, Long> LAST_ABILITY = new java.util.HashMap<>();

	/**
	 * Handles a cast packet. The client is not trusted: spells need a staff in the main hand, the queue is capped to
	 * the staff's size, weapon casts can't be requested and staff abilities are rate limited.
	 */
	private static void onCast(ServerPlayer player, CastPayload msg) {
		if (msg.action() == CastPayload.STOP) {
			SpellManager.stop(player);
			return;
		}
		if (!(player.getMainHandItem().getItem() instanceof ro.avi.minegicka3.item.StaffItem staff)) return;
		List<Element> els = sanitize(msg.elements(), staff.stats.queue());
		if (msg.action() == CastPayload.MAGICK) {
			ro.avi.minegicka3.magick.Magick m = ro.avi.minegicka3.magick.Magick.match(els);
			if (m != null) {
				ro.avi.minegicka3.magick.Magicks.cast(m, new ro.avi.minegicka3.magick.MagickContext(player.level(), player, SpellManager.staffOf(player)));
			}
			return;
		}
		if (msg.action() == CastPayload.START && msg.elements().length == 0) {
			long now = player.level().getGameTime();
			Long last = LAST_ABILITY.get(player.getUUID());
			if (last != null && now - last < ABILITY_COOLDOWN && now >= last) return;
			LAST_ABILITY.put(player.getUUID(), now);
			staff.triggerAbility(player.level(), player);
			return;
		}
		if (msg.action() == CastPayload.START && !els.isEmpty()) {
			CastType[] types = CastType.values();
			int t = msg.castType();
			// WEAPON is only used internally (imbued weapons), never asked for by a client
			CastType ct = t >= 0 && t < types.length && types[t] != CastType.WEAPON ? types[t] : CastType.SINGLE;
			SpellManager.start(player, els, ct);
		}
	}

	/**
	 * Checks the queue a client sent. It is the client's final queue, not its key presses, so it can't be replayed
	 * through the queue rules (a break-down leaves pairs such as WATER next to LIGHTNING). Only what holds for every
	 * real queue is enforced: known elements, at most max of them, at most one Shield.
	 */
	public static List<Element> sanitize(byte[] sent, int max) {
		List<Element> els = new ArrayList<>();
		for (int i = 0; i < sent.length && els.size() < max; i++) {
			Element e = Element.byId(sent[i]);
			if (e == null || e == Element.SHIELD && els.contains(Element.SHIELD)) continue;
			els.add(e);
		}
		return els;
	}

	public static void forget(ServerPlayer player) {
		LAST_ABILITY.remove(player.getUUID());
	}
}
