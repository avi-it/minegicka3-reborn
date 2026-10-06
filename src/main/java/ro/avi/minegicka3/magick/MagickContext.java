package ro.avi.minegicka3.magick;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.spell.StaffStats;

/** Who casts a magick, where, and with which staff stats (P = power, S = attack speed). */
public record MagickContext(ServerLevel level, LivingEntity caster, StaffStats staff) {
	public double power() {
		return staff.power();
	}

	public double speed() {
		return staff.atkSpeed();
	}

	public Vec3 pos() {
		return caster.position();
	}

	/** Entities in a box of +-r (vertical +-r*yScale) around the caster. */
	public List<Entity> around(double r, double yScale, boolean includeCaster, Predicate<Entity> filter) {
		AABB box = new AABB(pos(), pos()).inflate(r, r * yScale, r);
		return level.getEntities(includeCaster ? null : caster, box, filter);
	}

	/** Where the caster's look ray hits a block (or its end point) within range. */
	public Vec3 rayHit(double range) {
		Vec3 from = caster.getEyePosition();
		Vec3 to = from.add(caster.getLookAngle().scale(range));
		BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
	}
}
