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

/**
 * Client-side visuals: replays spray droplets and draws beams, lightning arcs and nova rings with particles.
 * Beams get a white-hot core with twisting colour strands, lightning gets forks, sprays change with age.
 */
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
		Vec3 eye = mc.player != null ? mc.player.getEyePosition() : null;
		RandomSource rnd = level.getRandom();
		for (SprayParticle s : SPRAYS) {
			Vec3 a = s.pos;
			s.move(level);
			float life = s.age / (float)s.maxTicks;
			// two puffs per tick along the path so fast streams look continuous
			for (int i = 0; i < 2; i++) {
				Vec3 at = a.lerp(s.pos, i * 0.5);
				if (eye != null && eye.distanceToSqr(at) < 2.25) continue; // keep the caster's own view clear
				level.addParticle(sprayParticle(s.element, rnd, life), at.x, at.y, at.z, 0, 0, 0);
			}
			if (s.touching && rnd.nextInt(3) == 0 && (eye == null || eye.distanceToSqr(s.pos) >= 2.25)) splash(level, rnd, s.element, s.pos);
		}
		SPRAYS.removeIf(s -> s.dead);
	}

	/** Spray look per element; life runs 0 to 1 over the droplet's lifetime (fire turns to smoke, steam thins out). */
	private static ParticleOptions sprayParticle(Element e, RandomSource rnd, float life) {
		return switch (e) {
			case FIRE -> life > 0.75f && rnd.nextInt(3) == 0 ? ParticleTypes.SMOKE
				: rnd.nextInt(4) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME;
			case COLD -> rnd.nextInt(3) == 0 ? ParticleTypes.SNOWFLAKE
				: new DustParticleOptions(rnd.nextBoolean() ? 0xEAF6FF : 0xB8DCFF, 1.0f + rnd.nextFloat() * 0.5f);
			case WATER -> new DustParticleOptions(pick(rnd, 0x1E3FCF, 0x2B5BEA, 0x4F86F7), 1.1f + rnd.nextFloat() * 0.5f);
			case STEAM -> new DustParticleOptions(rnd.nextBoolean() ? 0xF2F2F2 : 0xD0D4D8, 1.8f + rnd.nextFloat() * (1 - life));
			default -> new DustParticleOptions(rnd.nextBoolean() ? 0x979797 : 0x787878, 1.6f);
		};
	}

	/** Small burst where a droplet scrapes along a block. */
	private static void splash(ClientLevel level, RandomSource rnd, Element e, Vec3 p) {
		switch (e) {
			case FIRE -> level.addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.03, 0);
			case WATER -> level.addParticle(ParticleTypes.SPLASH, p.x, p.y, p.z, 0, 0.1, 0);
			case STEAM -> level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0.04, 0);
			case COLD -> level.addParticle(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, (rnd.nextDouble() - 0.5) * 0.05, 0.02, (rnd.nextDouble() - 0.5) * 0.05);
			default -> {
			}
		}
	}

	/** Particles one LineFxPayload may spawn per tick, shared fairly between its segments. */
	private static final int BUDGET = 260;

	public static void onLines(LineFxPayload p) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || p.colors().isEmpty()) return;
		RandomSource rnd = level.getRandom();
		List<Vec3> pts = p.points();
		List<Integer> colors = p.colors();
		int segments = pts.size() / 2;
		if (segments == 0) return;
		int share = Math.max(5, BUDGET / segments);
		double ringLength = 0;
		for (int i = 0; i + 1 < pts.size(); i += 2) ringLength += pts.get(i).distanceTo(pts.get(i + 1));
		for (int i = 0; i + 1 < pts.size(); i += 2) {
			Vec3 a = pts.get(i), b = pts.get(i + 1);
			boolean last = i / 2 == segments - 1;
			if (p.kind() == LineFxPayload.LIGHTNING) {
				arc(level, rnd, a, b, colors, share);
			} else if (p.kind() == LineFxPayload.NOVA) {
				// one sample spacing for the whole ring, so every segment of a big nova gets drawn
				ring(level, rnd, a, b, colors, Math.max(0.3, ringLength / BUDGET));
			} else {
				// a beam from the local player's own staff starts a bit ahead so it doesn't cover the screen
				double start = segments == 1 && near(a) ? Math.min(1.5, a.distanceTo(b)) : 0;
				beam(level, rnd, a, b, colors, start, last ? share - 4 : share, last);
			}
		}
	}

	/**
	 * A beam is a bright, almost white core wrapped in two strands of the element colours that twist along it,
	 * with a flare where it hits (only on the last segment, flare = true). Spawns at most budget particles plus the flare's 4.
	 */
	private static void beam(ClientLevel level, RandomSource rnd, Vec3 a, Vec3 b, List<Integer> colors, double start, int budget,
		boolean flare) {
		double len = a.distanceTo(b);
		if (len >= 1e-3) {
			Vec3 axis = b.subtract(a).scale(1 / len);
			Vec3 u = axis.cross(Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
			Vec3 v = axis.cross(u);
			int core = mix(average(colors), 0xFFFFFF, 0.6f);
			double step = Math.max(0.3, len / Math.max(1, budget / 3));
			double phase = level.getGameTime() * 0.6;
			int n = 0;
			for (double d = start; d <= len && n + 3 <= budget; d += step) {
				Vec3 at = a.add(axis.scale(d));
				level.addParticle(new DustParticleOptions(core, 0.7f), at.x, at.y, at.z, 0, 0, 0);
				for (int strand = 0; strand < 2; strand++) {
					double t = d * 2.2 + phase + strand * Math.PI;
					Vec3 off = u.scale(Math.cos(t) * 0.2).add(v.scale(Math.sin(t) * 0.2));
					int col = colors.get((strand + (int)(d * 2)) % colors.size());
					level.addParticle(new DustParticleOptions(col, 1.0f), at.x + off.x, at.y + off.y, at.z + off.z, 0, 0, 0);
				}
				n += 3;
			}
		}
		if (!flare) return;
		level.addParticle(ParticleTypes.END_ROD, b.x, b.y, b.z, 0, 0.02, 0);
		for (int k = 0; k < 3; k++) {
			level.addParticle(new DustParticleOptions(colors.get(rnd.nextInt(colors.size())), 1.4f),
				b.x + (rnd.nextDouble() - 0.5) * 0.5, b.y + (rnd.nextDouble() - 0.5) * 0.5, b.z + (rnd.nextDouble() - 0.5) * 0.5, 0, 0, 0);
		}
	}

	/** One segment of a nova ring: chunky element dust every step blocks, with the odd rising spark. */
	private static void ring(ClientLevel level, RandomSource rnd, Vec3 a, Vec3 b, List<Integer> colors, double step) {
		double len = a.distanceTo(b);
		for (double d = 0; d <= len; d += step) {
			Vec3 at = len < 1e-3 ? a : a.lerp(b, d / len);
			int col = colors.get(rnd.nextInt(colors.size()));
			level.addParticle(new DustParticleOptions(col, 1.6f), at.x, at.y, at.z, 0, 0, 0);
			if (rnd.nextInt(6) == 0) level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0, 0.04, 0);
		}
	}

	private static boolean near(Vec3 a) {
		var pl = Minecraft.getInstance().player;
		return pl != null && pl.getEyePosition().distanceToSqr(a) < 1;
	}

	/**
	 * A jagged bolt from a to b (midpoint displacement) with a white-hot core and coloured glow, spending at most budget
	 * particles. With room to spare it also throws a side fork or two out of that same budget.
	 */
	private static int arc(ClientLevel level, RandomSource rnd, Vec3 a, Vec3 b, List<Integer> colors, int budget) {
		boolean forks = budget >= 100;
		int own = (forks ? budget * 6 / 10 : budget) - 1;
		// fewer kinks for a small budget: a bolt with 2^iterations legs costs at least 2 per leg
		int iterations = own >= 16 ? 3 : own >= 8 ? 2 : 1;
		List<Vec3> path = new ArrayList<>(List.of(a, b));
		double off = a.distanceTo(b) * 0.15;
		for (int it = 0; it < iterations; it++) {
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
		int legs = path.size() - 1;
		// 2 particles per sample; at least one sample per leg so the bolt never has gaps
		int samples = Math.max(1, Math.min(3, own / 2 / legs));
		int n = 0;
		int core = mix(average(colors), 0xFFFFFF, 0.75f);
		for (int i = 0; i < legs; i++) {
			Vec3 p = path.get(i), q = path.get(i + 1);
			for (int k = 0; k < samples; k++) {
				Vec3 at = p.lerp(q, k / (double)samples);
				level.addParticle(new DustParticleOptions(core, 0.45f), at.x, at.y, at.z, 0, 0, 0);
				int col = colors.get(rnd.nextInt(colors.size()));
				level.addParticle(rnd.nextInt(4) == 0 ? ParticleTypes.ELECTRIC_SPARK : new DustParticleOptions(col, 0.9f), at.x, at.y, at.z, 0, 0, 0);
				n += 2;
			}
		}
		if (forks) {
			int count = 1 + rnd.nextInt(2);
			int each = (budget - n - 1) / count;
			for (int f = 0; f < count && each >= 17; f++) {
				Vec3 from = path.get(1 + rnd.nextInt(path.size() - 2));
				Vec3 dir = b.subtract(a).scale(0.3);
				Vec3 to = from.add(dir).add((rnd.nextDouble() - 0.5) * dir.length(), (rnd.nextDouble() - 0.5) * dir.length(),
					(rnd.nextDouble() - 0.5) * dir.length());
				n += arc(level, rnd, from, to, colors, each);
			}
		}
		level.addParticle(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 0, 0.05, 0);
		return n + 1;
	}

	private static int pick(RandomSource rnd, int... cols) {
		return cols[rnd.nextInt(cols.length)];
	}

	private static int average(List<Integer> cols) {
		int r = 0, g = 0, b = 0;
		for (int c : cols) {
			r += c >> 16 & 255;
			g += c >> 8 & 255;
			b += c & 255;
		}
		int n = cols.size();
		return r / n << 16 | g / n << 8 | b / n;
	}

	/** Blends colour c towards colour to by t (0 to 1). */
	private static int mix(int c, int to, float t) {
		int r = (int)((c >> 16 & 255) + ((to >> 16 & 255) - (c >> 16 & 255)) * t);
		int g = (int)((c >> 8 & 255) + ((to >> 8 & 255) - (c >> 8 & 255)) * t);
		int b = (int)((c & 255) + ((to & 255) - (c & 255)) * t);
		return r << 16 | g << 8 | b;
	}
}
