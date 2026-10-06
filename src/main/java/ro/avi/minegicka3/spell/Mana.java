package ro.avi.minegicka3.spell;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ro.avi.minegicka3.Minegicka;

/** Player mana (default max 1000, raised by magic apples), stored and synced to its owner with data attachments. */
public final class Mana {
	public static final double MAX = 1000;
	/** Base regeneration per tick (10 mana/s), scaled by the held staff's recover stat. */
	public static final double REGEN = 1.0 / 6.0;

	public static final AttachmentType<Double> MANA = AttachmentRegistry.<Double>builder()
		.persistent(Codec.DOUBLE)
		.copyOnDeath()
		.initializer(() -> MAX)
		.syncWith(ByteBufCodecs.DOUBLE, AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(Minegicka.id("mana"));

	public static final AttachmentType<Double> MAX_MANA = AttachmentRegistry.<Double>builder()
		.persistent(Codec.DOUBLE)
		.copyOnDeath()
		.initializer(() -> MAX)
		.syncWith(ByteBufCodecs.DOUBLE, AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(Minegicka.id("max_mana"));

	private Mana() {
	}

	public static void init() {
	}

	public static double get(LivingEntity e) {
		Double d = e.getAttached(MANA);
		return d == null ? MAX : d;
	}

	public static double max(LivingEntity e) {
		Double d = e.getAttached(MAX_MANA);
		return d == null ? MAX : d;
	}

	public static void raiseMax(LivingEntity e, double by) {
		e.setAttached(MAX_MANA, max(e) + by);
		set(e, get(e) + by);
	}

	public static void set(LivingEntity e, double v) {
		double c = Math.max(0, Math.min(max(e), v));
		if (Math.abs(c - get(e)) > 1e-6 || !e.hasAttached(MANA)) e.setAttached(MANA, c);
	}

	public static double consume(LivingEntity e, double amount, boolean mustHaveAll, boolean warn) {
		if (amount <= 0) return 1;
		if (!(e instanceof Player p)) return 1;
		if (p.getAbilities().instabuild) return 1;
		double have = get(p);
		double pay = Math.min(amount, have);
		if (mustHaveAll && have < amount) pay = 0;
		if (pay > 0) set(p, have - pay);
		if (have < amount && warn) {
			p.sendOverlayMessage(Component.literal("That requires " + Math.round(amount) + " mana.").withStyle(ChatFormatting.RED));
		}
		return pay / amount;
	}
}
