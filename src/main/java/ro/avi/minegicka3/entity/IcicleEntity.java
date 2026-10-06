package ro.avi.minegicka3.entity;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ice shard: light arc, sticks where it lands, shatters on the first living thing it meets. */
public class IcicleEntity extends MagicEntity {
	private int stuckTicks;

	public IcicleEntity(EntityType<? extends IcicleEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) return;
		Vec3 v = getDeltaMovement();
		if (!horizontalCollision && stuckTicks == 0) v = v.add(0, -0.01, 0).scale(0.98);
		if (level() instanceof ServerLevel sl && spell != null && stuckTicks == 0) {
			List<Entity> hit = sl.getEntities(this, getBoundingBox().expandTowards(v).inflate(0.1),
				e -> e instanceof LivingEntity && e.isAlive() && e != spell.owner);
			if (!hit.isEmpty()) {
				Entity t = hit.stream().min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this))).get();
				spell.affect(t, 0);
				discard();
				return;
			}
		}
		move(MoverType.SELF, v);
		if (horizontalCollision || onGround() || stuckTicks > 0) {
			v = Vec3.ZERO;
			stuckTicks++;
		}
		setDeltaMovement(v);
		if (!level().isClientSide() && (stuckTicks >= 200 || tickCount >= 2000)) discard();
	}
}
