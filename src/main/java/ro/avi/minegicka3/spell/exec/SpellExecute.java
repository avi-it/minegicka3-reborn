package ro.avi.minegicka3.spell.exec;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.net.LineFxPayload;
import ro.avi.minegicka3.spell.Spell;

/** Behaviour of one spell shape (Minegicka's SpellExecute*). */
public abstract class SpellExecute {
	public static final SpellExecute SPRAY = new SprayExecute();
	public static final SpellExecute BEAM = new BeamExecute();
	public static final SpellExecute LIGHTNING = new LightningExecute();
	public static final SpellExecute PROJECTILE = new ProjectileExecute();
	public static final SpellExecute GROUNDED = new GroundedExecute();

	public static SpellExecute of(Spell s) {
		return switch (s.type) {
			case SPRAY -> SPRAY;
			case BEAM -> BEAM;
			case LIGHTNING -> LIGHTNING;
			case PROJECTILE -> PROJECTILE;
			case GROUNDED -> GROUNDED;
		};
	}

	public void start(Spell s) {
	}

	public void update(Spell s) {
	}

	public void stop(Spell s) {
	}

	/** Point a little in front of the caster's eyes, where spells leave the staff. */
	static Vec3 muzzle(Spell s, double forward, double down) {
		return s.caster.getEyePosition().add(s.caster.getLookAngle().scale(forward)).add(0, -down, 0);
	}

	static List<Integer> colors(Spell s) {
		List<Integer> c = new ArrayList<>();
		for (Element e : s.elements) if (!c.contains(e.color) && c.size() < 8) c.add(e.color); // LineFxPayload holds 8
		return c;
	}

	static void sendLines(Spell s, int kind, List<Vec3> points) {
		if (points.isEmpty()) return;
		LineFxPayload fx = new LineFxPayload(kind, points, colors(s));
		for (ServerPlayer p : PlayerLookup.around(s.level(), s.caster.position(), 96)) ServerPlayNetworking.send(p, fx);
	}
}
