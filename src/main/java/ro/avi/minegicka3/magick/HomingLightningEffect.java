package ro.avi.minegicka3.magick;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.spell.SpellEffect;

/** Homing Lightning: glowing orbs leap up, home in on nearby creatures and call down a lightning bolt on each. */
public class HomingLightningEffect implements SpellEffect {
	private static class Orb {
		Vec3 pos, vel;
		final Entity target;
		final Vec3 spot;

		Orb(Vec3 pos, Entity target, Vec3 spot) {
			this.pos = pos;
			this.vel = new Vec3(0, 0.7, 0);
			this.target = target;
			this.spot = spot;
		}

		Vec3 dest() {
			return target != null && target.isAlive() ? target.position().add(0, target.getBbHeight() / 2, 0) : spot;
		}
	}

	private final ServerLevel level;
	private final List<Orb> orbs = new ArrayList<>();
	private int age;

	public HomingLightningEffect(MagickContext c, double range, int n) {
		level = c.level();
		RandomSource rnd = level.getRandom();
		List<Entity> pool = new ArrayList<>(c.around(range, 1, false, e -> e instanceof LivingEntity && e.isAlive()));
		Vec3 start = c.caster().getEyePosition();
		for (int i = 0; i < n; i++) {
			Entity t = pool.isEmpty() ? null : pool.remove(rnd.nextInt(pool.size()));
			Vec3 spot = c.pos().add((rnd.nextDouble() - 0.5) * 2 * range, 0, (rnd.nextDouble() - 0.5) * 2 * range);
			orbs.add(new Orb(start.add((rnd.nextDouble() - 0.5) * 0.6, 0, (rnd.nextDouble() - 0.5) * 0.6), t, spot));
		}
	}

	@Override
	public boolean tick() {
		if (++age > 400) return false;
		orbs.removeIf(o -> {
			Vec3 to = o.dest().subtract(o.pos);
			o.vel = o.vel.scale(0.96).add(to.normalize().scale(0.1));
			o.pos = o.pos.add(o.vel);
			level.sendParticles(new DustParticleOptions(0xFFFF77, 1.5f), o.pos.x, o.pos.y, o.pos.z, 2, 0.05, 0.05, 0.05, 0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, o.pos.x, o.pos.y, o.pos.z, 1, 0.1, 0.1, 0.1, 0.05);
			double d2 = o.dest().distanceToSqr(o.pos);
			if (d2 < 4 || d2 < o.vel.lengthSqr()) {
				Magicks.bolt(level, o.dest());
				return true;
			}
			return false;
		});
		return !orbs.isEmpty();
	}
}
