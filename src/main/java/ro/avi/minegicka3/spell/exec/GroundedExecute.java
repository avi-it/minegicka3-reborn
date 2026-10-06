package ro.avi.minegicka3.spell.exec;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.block.ModBlocks;
import ro.avi.minegicka3.block.ShieldBlockEntity;
import ro.avi.minegicka3.block.WallBlock;
import ro.avi.minegicka3.block.WallBlockEntity;
import ro.avi.minegicka3.entity.MineEntity;
import ro.avi.minegicka3.entity.ModEntities;
import ro.avi.minegicka3.entity.StormEntity;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.ModEffects;
import ro.avi.minegicka3.spell.Spell;
import ro.avi.minegicka3.spell.SpellType;
import ro.avi.minegicka3.spell.SprayParticle;

/**
 * Shield spells, all one-shot. Self: wards (resistance effects). Single/Area: pure Shield raises a dome,
 * Shield+Earth/Ice raises a wall ring, Shield+Spray/Lightning drops storms, Shield+Arcane/Life lays mines.
 */
public class GroundedExecute extends SpellExecute {
	@Override
	public void start(Spell s) {
		s.finished = true;
		if (!(s.caster instanceof LivingEntity caster)) return;
		if (s.cast == CastType.SELF) {
			wards(s, caster);
			return;
		}
		List<Element> rest = new ArrayList<>(s.elements);
		rest.removeIf(e -> e == Element.SHIELD);
		int count = s.count();
		boolean area = s.cast == CastType.AREA;
		double cost = (count == 1 ? 250 : (count - 1) * (count - 1) * 100) * (area ? 4 : 1);
		if (s.consumeMana(cost, true, true) < 1) return;
		double p = Math.min(Math.sqrt(s.staff.power()), 5);
		ServerLevel level = s.level();
		if (rest.isEmpty()) {
			dome(s, level, caster, 5 * p, area);
		} else if (SpellType.of(rest) == SpellType.PROJECTILE) {
			wall(s.derive(rest, s.cast, caster), level, caster, 3 + count * p, area, count);
		} else {
			radial(s, level, caster, rest, area, count);
		}
	}

	private static void wards(Spell s, LivingEntity c) {
		double pct = s.consumeMana(100 * s.count(), false, false);
		if (pct <= 0) return;
		int amp = (int)Math.ceil(s.staff.power() - 1);
		int physic = s.count() == 1 ? 2 : s.count(Element.EARTH);
		ward(c, MobEffects.WATER_BREATHING, s.count(Element.WATER) + s.count(Element.STEAM), pct, amp);
		ward(c, MobEffects.FIRE_RESISTANCE, s.count(Element.FIRE) + s.count(Element.STEAM), pct, amp);
		ward(c, MobEffects.RESISTANCE, physic, pct, amp);
		ward(c, ModEffects.COLD_RESISTANCE, s.count(Element.COLD) + s.count(Element.ICE), pct, amp);
		ward(c, ModEffects.LIFE_BOOST, s.count(Element.LIFE), pct, amp);
		ward(c, ModEffects.LIGHTNING_RESISTANCE, s.count(Element.LIGHTNING), pct, amp);
		ward(c, ModEffects.ARCANE_RESISTANCE, s.count(Element.ARCANE), pct, amp);
	}

	private static void ward(LivingEntity c, Holder<MobEffect> eff, int n, double pct, int amp) {
		if (n > 0) c.addEffect(new MobEffectInstance(eff, (int)(150 * n * pct), amp));
	}

	private static boolean free(BlockState st) {
		return st.isAir() || st.canBeReplaced() && !st.getFluidState().is(Fluids.LAVA) && !st.getFluidState().is(Fluids.FLOWING_LAVA);
	}

	/** One-block-thick spherical shell; Single only within ~51 degrees of where you look. */
	private static void dome(Spell s, ServerLevel level, LivingEntity c, double r, boolean area) {
		Vec3 center = c.getBoundingBox().getCenter();
		Vec3 look = c.getLookAngle();
		int ir = (int)Math.ceil(r);
		int shell = (int)Math.floor(r);
		BlockPos cp = BlockPos.containing(center);
		for (BlockPos p : BlockPos.betweenClosed(cp.offset(-ir, -ir, -ir), cp.offset(ir, ir, ir))) {
			Vec3 d = Vec3.atCenterOf(p).subtract(center);
			if (Math.ceil(d.length()) != shell) continue;
			if (!area && d.normalize().dot(look) < Math.cos(2 * Math.PI / 7)) continue;
			if (level.isOutsideBuildHeight(p) || !free(level.getBlockState(p))) continue;
			level.setBlockAndUpdate(p, ModBlocks.SHIELD.defaultBlockState());
			if (level.getBlockEntity(p) instanceof ShieldBlockEntity be) be.setup(s);
		}
	}

	/** Ring of spikes around the caster; Single only within 60 degrees of the horizontal look direction. */
	private static void wall(Spell s, ServerLevel level, LivingEntity c, double r, boolean area, int count) {
		Vec3 center = c.getBoundingBox().getCenter();
		Vec3 look = horizontal(c);
		int ir = (int)Math.ceil(r);
		int shell = (int)Math.floor(r);
		int y0 = (int)Math.round(center.y - Math.sqrt(count) - 2), y1 = (int)Math.round(center.y + Math.sqrt(count) + 2);
		boolean icy = s.has(Element.ICE);
		for (int x = -ir; x <= ir; x++) {
			for (int z = -ir; z <= ir; z++) {
				Vec3 d = new Vec3(Math.floor(center.x) + x + 0.5 - center.x, 0, Math.floor(center.z) + z + 0.5 - center.z);
				if (Math.ceil(d.length()) != shell) continue;
				if (!area && d.normalize().dot(look) < Math.cos(Math.PI / 3)) continue;
				for (int y = y0; y <= y1; y++) {
					BlockPos p = BlockPos.containing(center.x + x, y, center.z + z);
					if (level.isOutsideBuildHeight(p) || !free(level.getBlockState(p))) continue;
					level.setBlockAndUpdate(p, ModBlocks.WALL.defaultBlockState().setValue(WallBlock.ICY, icy));
					if (level.getBlockEntity(p) instanceof WallBlockEntity be) be.setup(s);
				}
			}
		}
	}

	/** Storms (spray/lightning) or mines (arcane/life) fanned 90 degrees ahead, or all around for Area. */
	private static void radial(Spell s, ServerLevel level, LivingEntity c, List<Element> rest, boolean area, int count) {
		SpellType t = SpellType.of(rest);
		Vec3 look = horizontal(c);
		double radius = count + 3;
		int loops = (int)Math.round(radius * (area ? 4 : 1)) + (area ? 0 : 1);
		if (t == SpellType.LIGHTNING) radius *= 1.2;
		double step = Math.PI / 2 * (area ? 4 : 1) / loops;
		Vec3 d = SprayParticle.rotateAround(look, new Vec3(0, 1, 0), area ? 0 : -(loops / 2.0 - 0.5) * step);
		for (int i = 0; i < loops; i++, d = SprayParticle.rotateAround(d, new Vec3(0, 1, 0), step)) {
			Vec3 at = c.position().add(d.scale(radius));
			if (t == SpellType.SPRAY || t == SpellType.LIGHTNING) {
				StormEntity storm = ModEntities.STORM.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
				if (storm == null) continue;
				storm.snapTo(at.x, c.getY() + 2 + Math.pow(count, 0.85), at.z);
				level.addFreshEntity(storm);
				storm.begin(s, rest);
			} else {
				MineEntity mine = ModEntities.MINE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
				if (mine == null) continue;
				mine.spell = s.derive(rest, CastType.AREA, mine);
				mine.setSize(0.5f);
				mine.setLook(rest.contains(Element.ARCANE) ? MineEntity.LOOK_ARCANE : MineEntity.LOOK_LIFE);
				mine.snapTo(at.x, c.getY() + 2, at.z);
				level.addFreshEntity(mine);
			}
		}
	}

	private static Vec3 horizontal(LivingEntity c) {
		Vec3 l = c.getLookAngle();
		Vec3 h = new Vec3(l.x, 0, l.z);
		return h.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : h.normalize();
	}
}
