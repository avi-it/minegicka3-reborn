package ro.avi.minegicka3.spell.exec;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.entity.BoulderEntity;
import ro.avi.minegicka3.entity.IcicleEntity;
import ro.avi.minegicka3.entity.MagicEntity;
import ro.avi.minegicka3.entity.ModEntities;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.EarthRumble;
import ro.avi.minegicka3.spell.Spell;
import ro.avi.minegicka3.spell.SpellManager;

/**
 * Earth / Ice. Single: hold to charge (100 ticks / attack speed), release to throw a boulder or a fan of icicles.
 * Area: ice shards around you and/or an earth rumble. Self: rocks or icicles rain down on you.
 */
public class ProjectileExecute extends SpellExecute {
	private static final double MAX_CHARGE_TICKS = 100;

	@Override
	public void start(Spell s) {
		ServerLevel level = s.level();
		RandomSource rnd = level.getRandom();
		Entity c = s.caster;
		int count = s.count();
		if (s.cast == CastType.AREA) {
			s.finished = true;
			if (s.consumeMana(count * count * 100, true, true) <= 0) return;
			if (s.has(Element.ICE)) {
				int max = count * count * 2 + 4;
				double range = count * 4 + 4, hrange = Math.sqrt(count);
				List<Entity> list = level.getEntities(c, new AABB(c.position(), c.position()).inflate(range, hrange, range),
					e -> e instanceof LivingEntity && e.isAlive() && e != s.owner);
				for (int a = 0; a < max; a++) {
					Vec3 at = c.position().add((rnd.nextDouble() - 0.5) * 2 * range, c.getBbHeight() / 2 + (rnd.nextDouble() - 0.5) * 2 * hrange,
						(rnd.nextDouble() - 0.5) * 2 * range);
					if (!list.isEmpty()) {
						Entity e = list.get(rnd.nextInt(list.size()));
						s.affect(e, 1);
						at = e.getBoundingBox().getCenter();
					}
					level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.3);
				}
				if (s.has(Element.EARTH)) {
					List<Element> earths = new ArrayList<>();
					for (Element e : s.elements) if (e == Element.EARTH) earths.add(e);
					SpellManager.add(new EarthRumble(s.derive(earths, CastType.AREA, c), c.position()));
				}
			} else {
				SpellManager.add(new EarthRumble(s, c.position()));
			}
		} else if (s.cast == CastType.SELF) {
			s.finished = true;
			if (s.consumeMana(count * 75, true, true) <= 0) return;
			if (s.has(Element.EARTH)) {
				for (int a = 0; a < s.count(Element.EARTH); a++) {
					BoulderEntity b = ModEntities.BOULDER.create(level, EntitySpawnReason.TRIGGERED);
					if (b == null) continue;
					b.spell = s;
					b.setup(0.6);
					b.snapTo(c.getX() + (rnd.nextDouble() - 0.5) * b.size() * 4, c.getY() + 8 + b.size(), c.getZ() + (rnd.nextDouble() - 0.5) * b.size() * 4);
					b.setDeltaMovement(0, -1, 0);
					level.addFreshEntity(b);
				}
			} else {
				for (int a = 0; a < count * 4 + 4; a++) {
					spawnIcicle(s, level, new Vec3(c.getX() + (rnd.nextDouble() - 0.5) * 2 * count, c.getY() + 8,
						c.getZ() + (rnd.nextDouble() - 0.5) * 2 * count), new Vec3(0, -1.5, 0));
				}
			}
		}
	}

	@Override
	public void update(Spell s) {
		if (s.cast != CastType.SINGLE) return;
		boolean charging = s.charge < 1;
		s.charge = Math.min(1, s.charge + 1 / MAX_CHARGE_TICKS * s.staff.atkSpeed());
		if (charging) {
			double cost = s.has(Element.EARTH)
				? s.count() * s.count() * 100 / MAX_CHARGE_TICKS * s.staff.atkSpeed()
				: s.count() * 150 / MAX_CHARGE_TICKS;
			if (s.consumeMana(cost, false, false) < 1) s.finished = true;
		}
		// gathering motes in front of the staff
		if (s.ticks % 2 == 0) {
			Vec3 mid = muzzle(s, 0.6, 0.2);
			double spread = 0.6 - 0.4 * s.charge;
			int col = s.has(Element.ICE) && (!s.has(Element.EARTH) || s.ticks % 4 == 0) ? Element.ICE.color : 0x6B4A26;
			s.level().sendParticles(new DustParticleOptions(col, (float)(0.6 + s.charge)), mid.x, mid.y, mid.z,
				(int)Math.ceil(2 * s.charge) + 1, spread, spread, spread, 0);
		}
	}

	@Override
	public void stop(Spell s) {
		if (s.cast != CastType.SINGLE) return;
		ServerLevel level = s.level();
		Entity c = s.caster;
		Vec3 look = c.getLookAngle();
		if (s.has(Element.EARTH)) {
			double charged = s.charge * s.staff.power() * Math.pow(s.count(), 0.4) + 1;
			BoulderEntity b = ModEntities.BOULDER.create(level, EntitySpawnReason.TRIGGERED);
			if (b == null) return;
			b.spell = s;
			b.setup(s.charge);
			double further = Math.sqrt(b.size() * b.size() / 2);
			Vec3 at = c.getEyePosition().add(0, -0.2, 0).add(look.scale(further));
			b.snapTo(at.x, at.y - b.size() / 2, at.z);
			b.setDeltaMovement(look.scale(1.5 * charged / 2));
			level.addFreshEntity(b);
		} else {
			RandomSource rnd = level.getRandom();
			int n = s.count(Element.ICE) * 4 + 4;
			double spread = 0.65 - 0.5 * s.charge;
			double fly = s.staff.power() * (1 + s.charge);
			for (int a = 0; a < n; a++) {
				Vec3 dir = SprayExecute.scatter(look, spread, rnd).normalize();
				spawnIcicle(s, level, c.getEyePosition().add(0, -0.2, 0).add(look.scale(0.2)), dir.scale(fly));
			}
		}
	}

	private static void spawnIcicle(Spell s, ServerLevel level, Vec3 at, Vec3 vel) {
		IcicleEntity i = ModEntities.ICICLE.create(level, EntitySpawnReason.TRIGGERED);
		if (i == null) return;
		i.spell = s;
		i.setSize(0.2f);
		i.setLook(MagicEntity.LOOK_ICE);
		i.snapTo(at.x, at.y, at.z);
		i.setDeltaMovement(vel);
		level.addFreshEntity(i);
	}
}
