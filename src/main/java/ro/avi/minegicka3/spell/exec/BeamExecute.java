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
import ro.avi.minegicka3.spell.Nova;
import ro.avi.minegicka3.spell.Spell;
import ro.avi.minegicka3.spell.SpellManager;

/** Arcane / Life beams. Single = held ray, Area = expanding ring burst, Self = apply to yourself. */
public class BeamExecute extends SpellExecute {
	@Override
	public void start(Spell s) {
		if (s.cast == CastType.AREA) {
			double cost = s.count() * s.count() * 100;
			if (s.consumeMana(cost, true, true) > 0) {
				SpellManager.add(new Nova(s, s.caster.position().add(0, s.caster.getBbHeight() / 2, 0), 1));
			}
			s.finished = true;
		} else if (s.cast == CastType.SELF) {
			double paid = s.consumeMana(s.count() * 50, false, false);
			if (paid > 0) s.affect(s.caster, 0, paid);
			s.finished = true;
		}
	}

	@Override
	public void update(Spell s) {
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
}
