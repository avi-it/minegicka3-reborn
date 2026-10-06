package ro.avi.minegicka3.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.net.LineFxPayload;
import ro.avi.minegicka3.net.SprayFxPayload;
import ro.avi.minegicka3.spell.SprayParticle;

/** Client-side visuals: replays spray droplets and draws beams / lightning arcs with particles. */
public final class ClientFx {
	private static final List<SprayParticle> SPRAYS = new ArrayList<>();

	private ClientFx() {
	}

	public static void onSpray(SprayFxPayload p) {
		Element e = Element.byId(p.element());
		if (e != null && SPRAYS.size() < 4000) SPRAYS.add(new SprayParticle(e, p.pos(), p.vel(), p.axis(), p.count()));
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null) {
			SPRAYS.clear();
			return;
		}
		for (SprayParticle s : SPRAYS) {
			Vec3 a = s.pos;
			s.move(level);
			// two puffs per tick along the path so fast streams look continuous
			for (int i = 0; i < 2; i++) {
				Vec3 at = a.lerp(s.pos, i * 0.5);
				level.addParticle(sprayParticle(s.element, level.getRandom()), at.x, at.y, at.z, 0, 0, 0);
			}
		}
		SPRAYS.removeIf(s -> s.dead);
	}

	private static ParticleOptions sprayParticle(Element e, RandomSource rnd) {
		return switch (e) {
			case FIRE -> rnd.nextInt(4) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME;
			case COLD -> rnd.nextBoolean() ? ParticleTypes.SNOWFLAKE : new DustParticleOptions(0xFFFFFF, 1.2f);
			case WATER -> new DustParticleOptions(rnd.nextBoolean() ? 0x1432F7 : 0x1D45F0, 1.3f);
			default -> new DustParticleOptions(rnd.nextBoolean() ? 0x979797 : 0x787878, 1.6f);
		};
	}

	public static void onLines(LineFxPayload p) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || p.colors().isEmpty()) return;
		RandomSource rnd = level.getRandom();
		List<Vec3> pts = p.points();
		int budget = 220;
		for (int i = 0; i + 1 < pts.size(); i += 2) {
			Vec3 a = pts.get(i), b = pts.get(i + 1);
			if (p.kind() == LineFxPayload.LIGHTNING) {
				budget -= arc(level, rnd, a, b, p.colors());
			} else {
				double len = a.distanceTo(b);
				double step = Math.max(0.35, len / budget);
				for (double d = 0; d <= len && budget > 0; d += step, budget--) {
					Vec3 at = a.lerp(b, d / len);
					int col = p.colors().get(rnd.nextInt(p.colors().size()));
					level.addParticle(new DustParticleOptions(col, 1.1f), at.x, at.y, at.z, 0, 0, 0);
				}
				if (pts.size() == 2) level.addParticle(ParticleTypes.END_ROD, b.x, b.y, b.z, 0, 0.02, 0);
			}
		}
	}

	/** A jagged bolt from a to b: midpoint displacement, drawn with sparks and purple dust. */
	private static int arc(ClientLevel level, RandomSource rnd, Vec3 a, Vec3 b, List<Integer> colors) {
		List<Vec3> path = new ArrayList<>(List.of(a, b));
		double off = a.distanceTo(b) * 0.15;
		for (int it = 0; it < 3; it++) {
			List<Vec3> next = new ArrayList<>();
			for (int i = 0; i + 1 < path.size(); i++) {
				Vec3 m = path.get(i).lerp(path.get(i + 1), 0.5)
					.add((rnd.nextDouble() - 0.5) * off, (rnd.nextDouble() - 0.5) * off, (rnd.nextDouble() - 0.5) * off);
				next.add(path.get(i));
				next.add(m);
			}
			next.add(path.getLast());
			path = next;
			off *= 0.5;
		}
		int n = 0;
		for (int i = 0; i + 1 < path.size(); i++) {
			Vec3 p = path.get(i), q = path.get(i + 1);
			for (double t = 0; t < 1; t += 0.34) {
				Vec3 at = p.lerp(q, t);
				int col = colors.get(rnd.nextInt(colors.size()));
				level.addParticle(rnd.nextInt(3) == 0 ? ParticleTypes.ELECTRIC_SPARK : new DustParticleOptions(col, 0.7f), at.x, at.y, at.z, 0, 0, 0);
				n++;
			}
		}
		return n;
	}
}
