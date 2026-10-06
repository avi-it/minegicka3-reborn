package ro.avi.minegicka3.spell;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;

/** Area Earth: a shockwave rolling out to 4*count+4 blocks, tossing grounded targets into the air. */
public class EarthRumble implements SpellEffect {
	private final Spell spell;
	private final Vec3 center;
	private final double maxRange;
	private final int maxTick;
	private final List<Entity> targets = new ArrayList<>();
	private int age;

	public EarthRumble(Spell spell, Vec3 center) {
		this.spell = spell;
		this.center = center;
		int count = spell.count();
		this.maxRange = count * 4 + 4;
		this.maxTick = 8 * Math.max(6 - count, 4) + 7;
		targets.addAll(spell.level().getEntities(spell.caster, new AABB(center, center).inflate(maxRange, count * 2 + 1, maxRange),
			e -> e instanceof LivingEntity && e.isAlive() && e != spell.owner));
	}

	@Override
	public boolean tick() {
		if (age > maxTick) return false;
		ServerLevel level = spell.level();
		double r = (double)age / maxTick * maxRange;
		if (age % 8 == 0) {
			targets.removeIf(e -> {
				if (!e.onGround() || !e.isAlive()) return true;
				if (e.position().subtract(center).horizontalDistance() > r) return false;
				spell.affect(e, 0);
				e.push(0, spell.count(Element.EARTH) * spell.staff.power() * 0.15 + 0.3, 0);
				return true;
			});
			level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.3f, 0.5f);
		}
		int n = Math.min(40, (int)(r * 4) + 4);
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			BlockPos p = BlockPos.containing(center.x + Math.cos(a) * r, center.y - 0.5, center.z + Math.sin(a) * r);
			BlockState st = level.getBlockState(p);
			if (st.isAir()) continue;
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), p.getX() + 0.5, p.getY() + 1.05, p.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.15);
		}
		age++;
		return true;
	}
}
