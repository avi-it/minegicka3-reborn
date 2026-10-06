package ro.avi.minegicka3.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.block.ShieldBlockEntity;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Nova;
import ro.avi.minegicka3.spell.SpellManager;

/**
 * Earth (and Earth+Ice) projectile. A pure rock rolls, smashes glass, chips shields and bowls through E+2 targets.
 * Mixed with other elements it bursts into a Nova on first contact.
 */
public class BoulderEntity extends MagicEntity {
	private double charged = 0.2, friction = 0.985, gravity = 0.01;
	private int collideLeft = 3, groundTicks;
	private boolean pure = true;

	public BoulderEntity(EntityType<? extends BoulderEntity> type, Level level) {
		super(type, level);
	}

	public void setup(double charge) {
		charged = Math.max(0.2, Math.min(1, charge));
		int e = spell.count(Element.EARTH) + spell.count(Element.ICE);
		double radius = (charged + 0.4) * 0.3 * Math.pow(e, 0.85);
		setSize((float)(radius * 2));
		collideLeft = e + 2;
		friction = 0.99 - 0.005 * e;
		gravity = 0.03 * Math.pow(e, 1.1);
		for (Element el : spell.elements) if (el != Element.EARTH && el != Element.ICE) pure = false;
		setLook(spell.has(Element.ICE) && !spell.has(Element.EARTH) ? LOOK_ICE : LOOK_ROCK);
	}

	@Override
	public boolean canBeCollidedWith(Entity other) {
		return pure && (spell == null || other != spell.owner || spell.cast == CastType.SELF);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) return;
		Vec3 v = getDeltaMovement();
		if (level() instanceof ServerLevel sl && spell != null) {
			if (hitEntities(sl, v) || hitBlocks(sl)) return;
		}
		move(MoverType.SELF, v);
		v = getDeltaMovement();
		if (!onGround()) v = v.add(0, -gravity, 0);
		v = v.scale(friction);
		if (horizontalCollision) {
			v = v.scale(0.7 * friction);
			groundTicks++;
		}
		if (onGround()) {
			groundTicks++;
			if (v.horizontalDistanceSqr() >= 0.1 && tickCount % 3 == 0 && level() instanceof ServerLevel sl
				&& (spell == null || spell.owner == null || spell.owner.distanceToSqr(this) > 16)) {
				BlockState below = sl.getBlockState(blockPosition().below());
				if (!below.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), getY(), getZ(), 2, size() / 2, 0.05, size() / 2, 0.05);
			}
		}
		setDeltaMovement(v);
		if (level() instanceof ServerLevel && (groundTicks >= 100 || tickCount >= 2000 || !pure && tickCount >= 400)) {
			if (!pure) nova();
			discard();
		}
	}

	private boolean hitEntities(ServerLevel sl, Vec3 v) {
		AABB swept = getBoundingBox().expandTowards(v).inflate(0.1);
		List<Entity> list = sl.getEntities(this, swept, e -> e instanceof LivingEntity && e.isAlive() && e != spell.owner && spell.canHit(e));
		if (list.isEmpty()) return false;
		Entity closest = list.stream().min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this))).get();
		if (!pure) {
			nova();
			discard();
			return true;
		}
		spell.affect(closest, 2, 1 + charged * Math.sqrt(v.length()));
		closest.push(v.x * 0.5, 0.2, v.z * 0.5);
		if (--collideLeft <= 0) {
			discard();
			return true;
		}
		return false;
	}

	private boolean hitBlocks(ServerLevel sl) {
		double strength = spell.count() * charged / 2.5;
		AABB box = getBoundingBox().inflate(0.2);
		for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			BlockState st = sl.getBlockState(p);
			if (st.isAir() || !st.getFluidState().isEmpty()) continue;
			if (!pure) {
				nova();
				discard();
				return true;
			}
			if (sl.getBlockEntity(p) instanceof ShieldBlockEntity shield) {
				shield.damage(strength * 15);
			} else if ((st.is(BlockTags.IMPERMEABLE) || st.is(Blocks.GLASS_PANE) || st.is(BlockTags.ICE)) && st.getDestroySpeed(sl, p) < strength) {
				sl.destroyBlock(p.immutable(), true, this);
			}
		}
		return false;
	}

	private void nova() {
		if (level() instanceof ServerLevel sl) {
			sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
			SpellManager.add(new Nova(spell.derive(spell.elements, CastType.AREA, this), position().add(0, 1, 0), 0.1 + charged));
		}
	}
}
