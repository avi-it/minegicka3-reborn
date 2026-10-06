package ro.avi.minegicka3.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Nova;
import ro.avi.minegicka3.spell.SpellManager;

/** Shield + Arcane/Life: a mine that sinks to the ground and bursts into a Nova when anything touches or hits it. */
public class MineEntity extends MagicEntity {
	private boolean detonated;

	public MineEntity(EntityType<? extends MineEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) return;
		Vec3 v = getDeltaMovement().add(0, -0.02, 0);
		move(MoverType.SELF, v);
		setDeltaMovement(onGround() ? Vec3.ZERO : v);
		if (level() instanceof ServerLevel sl && tickCount > 10 && !sl.getEntities(this, getBoundingBox(), e -> !(e instanceof MagicEntity) && !e.isSpectator()).isEmpty()) {
			detonate();
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (damage > 0) detonate();
		return damage > 0;
	}

	public void detonate() {
		if (detonated || spell == null) return;
		detonated = true;
		SpellManager.add(new Nova(spell.derive(spell.elements, CastType.AREA, this), position().add(0, 1, 0), 1));
		discard();
	}
}
