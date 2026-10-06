package ro.avi.minegicka3.spell;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import ro.avi.minegicka3.Element;

/**
 * Motion model of one spray droplet (Minegicka's EntitySpray), shared by the server (which applies its effects)
 * and the client (which only draws it). Spirals around the cast direction, drags at 0.92/tick, dies early when it
 * hits blocks.
 */
public class SprayParticle {
	public final Element element;
	public Vec3 pos, vel;
	public final Vec3 spiralAxis;
	public final double spiralStep;
	public final double gravity;
	public final int maxTicks;
	public int age;
	public boolean dead;
	public boolean touching;
	/** Block (or fluid) the droplet ran into this tick. */
	public net.minecraft.core.BlockPos hitBlock;

	public SprayParticle(Element element, Vec3 pos, Vec3 vel, Vec3 spiralAxis, int elementCount) {
		this.element = element;
		this.pos = pos;
		this.vel = vel;
		this.spiralAxis = spiralAxis.normalize();
		this.spiralStep = Math.PI / 20 / elementCount;
		this.maxTicks = (int)(30 * Math.pow(elementCount, 0.45));
		this.gravity = switch (element) {
			case FIRE -> -0.01 / elementCount / elementCount;
			case STEAM -> -0.03 / elementCount / elementCount;
			default -> 0;
		};
	}

	public void move(Level level) {
		vel = rotateAround(vel, spiralAxis, spiralStep);
		vel = vel.add(0, -gravity, 0);
		Vec3 next = pos.add(vel);
		BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, element == Element.FIRE ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY, CollisionContext.empty()));
		touching = hit.getType() != HitResult.Type.MISS;
		hitBlock = touching ? hit.getBlockPos() : null;
		if (touching) {
			Vec3 n = hit.getDirection().getUnitVec3();
			pos = hit.getLocation().add(n.scale(0.03));
			// slide along the surface
			vel = vel.subtract(n.scale(vel.dot(n)));
		} else {
			pos = next;
		}
		vel = vel.scale(0.92);
		if (touching) {
			age += 2;
			vel = new Vec3(vel.x * 0.7, vel.y, vel.z * 0.7);
		}
		if (++age >= maxTicks || Math.abs(vel.x) + Math.abs(vel.y) + Math.abs(vel.z) <= 0.05) dead = true;
	}

	/** Rodrigues rotation of v around unit axis k. */
	public static Vec3 rotateAround(Vec3 v, Vec3 k, double angle) {
		double c = Math.cos(angle), s = Math.sin(angle);
		return v.scale(c).add(k.cross(v).scale(s)).add(k.scale(k.dot(v) * (1 - c)));
	}
}
