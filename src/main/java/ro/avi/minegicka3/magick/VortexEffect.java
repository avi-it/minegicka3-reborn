package ro.avi.minegicka3.magick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.spell.SpellEffect;

/**
 * Vortex magick: a black hole that pulls everything within `range`, kills what reaches its core and eats blocks
 * shell by shell every `interval` ticks. Unbreakable blocks (bedrock, portals) are left alone.
 */
public class VortexEffect implements SpellEffect {
	private final ServerLevel level;
	private final Vec3 c;
	private final double range, power;
	private final int interval;
	private final LivingEntity caster;
	private int life;

	public VortexEffect(ServerLevel level, Vec3 c, int life, double range, int interval, double power, LivingEntity caster) {
		this.level = level;
		this.c = c;
		this.life = life;
		this.range = range;
		this.interval = interval;
		this.power = power;
		this.caster = caster;
	}

	@Override
	public boolean tick() {
		if (--life < 0) return false;
		// swirl
		for (int a = 0; a < 6; a++) {
			double th = level.getRandom().nextDouble() * Math.PI * 2, ph = Math.acos(level.getRandom().nextDouble() * 2 - 1);
			Vec3 p = c.add(Math.sin(ph) * Math.cos(th) * range * 0.6, Math.cos(ph) * range * 0.6, Math.sin(ph) * Math.sin(th) * range * 0.6);
			Vec3 v = c.subtract(p).scale(0.08);
			int col = new int[] {0x96008C, 0x96005A, 0xA0128E, 0x891B64}[a % 4];
			level.sendParticles(new DustParticleOptions(col, 2f), p.x, p.y, p.z, 0, v.x, v.y, v.z, 1);
		}
		level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 10, 0.3, 0.3, 0.3, 1);
		// pull
		for (Entity e : level.getEntities((Entity)null, new AABB(c, c).inflate(range), e -> e.isAlive() && !e.isSpectator())) {
			Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
			if (d.lengthSqr() > range * range) continue;
			double k = power / 4 / (e instanceof FallingBlockEntity ? 12 : 1) / (e instanceof Player p && p.getAbilities().instabuild ? 4 : 1);
			Vec3 pull = d.normalize().scale(k * 0.25);
			e.push(pull.x, pull.y, pull.z);
			if (d.lengthSqr() <= 4 && !(e instanceof Player p && p.getAbilities().instabuild)) {
				if (e instanceof LivingEntity le) le.hurtServer(level, level.damageSources().indirectMagic(e, caster), 9999);
				else e.discard();
			}
		}
		if (life % interval == 0) eat();
		return true;
	}

	/** Block offsets of each shell (a - 1 < distance <= a), built once per radius instead of scanning a cube. */
	private static final java.util.Map<Integer, java.util.List<BlockPos>> SHELLS = new java.util.concurrent.ConcurrentHashMap<>();

	public static java.util.List<BlockPos> shell(int a) {
		return SHELLS.computeIfAbsent(a, r -> {
			java.util.List<BlockPos> out = new java.util.ArrayList<>();
			for (BlockPos p : BlockPos.betweenClosed(new BlockPos(-r, -r, -r), new BlockPos(r, r, r))) {
				double dist = Math.sqrt(p.distSqr(BlockPos.ZERO));
				if (dist <= r && dist > r - 1) out.add(p.immutable());
			}
			return java.util.List.copyOf(out);
		});
	}

	private void eat() {
		BlockPos center = BlockPos.containing(c);
		for (int a = 0; a < range; a++) {
			boolean took = false;
			for (BlockPos off : shell(a)) {
				BlockPos p = center.offset(off);
				BlockState st = level.getBlockState(p);
				if (st.isAir() || !st.getFluidState().isEmpty() || st.getDestroySpeed(level, p) < 0 || level.getBlockEntity(p) != null) continue;
				if (a <= 3) {
					level.removeBlock(p, false);
					took = true;
				} else if (level.getRandom().nextInt(a + 1) == 0) {
					if (level.getRandom().nextInt(4) == 0) FallingBlockEntity.fall(level, p, st);
					else level.removeBlock(p, false);
					took = true;
				}
			}
			if (took) return;
		}
	}
}
