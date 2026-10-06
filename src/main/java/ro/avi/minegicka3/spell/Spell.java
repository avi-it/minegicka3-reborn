package ro.avi.minegicka3.spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ro.avi.minegicka3.Element;

/** One running spell on the server: elements, shape, caster and per-target hit cooldowns. */
public class Spell {
	public final List<Element> elements;
	public final SpellType type;
	public CastType cast;
	public final LivingEntity caster;
	public final StaffStats staff;
	public int ticks;
	public boolean finished;
	/** 0..1 charge for projectile spells. */
	public double charge;
	/** Ticks until a beam may change a block again. */
	public int blockCooldown;
	/** Runs on its own, independent of the caster's held button. */
	public boolean detached;
	/** Fixed centre for area spells that outlive the button press. */
	public net.minecraft.world.phys.Vec3 areaCenter;
	/** Per-target cooldown so continuous spells do not hit every tick. */
	private final Map<UUID, Integer> recentlyHit = new HashMap<>();

	public Spell(List<Element> elements, CastType cast, LivingEntity caster, StaffStats staff) {
		this.elements = new ArrayList<>(elements);
		this.type = SpellType.of(elements);
		this.cast = cast;
		this.caster = caster;
		this.staff = staff;
	}

	public ServerLevel level() {
		return (ServerLevel)caster.level();
	}

	public int count() {
		return elements.size();
	}

	public int count(Element e) {
		int c = 0;
		for (Element x : elements) if (x == e) c++;
		return c;
	}

	public boolean has(Element e) {
		return elements.contains(e);
	}

	/** Continuous spells last 75 + 25 per element ticks. */
	public int maxContinuousTicks() {
		return 75 + count() * 25;
	}

	public boolean canHit(Entity e) {
		return !recentlyHit.containsKey(e.getUUID());
	}

	public void tickCooldowns() {
		recentlyHit.replaceAll((k, v) -> v - 1);
		recentlyHit.values().removeIf(v -> v <= 0);
	}

	/**
	 * Applies this spell's elements to an entity. Returns false if the entity is still on cooldown.
	 * Status effects (burning, wet, slow) are applied every call; damage/heal only off cooldown.
	 */
	public boolean affect(Entity target, int cooldown) {
		return affect(target, cooldown, 1.0);
	}

	public boolean affect(Entity target, int cooldown, double scale) {
		SpellDamage.apply(this, target, scale);
		if (!canHit(target)) return false;
		SpellDamage.damage(this, target, scale);
		if (cooldown > 0) recentlyHit.put(target.getUUID(), Math.max(1, (int)(cooldown / staff.atkSpeed())));
		return true;
	}

	/** Consumes mana; returns the fraction actually paid (1 = full). Creative players pay nothing. */
	public double consumeMana(double amount, boolean mustHaveAll, boolean warn) {
		return Mana.consume(caster, amount * staff.consume(), mustHaveAll, warn);
	}

	public boolean isPlayer() {
		return caster instanceof Player;
	}
}
