package ro.avi.minegicka3.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.magick.MagickContext;
import ro.avi.minegicka3.magick.Magicks;
import ro.avi.minegicka3.spell.StaffStats;

/** The special staves' active and passive powers (Minegicka III section "Staff"). */
public final class StaffAbilities {
	private StaffAbilities() {
	}

	private static java.util.List<Entity> around(Player p, double r, java.util.function.Predicate<Entity> f) {
		return p.level().getEntities(p, new AABB(p.position(), p.position()).inflate(r), f);
	}

	private static boolean hostile(Entity e) {
		return e instanceof Enemy;
	}

	public static final StaffItem.Ability HEMMY = new StaffItem.Ability() {
		public double activeCost() {
			return 0;
		}

		public void active(ServerLevel level, Player p) {
			Magicks.bolt(level, new MagickContext(level, p, StaffStats.DEFAULT).rayHit(64));
		}

		public String activeText() {
			return "call lightning where you look";
		}
	};

	public static final StaffItem.Ability BLESSING = new StaffItem.Ability() {
		public double activeCost() {
			return 200;
		}

		public void active(ServerLevel level, Player p) {
			boolean mine = hostile(p);
			for (Entity e : around(p, 12, x -> x instanceof LivingEntity)) {
				LivingEntity le = (LivingEntity)e;
				if (hostile(e) == mine) {
					for (var eff : java.util.List.of(MobEffects.STRENGTH, MobEffects.HASTE, MobEffects.FIRE_RESISTANCE, MobEffects.JUMP_BOOST,
						MobEffects.SPEED, MobEffects.NIGHT_VISION, MobEffects.RESISTANCE, MobEffects.WATER_BREATHING)) {
						le.addEffect(new MobEffectInstance(eff, 200, 0), p);
					}
					le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0), p);
				} else {
					for (var eff : java.util.List.of(MobEffects.BLINDNESS, MobEffects.NAUSEA, MobEffects.MINING_FATIGUE, MobEffects.HUNGER,
						MobEffects.SLOWNESS, MobEffects.WEAKNESS)) {
						le.addEffect(new MobEffectInstance(eff, 150, 0), p);
					}
					le.addEffect(new MobEffectInstance(MobEffects.POISON, 75, 0), p);
				}
			}
			p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
		}

		public void passive(ServerLevel level, Player p) {
			if (p.tickCount % 20 != 0) return;
			var list = around(p, 8, x -> x instanceof LivingEntity le && !hostile(x) && le.getHealth() < le.getMaxHealth());
			list.add(p);
			LivingEntity e = (LivingEntity)list.get((p.tickCount / 5) % list.size());
			if (e.getHealth() < e.getMaxHealth()) {
				e.heal(1);
				level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 1, 0.2, 0.2, 0.2, 0);
			}
		}

		public String activeText() {
			return "bless allies, curse enemies (200 mana)";
		}

		public String passiveText() {
			return "slowly heals friendly creatures nearby";
		}
	};

	public static final StaffItem.Ability DESTRUCTION = new StaffItem.Ability() {
		public double activeCost() {
			return 200;
		}

		public void active(ServerLevel level, Player p) {
			Vec3 at = new MagickContext(level, p, StaffStats.DEFAULT).rayHit(16);
			level.explode(p, at.x, at.y, at.z, 1 + level.getRandom().nextFloat() * 2, Level.ExplosionInteraction.TNT);
		}

		public void passive(ServerLevel level, Player p) {
			if (p.tickCount % 40 != 0) return;
			BlockPos o = p.blockPosition();
			for (BlockPos f : BlockPos.betweenClosed(o.offset(-12, -12, -12), o.offset(12, 12, 12))) {
				if (Math.abs(f.getX() - o.getX()) <= 3 && Math.abs(f.getY() - o.getY()) <= 3 && Math.abs(f.getZ() - o.getZ()) <= 3) continue;
				if (!level.getBlockState(f).is(net.minecraft.tags.BlockTags.FIRE) || level.getRandom().nextInt(3) != 0) continue;
				for (Direction d : Direction.values()) {
					BlockPos n = f.relative(d);
					if (level.getRandom().nextInt(3) == 0 && level.getBlockState(n).canBeReplaced() && BaseFireBlock.canBePlacedAt(level, n, d)) {
						level.setBlockAndUpdate(n, BaseFireBlock.getState(level, n));
					}
				}
			}
		}

		public String activeText() {
			return "explosion where you look (200 mana)";
		}

		public String passiveText() {
			return "nearby fires spread faster";
		}
	};

	public static final StaffItem.Ability TELEKINESIS = new StaffItem.Ability() {
		public double activeCost() {
			return 250;
		}

		public void active(ServerLevel level, Player p) {
			boolean mine = hostile(p);
			for (Entity e : around(p, 16, x -> true)) {
				Vec3 d = p.position().subtract(e.position());
				Vec3 v;
				if (e instanceof Projectile || e instanceof LivingEntity && hostile(e) != mine) {
					v = d.normalize().scale(-3);
				} else {
					v = d.normalize().scale(0.4 * d.length() * 0.25);
				}
				e.push(v.x, v.y / 2, v.z);
			}
		}

		public void passive(ServerLevel level, Player p) {
			if (p.tickCount % 4 != 0) return;
			for (Entity e : around(p, 8, x -> x instanceof Projectile pr && pr.getOwner() != p)) {
				Vec3 v = e.position().subtract(p.position()).normalize().scale(0.35);
				e.push(v.x, v.y, v.z);
			}
		}

		public String activeText() {
			return "pull friends and items in, push foes and projectiles away (250 mana)";
		}

		public String passiveText() {
			return "deflects incoming projectiles";
		}
	};

	public static final StaffItem.Ability MANIPULATION = new StaffItem.Ability() {
		public double activeCost() {
			return 250;
		}

		public void active(ServerLevel level, Player p) {
			p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 1));
		}

		public void passive(ServerLevel level, Player p) {
			if (p.tickCount % 100 != 0) return;
			var mobs = around(p, 12, x -> x instanceof Mob && x instanceof Enemy);
			for (Entity e : mobs) {
				Mob m = (Mob)e;
				Entity t = mobs.get(level.getRandom().nextInt(mobs.size()));
				if (t != m) m.setTarget((LivingEntity)t);
			}
		}

		public String activeText() {
			return "turn invisible (250 mana)";
		}

		public String passiveText() {
			return "hostile mobs nearby turn on each other";
		}
	};
}
