package ro.avi.minegicka3.spell.exec;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.net.LineFxPayload;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Spell;

/** Arcane / Life beams. Single = held ray, Area = expanding ring burst, Self = apply to yourself. */
public class BeamExecute extends SpellExecute {
	@Override
	public void start(Spell s) {
		if (s.cast == CastType.AREA) {
			double cost = s.count() * s.count() * 100;
			if (s.consumeMana(cost, true, true) > 0) {
				// The ring keeps expanding after the button is released.
				s.detached = true;
			} else {
				s.finished = true;
			}
		} else if (s.cast == CastType.SELF) {
			double paid = s.consumeMana(s.count() * 50, false, false);
			if (paid > 0) s.affect(s.caster, 0, paid);
			s.finished = true;
		}
	}

	@Override
	public void update(Spell s) {
		if (s.cast == CastType.AREA) {
			updateArea(s);
			return;
		}
		if (s.consumeMana(s.count() * 2.2, false, false) == 0 || s.ticks > s.maxContinuousTicks()) {
			s.finished = true;
			return;
		}
		double max = 64 + 32 * (s.count(Element.ARCANE) + s.count(Element.LIFE));
		Vec3 from = muzzle(s, 0.3, 0.25);
		Vec3 look = s.caster.getLookAngle();
		Vec3 to = from.add(look.scale(max));
		ServerLevel level = s.level();
		BlockHitResult bh = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, s.caster));
		Vec3 end = bh.getType() == HitResult.Type.MISS ? to : bh.getLocation();
		EntityHitResult eh = ProjectileUtil.getEntityHitResult(level, s.caster, from, end,
			new AABB(from, end).inflate(1), e -> e instanceof LivingEntity && e.isAlive() && e != s.caster, 0.3f);
		if (eh != null) {
			end = eh.getLocation();
			s.affect(eh.getEntity(), 25);
		} else if (bh.getType() == HitResult.Type.BLOCK && s.blockCooldown-- <= 0) {
			affectBlock(s, level, bh.getBlockPos().relative(bh.getDirection()));
		}
		sendLines(s, LineFxPayload.BEAM, List.of(from, end));
	}

	private static void affectBlock(Spell s, ServerLevel level, BlockPos pos) {
		s.blockCooldown = 0;
		int fire = s.count(Element.FIRE), water = s.count(Element.WATER), cold = s.count(Element.COLD), steam = s.count(Element.STEAM);
		int life = s.count(Element.LIFE), arcane = s.count(Element.ARCANE);
		BlockState st = level.getBlockState(pos);
		BlockPos below = pos.below();
		BlockState bs = level.getBlockState(below);
		if (st.isAir() || st.canBeReplaced()) {
			if (fire > 0 && BaseFireBlock.canBePlacedAt(level, pos, net.minecraft.core.Direction.UP)) {
				level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
				s.blockCooldown = 40 / fire;
				return;
			}
			if (cold > 0 && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
				level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
				s.blockCooldown = 40 / cold;
				return;
			}
		}
		if (fire > 0 && st.is(Blocks.SNOW)) {
			level.removeBlock(pos, false);
			s.blockCooldown = 40 / fire;
		} else if (st.is(BlockTags.FIRE) && cold + water + steam > 0) {
			level.removeBlock(pos, false);
			s.blockCooldown = 40 / (cold + water + steam);
		} else if (life > 0 && bs.is(Blocks.DIRT)) {
			level.setBlockAndUpdate(below, Blocks.GRASS_BLOCK.defaultBlockState());
			s.blockCooldown = 40 / life;
		} else if (arcane > 0 && bs.is(Blocks.GRASS_BLOCK)) {
			level.setBlockAndUpdate(below, Blocks.DIRT.defaultBlockState());
			s.blockCooldown = 40 / arcane;
		}
	}

	/** Area beam: a ring that grows to 4 + 2·elements blocks and hits everything it crosses once. */
	private static void updateArea(Spell s) {
		double maxR = 4 + 2 * s.count();
		double r = s.ticks * 0.5;
		if (r > maxR) {
			s.finished = true;
			return;
		}
		Vec3 c = s.areaCenter == null ? (s.areaCenter = s.caster.position().add(0, s.caster.getBbHeight() / 2, 0)) : s.areaCenter;
		AABB box = new AABB(c, c).inflate(r, 1.5, r);
		for (Entity e : s.level().getEntities(s.caster, box, e -> e instanceof LivingEntity && e.isAlive())) {
			double d = Math.sqrt(e.distanceToSqr(c.x, e.getY(), c.z));
			if (d <= r && d >= r - 1.5) s.affect(e, 1000);
		}
		int segs = Math.max(12, (int)(r * 6));
		java.util.ArrayList<Vec3> pts = new java.util.ArrayList<>();
		for (int i = 0; i < segs && pts.size() < 62; i++) {
			double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
			pts.add(c.add(Math.cos(a0) * r, 0, Math.sin(a0) * r));
			pts.add(c.add(Math.cos(a1) * r, 0, Math.sin(a1) * r));
		}
		sendLines(s, LineFxPayload.BEAM, pts);
	}
}
