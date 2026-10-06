package ro.avi.minegicka3.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import ro.avi.minegicka3.Element;

/** Minegicka's element damage table, applied to a single target. */
public final class SpellDamage {
	private SpellDamage() {
	}

	/** Side effects that apply on every contact: burning, wetness, chill, poison cure. */
	static void apply(Spell s, Entity e, double scale) {
		int fire = s.count(Element.FIRE), water = s.count(Element.WATER), cold = s.count(Element.COLD);
		int steam = s.count(Element.STEAM), life = s.count(Element.LIFE);
		boolean wet = water + steam > 0;
		if (fire > 0 && !wet) e.igniteForSeconds(fire * 3);
		if (wet || cold > 0) e.clearFire();
		if (e instanceof LivingEntity le) {
			if (cold > 0) le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * cold, cold - 1), s.caster);
			if (life > 0) le.removeEffect(MobEffects.POISON);
		}
	}

	static void damage(Spell s, Entity e, double scale) {
		int water = s.count(Element.WATER), life = s.count(Element.LIFE), cold = s.count(Element.COLD);
		int lightning = s.count(Element.LIGHTNING), arcane = s.count(Element.ARCANE), steam = s.count(Element.STEAM);
		int fire = s.count(Element.FIRE), ice = s.count(Element.ICE), earth = s.count(Element.EARTH);
		boolean wet = e.isInWaterOrRain() || water + steam > 0;

		double dWater = 0, dFire = 0.6 * fire, dArcane = arcane, dLightning = 0.8 * lightning, dEarth = earth;
		double dIce = 0.5 * ice, dCold = 0.4 * cold, dSteam = 0.3 * steam, heal = life;

		if (e instanceof Blaze || e instanceof MagmaCube || e instanceof ZombifiedPiglin) {
			dWater = water;
			dFire = 0;
			dArcane = 0.5 * arcane;
			dIce = 0.2 * ice;
			dCold = 1.2 * cold;
			dSteam = 0.5 * steam;
		}
		if (e instanceof SnowGolem) {
			heal += dCold;
			dCold = 0;
			dFire *= 2;
		}
		if (e instanceof Enderman) {
			dWater = 2 * water;
			dFire /= 2;
			dArcane /= 2;
			dEarth /= 2;
			dIce /= 2;
			dCold *= 2;
			dLightning /= 2;
			dSteam *= 2;
			heal /= 2;
		}
		if (wet) dLightning *= 2;
		if (e.fireImmune()) dFire = 0;
		// Undead: life hurts, arcane heals.
		if (e.is(EntityTypeTags.INVERTED_HEALING_AND_HARM)) {
			double t = heal;
			heal = dArcane;
			dArcane = t;
		}

		ServerLevel level = s.level();
		double total = (dWater + dFire + dArcane + dLightning + dEarth + dIce + dCold + dSteam) * s.staff.power() * scale;
		if (total > 0 && e != s.caster || total > 0 && s.cast == CastType.SELF) {
			DamageSource src = s.caster != null ? level.damageSources().indirectMagic(s.caster, s.caster) : level.damageSources().magic();
			e.hurtServer(level, src, (float)total);
			if (e instanceof Creeper c && dLightning >= 3.2 && level.getRandom().nextInt(4) == 0) {
				LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
				if (bolt != null) c.thunderHit(level, bolt);
			}
			if (e instanceof Pig && dLightning >= 3.8 && level.getRandom().nextInt(4) == 0) {
				LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
				if (bolt != null) {
					bolt.snapTo(e.position());
					bolt.setVisualOnly(true);
					level.addFreshEntity(bolt);
					e.thunderHit(level, bolt);
				}
			}
		}
		if (heal > 0 && e instanceof LivingEntity le) {
			le.heal((float)(heal * s.staff.power() * scale));
			level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 1, 0.2, 0.2, 0.2, 0);
		}
	}
}
