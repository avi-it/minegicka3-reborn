package ro.avi.minegicka3.spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.item.StaffItem;
import ro.avi.minegicka3.spell.exec.SpellExecute;

/** Server-side registry of running spells: one channelled spell per caster plus detached ones. */
public final class SpellManager {
	private static final Map<UUID, Spell> CHANNELLED = new HashMap<>();
	private static final List<Spell> DETACHED = new ArrayList<>();
	private static final List<SpellEffect> EFFECTS = new ArrayList<>();

	private SpellManager() {
	}

	public static void start(LivingEntity caster, List<Element> elements, CastType cast) {
		if (elements.isEmpty()) return;
		stop(caster);
		Spell s = new Spell(elements, cast, caster, staffOf(caster));
		SpellExecute ex = SpellExecute.of(s);
		ex.start(s);
		if (s.finished) return;
		if (s.detached) DETACHED.add(s);
		else CHANNELLED.put(caster.getUUID(), s);
	}

	public static void stop(LivingEntity caster) {
		Spell s = CHANNELLED.remove(caster.getUUID());
		if (s != null && !s.finished) {
			SpellExecute.of(s).stop(s);
			s.finished = true;
		}
	}

	/** Spells that keep running without the caster holding the button (e.g. area rumble). */
	public static void detach(Spell s) {
		s.detached = true;
		DETACHED.add(s);
	}

	/** Runs a free-standing effect (nova, rumble...) until its tick() returns false. */
	public static void add(SpellEffect e) {
		EFFECTS.add(e);
	}

	public static Spell active(LivingEntity caster) {
		return CHANNELLED.get(caster.getUUID());
	}

	public static StaffStats staffOf(LivingEntity e) {
		ItemStack st = e.getMainHandItem();
		if (st.getItem() instanceof StaffItem staff) return staff.stats;
		st = e.getOffhandItem();
		if (st.getItem() instanceof StaffItem staff) return staff.stats;
		return StaffStats.DEFAULT;
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.isDeadOrDying()) continue;
			double mana = Mana.get(p);
			if (mana < Mana.MAX) Mana.set(p, mana + Mana.REGEN * staffOf(p).recover());
		}
		Iterator<Map.Entry<UUID, Spell>> it = CHANNELLED.entrySet().iterator();
		while (it.hasNext()) {
			Spell s = it.next().getValue();
			if (tickOne(s)) {
				it.remove();
			}
		}
		DETACHED.removeIf(SpellManager::tickOne);
		EFFECTS.removeIf(e -> !e.tick());
		SprayManager.tick();
	}

	/** Returns true when the spell is done and should be dropped. */
	private static boolean tickOne(Spell s) {
		if (s.finished) return true;
		if (s.caster.isRemoved() || !s.caster.isAlive() || s.ticks >= 2000) {
			SpellExecute.of(s).stop(s);
			s.finished = true;
			return true;
		}
		if (s.paused) return false;
		s.ticks++;
		SpellExecute.of(s).update(s);
		s.tickCooldowns();
		if (s.finished) {
			SpellExecute.of(s).stop(s);
			return true;
		}
		return false;
	}

	public static void clear() {
		CHANNELLED.clear();
		DETACHED.clear();
		EFFECTS.clear();
		SprayManager.clear();
	}
}
