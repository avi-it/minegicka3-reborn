package ro.avi.minegicka3.spell;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.net.SprayFxPayload;

/** Server simulation of spray droplets: damage, knockback and block reactions (ignite, freeze, extinguish). */
public final class SprayManager {
	private record Live(Spell spell, SprayParticle p) {
	}

	private static final List<Live> LIVE = new ArrayList<>();

	private SprayManager() {
	}

	public static void emit(Spell s, Element e, Vec3 pos, Vec3 vel, Vec3 axis) {
		SprayParticle p = new SprayParticle(e, pos, vel, axis, s.count());
		LIVE.add(new Live(s, p));
		SprayFxPayload fx = new SprayFxPayload(pos, vel, axis, e.ordinal(), s.count());
		for (ServerPlayer pl : PlayerLookup.around(s.level(), pos, 64)) ServerPlayNetworking.send(pl, fx);
	}

	static void tick() {
		LIVE.removeIf(SprayManager::tickOne);
	}

	static void clear() {
		LIVE.clear();
	}

	private static boolean tickOne(Live l) {
		SprayParticle p = l.p;
		Spell s = l.spell;
		ServerLevel level = s.level();
		if (s.caster.level() != level || s.caster.isRemoved()) return true;
		Vec3 start = p.pos;
		AABB box = new AABB(start, start).inflate(0.15).expandTowards(p.vel);
		for (Entity e : level.getEntities(s.caster, box, x -> x instanceof LivingEntity && x.isAlive())) {
			s.affect(e, 20);
			if (p.element == Element.WATER) {
				double k = 0.3 / Math.max(0.2, e.getBbWidth() * e.getBbWidth() * e.getBbHeight());
				e.push(p.vel.x * k, p.vel.y * k, p.vel.z * k);
			} else if (p.element == Element.STEAM && p.vel.y > 0) {
				e.fallDistance = Math.max(0, e.fallDistance - p.vel.y);
				e.push(0, -p.vel.y / 3, 0);
			}
		}
		p.move(level);
		if ((p.touching || p.age % 4 == 0) && affectBlocks(level, p, BlockPos.containing(p.pos))) return true;
		if (p.hitBlock != null && affectBlocks(level, p, p.hitBlock)) return true;
		return p.dead;
	}

	/** Returns true if the droplet was consumed by the block reaction. */
	private static boolean affectBlocks(ServerLevel level, SprayParticle p, BlockPos pos) {
		BlockState st = level.getBlockState(pos);
		switch (p.element) {
			case FIRE -> {
				if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.ICE) || st.is(Blocks.POWDER_SNOW)) {
					level.removeBlock(pos, false);
					return true;
				}
				if (p.touching) {
					BlockPos up = st.isAir() ? pos : pos.above();
					if (level.getBlockState(up).isAir() && BaseFireBlock.canBePlacedAt(level, up, net.minecraft.core.Direction.UP)) {
						level.setBlockAndUpdate(up, BaseFireBlock.getState(level, up));
						return true;
					}
				}
			}
			case COLD -> {
				if (st.is(Blocks.WATER) && st.getFluidState().isSource()) {
					level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
					return true;
				}
				if (st.is(Blocks.LAVA)) {
					level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
					return true;
				}
				if (st.is(BlockTags.FIRE)) {
					level.removeBlock(pos, false);
					return true;
				}
				if (p.touching && st.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
					level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
					return true;
				}
			}
			case WATER, STEAM -> {
				if (st.is(BlockTags.FIRE)) {
					level.removeBlock(pos, false);
					return true;
				}
				if (p.element == Element.WATER && st.is(Blocks.LAVA)) {
					level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
					return true;
				}
			}
			default -> {
			}
		}
		return false;
	}
}
