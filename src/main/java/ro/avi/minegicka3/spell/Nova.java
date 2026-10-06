package ro.avi.minegicka3.spell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.entity.MineEntity;
import ro.avi.minegicka3.net.LineFxPayload;

/**
 * Minegicka's "Nova" (EntityBeamArea): a ring that expands to (4·elements + 12)·scale blocks and hits every
 * entity it reaches once, if it has line of sight. Used by area beams, mines and impure boulders.
 */
public class Nova implements SpellEffect {
	private final Spell spell;
	private final Vec3 center;
	private final double maxRange, scale;
	private final int maxTick;
	private final List<Entity> targets = new ArrayList<>();
	private final Set<Entity> hit = new HashSet<>();
	private int age;

	public Nova(Spell spell, Vec3 center, double scale) {
		this.spell = spell;
		this.center = center;
		this.scale = scale;
		this.maxRange = (spell.count() * 4 + 12) * scale;
		this.maxTick = (int)Math.max(Math.round(maxRange / 16 * 10), 10);
		ServerLevel level = spell.level();
		targets.addAll(level.getEntities(spell.caster, new AABB(center, center).inflate(maxRange, 2, maxRange),
			e -> e instanceof LivingEntity && e.isAlive() || e instanceof MineEntity));
	}

	@Override
	public boolean tick() {
		if (age > maxTick) return false;
		ServerLevel level = spell.level();
		double r = (age + 1.0) / maxTick * maxRange;
		for (Entity e : targets) {
			if (hit.contains(e) || !e.isAlive()) continue;
			if (e.position().subtract(center).horizontalDistance() > r) continue;
			hit.add(e);
			if (!visible(level, e)) continue;
			if (e instanceof MineEntity mine) {
				mine.detonate(); // chain reaction
			} else {
				spell.affect(e, 0, scale);
			}
		}
		ring(level, r);
		age++;
		return true;
	}

	/** Blocked only if all nine lines (centre + 8 corners) hit a solid block. */
	private boolean visible(ServerLevel level, Entity e) {
		AABB b = e.getBoundingBox();
		Vec3[] pts = {b.getCenter(), new Vec3(b.minX, b.minY, b.minZ), new Vec3(b.maxX, b.minY, b.minZ), new Vec3(b.minX, b.maxY, b.minZ),
			new Vec3(b.minX, b.minY, b.maxZ), new Vec3(b.maxX, b.maxY, b.minZ), new Vec3(b.maxX, b.minY, b.maxZ), new Vec3(b.minX, b.maxY, b.maxZ),
			new Vec3(b.maxX, b.maxY, b.maxZ)};
		for (Vec3 p : pts) {
			if (level.clip(new ClipContext(center, p, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) return true;
		}
		return false;
	}

	private void ring(ServerLevel level, double r) {
		int segs = Math.min(31, Math.max(10, (int)(r * 3)));
		List<Vec3> pts = new ArrayList<>();
		for (int i = 0; i < segs; i++) {
			double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
			pts.add(center.add(Math.cos(a0) * r, 0, Math.sin(a0) * r));
			pts.add(center.add(Math.cos(a1) * r, 0, Math.sin(a1) * r));
		}
		List<Integer> cols = new ArrayList<>();
		for (Element el : spell.elements) if (!cols.contains(el.color) && cols.size() < 8) cols.add(el.color); // LineFxPayload holds 8
		LineFxPayload fx = new LineFxPayload(LineFxPayload.NOVA, pts, cols);
		for (ServerPlayer p : PlayerLookup.around(level, center, 96)) ServerPlayNetworking.send(p, fx);
	}
}
