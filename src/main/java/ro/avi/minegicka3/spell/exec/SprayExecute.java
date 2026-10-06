package ro.avi.minegicka3.spell.exec;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Spell;
import ro.avi.minegicka3.spell.SprayManager;
import ro.avi.minegicka3.spell.SprayParticle;

/** Fire / Cold / Water / Steam streams. Area casts spin a ring around you, Self casts rain on you. */
public class SprayExecute extends SpellExecute {
	@Override
	public void update(Spell s) {
		if (s.ticks > s.maxContinuousTicks()) {
			s.finished = true;
			return;
		}
		RandomSource rnd = s.level().getRandom();
		int count = s.count();
		double power = Math.min(Math.pow(count * s.staff.power(), 0.4) * 1.25, 5.0);
		double consume = Math.pow(count, 1.2) * 1.5;
		int loop = 2;
		Vec3 dir = s.caster.getLookAngle();
		if (s.cast == CastType.AREA) {
			dir = new Vec3(dir.x, dir.y / 100, dir.z);
			if (dir.horizontalDistanceSqr() < 1e-4) dir = new Vec3(1, 0, 1);
			dir = SprayParticle.rotateAround(dir.normalize(), new Vec3(0, 1, 0), Math.PI / 16 * s.ticks);
			loop *= 2;
			consume *= 1.5;
		}
		loop = (int)(loop * Math.sqrt(s.staff.power()));
		double paid = s.consumeMana(consume, false, false);
		power *= paid;
		if (paid < 1) s.finished = true;

		Vec3 pos = s.caster.getEyePosition().add(dir.scale(s.caster.getBbWidth() / 2));
		if (s.cast == CastType.AREA) pos = pos.add(0, s.caster.getLookAngle().y, 0);
		for (int a = 0; a < loop; a++) {
			for (Element e : s.elements) {
				if (rnd.nextInt(2 * count) > 6) continue;
				Vec3 d = dir, p = pos;
				if (s.cast == CastType.SELF) {
					p = new Vec3(s.caster.getX() + (rnd.nextDouble() - 0.5) * count, s.caster.getEyeY() + 3, s.caster.getZ() + (rnd.nextDouble() - 0.5) * count);
					d = new Vec3(0, -1, 0);
				}
				SprayManager.emit(s, e, p, scatter(d, 0.5, rnd).scale(power).add(s.caster.getDeltaMovement()), d);
			}
		}
	}

	/** Tilts dir by a random amount (≤ scatter) in a random direction. */
	static Vec3 scatter(Vec3 dir, double scatter, RandomSource rnd) {
		Vec3 perp = Math.abs(dir.y) < 0.99 ? dir.cross(new Vec3(0, 1, 0)).normalize() : new Vec3(1, 0, 0);
		perp = SprayParticle.rotateAround(perp, dir, rnd.nextDouble() * Math.PI * 2);
		return dir.add(perp.scale(rnd.nextDouble() * scatter));
	}
}
