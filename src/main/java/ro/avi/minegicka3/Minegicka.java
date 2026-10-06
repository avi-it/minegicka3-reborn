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

		PayloadTypeRegistry.serverboundPlay().register(CastPayload.TYPE, CastPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SprayFxPayload.TYPE, SprayFxPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LineFxPayload.TYPE, LineFxPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CastPayload.TYPE, (msg, ctx) -> onCast(ctx.player(), msg));

		ServerTickEvents.END_SERVER_TICK.register(SpellManager::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SpellManager.stop(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SpellManager.clear());
		LOG.info("Minegicka III Reborn loaded");
	}

	private static void onCast(ServerPlayer player, CastPayload msg) {
		if (msg.action() == CastPayload.STOP) {
			SpellManager.stop(player);
			return;
		}
		List<Element> els = new ArrayList<>();
		for (byte b : msg.elements()) {
			Element e = Element.byId(b);
			if (e != null && els.size() < MAX_ELEMENTS) els.add(e);
		}
		if (msg.action() == CastPayload.START) {
			CastType[] types = CastType.values();
			CastType ct = msg.castType() >= 0 && msg.castType() < types.length ? types[msg.castType()] : CastType.SINGLE;
			SpellManager.start(player, els, ct);
		}
	}
}
