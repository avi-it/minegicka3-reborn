package ro.avi.minegicka3.spell.exec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.net.LineFxPayload;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Spell;

/**
 * Chain lightning. Each hop seeks living targets within 6·√lightning / √(hop+1) blocks inside a 60° cone
 * (any direction for Area), up to 2 + lightning hops deep. Casting while wet shocks yourself.
 */
public class LightningExecute extends SpellExecute {
	private static final double MIN_COS_CONE = 0.5;

	@Override
	public void start(Spell s) {
		boolean wet = s.caster.isInWaterOrRain() && !(s.caster instanceof Player p && p.getAbilities().invulnerable);
		if (wet) {
			s.affect(s.caster, 1);
			s.finished = true;
			return;
		}
		if (s.cast == CastType.SELF) {
			double cost = s.count() * 100 + s.count(Element.LIGHTNING) * 50;
			if (s.consumeMana(cost, true, true) > 0) {
				ServerLevel level = s.level();
				double dist = 4 * s.staff.atkSpeed();
				for (int a = 0; a < s.count(Element.LIGHTNING); a++) {
					LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
					if (bolt == null) continue;
					bolt.snapTo(s.caster.getX() + (level.getRandom().nextDouble() - 0.5) * dist, s.caster.getY(),
						s.caster.getZ() + (level.getRandom().nextDouble() - 0.5) * dist);
					level.addFreshEntity(bolt);
				}
				s.elements.removeIf(e -> e == Element.LIGHTNING);
				s.affect(s.caster, 1);
			}
			s.finished = true;
		}
	}

	@Override
	public void update(Spell s) {
		if (s.consumeMana(s.count() * 2.2, false, false) < 1 || s.ticks > s.maxContinuousTicks()) {
			s.finished = true;
			return;
		}
		Vec3 origin = muzzle(s, 0.3, 0.15);
		List<Vec3> segs = new ArrayList<>();
		Set<Entity> visited = new HashSet<>();
		visited.add(s.caster);
		seek(s, origin, s.caster.getLookAngle(), 0, visited, segs);
		if (segs.isEmpty()) {
			// Nothing in reach: a short crackle in front of the staff.
			Vec3 tip = origin.add(s.caster.getLookAngle().scale(2.5 + s.level().getRandom().nextDouble()));
			segs.add(origin);
			segs.add(tip);
		}
		sendLines(s, LineFxPayload.LIGHTNING, segs);
	}

	private static double radius(Spell s, int hop) {
		int lig = s.count(Element.LIGHTNING);
		if (hop >= 2 + lig) return 0;
		double r = 6 * Math.sqrt(lig) / Math.sqrt(hop + 1);
		return s.cast == CastType.AREA ? r * 1.3 : r;
	}

	private static void seek(Spell s, Vec3 from, Vec3 toward, int hop, Set<Entity> visited, List<Vec3> segs) {
		double r = radius(s, hop);
		if (r <= 0) return;
		ServerLevel level = s.level();
		for (Entity e : level.getEntities((Entity)null, new AABB(from, from).inflate(r), x -> x instanceof LivingEntity && x.isAlive())) {
			if (visited.contains(e)) continue;
			Vec3 c = e.getBoundingBox().getCenter();
			if (c.distanceToSqr(from) > r * r) continue;
			Vec3 to = c.subtract(from).normalize();
			if (s.cast != CastType.AREA && to.dot(toward.normalize()) < MIN_COS_CONE) continue;
			if (level.clip(new ClipContext(from, c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.BLOCK) continue;
			visited.add(e);
			if (segs.size() < 60) {
				segs.add(from);
				segs.add(c);
			}
			s.affect(e, 30);
			seek(s, c, to, hop + 1, visited, segs);
		}
	}
}
