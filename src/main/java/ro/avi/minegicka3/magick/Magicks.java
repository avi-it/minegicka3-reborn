package ro.avi.minegicka3.magick;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.block.MagicBlockEntity;
import ro.avi.minegicka3.entity.MagicEntity;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Spell;
import ro.avi.minegicka3.spell.SpellManager;

/** The 16 magicks of Minegicka III, with their original combinations and costs. */
public final class Magicks {
	private Magicks() {
	}

	public static void init() {
	}

	private static int area(MagickContext c, int cap) {
		return (int)(16 * Math.min(c.power(), cap));
	}

	public static final Magick HASTE = Magick.register("Haste", "HAF", 100, c ->
		c.caster().addEffect(new MobEffectInstance(MobEffects.SPEED, (int)(100 * c.power()), (int)Math.ceil(c.speed() - 1))));

	public static final Magick NULLIFY = Magick.register("Nullify", "AD", 150, c -> {
		int r = area(c, 8);
		c.around(r, 1, false, e -> e instanceof MagicEntity).forEach(Entity::discard);
		BlockPos o = c.caster().blockPosition();
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
			if (c.level().getBlockEntity(p) instanceof MagicBlockEntity) c.level().removeBlock(p, false);
		}
	});

	public static final Magick TELEPORT = Magick.register("Teleport", "HAH", 400, c -> {
		LivingEntity e = c.caster();
		Vec3 from = e.position();
		Vec3 to = c.rayHit(6 * (c.power() + 1));
		to = to.subtract(e.getLookAngle().scale(0.68));
		c.level().sendParticles(ParticleTypes.SMOKE, from.x, from.y + 1, from.z, 40, 0.3, 0.6, 0.3, 0.02);
		e.teleportTo(to.x, to.y, to.z);
		e.resetFallDistance();
		c.level().sendParticles(ParticleTypes.CLOUD, to.x, to.y + 1, to.z, 40, 0.3, 0.6, 0.3, 0.02);
		c.level().playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
	});

	public static final Magick LIGHTNING_BOLT = Magick.register("Lightning Bolt", "SHAH", 400, c ->
		bolt(c.level(), c.rayHit(12 * c.power())));

	public static final Magick EXPLOSION = Magick.register("Explosion", "HFAF", 250, c -> {
		Vec3 p = c.rayHit(12 * c.power());
		c.level().explode(c.caster(), p.x, p.y, p.z, (float)(Math.sqrt(c.power()) * 2), Level.ExplosionInteraction.TNT);
	});

	public static final Magick VORTEX = Magick.register("Vortex", "IAIDI", 666, c -> {
		double p = Math.min(c.power(), 10), s = Math.min(c.speed(), 10);
		Vec3 at = c.caster().getEyePosition().add(c.caster().getLookAngle().scale(Math.max(7 * p, 5)));
		SpellManager.add(new VortexEffect(c.level(), at, (int)Math.max(40 * s, 80), Math.max(6 * p, 2),
			(int)Math.max(1, Math.min(30, 10 / s)), p, c.caster()));
	});

	public static final Magick HOMING_LIGHTNING = Magick.register("Homing Lightning", "SSHAH", 600, c ->
		SpellManager.add(new HomingLightningEffect(c, Math.min(8 * c.power(), 32), (int)Math.floor(6 + 2 * c.speed()))));

	public static final Magick EXTINGUISH = Magick.register("Extinguish", "WD", 100, c -> {
		int r = area(c, 10);
		c.around(r, 1, true, e -> true).forEach(Entity::clearFire);
		BlockPos o = c.caster().blockPosition();
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
			if (c.level().getBlockState(p).is(BlockTags.FIRE)) c.level().removeBlock(p, false);
		}
	});

	public static final Magick DEPOTION = Magick.register("De-potion", "LD", 100, c ->
		c.around(area(c, 10), 2, true, e -> e instanceof LivingEntity).forEach(e -> ((LivingEntity)e).removeAllEffects()));

	public static final Magick FEATHER_FALL = Magick.register("Feather Fall", "SD", 100, c ->
		c.around(area(c, 10), 2, true, e -> true).forEach(Entity::resetFallDistance));

	public static final Magick FREEZE_MOTION = Magick.register("Freeze Motion", "ID", 150, c ->
		c.around(area(c, 10), 2, false, e -> true).forEach(e -> {
			e.setDeltaMovement(Vec3.ZERO);
			e.needsSync = true;
			if (e instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3), c.caster());
		}));

	public static final Magick GRAVITATIONAL = Magick.register("Gravitational", "ED", 100, c ->
		c.around(area(c, 10), 2, false, e -> true).forEach(e -> e.push(0, -2 * c.speed(), 0)));

	public static final Magick SNOW = Magick.register("Snow", "CD", 100, c -> {
		int r = area(c, 10);
		ServerLevel l = c.level();
		BlockPos o = c.caster().blockPosition();
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
			if (l.getBlockState(p).isAir() && l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP)) {
				l.setBlockAndUpdate(p, Blocks.SNOW.defaultBlockState());
			}
		}
	});

	public static final Magick THAW = Magick.register("Thaw", "FD", 100, c -> {
		int r = area(c, 10);
		ServerLevel l = c.level();
		BlockPos o = c.caster().blockPosition();
		List<BlockPos> ice = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
			BlockState st = l.getBlockState(p);
			if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.POWDER_SNOW)) l.removeBlock(p, false);
			else if (st.is(Blocks.ICE) || st.is(Blocks.FROSTED_ICE)) ice.add(p.immutable());
		}
		for (BlockPos p : ice) {
			boolean wet = false;
			for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
				BlockState ns = l.getBlockState(n);
				if (!n.equals(p) && (ns.is(Blocks.ICE) || ns.is(Blocks.WATER))) wet = true;
			}
			l.setBlockAndUpdate(p, wet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
		}
	});

	public static final Magick WATER_SHOCK = Magick.register("Water Shock", "HD", 100, c -> {
		Spell s = new Spell(List.of(Element.LIGHTNING), CastType.SINGLE, c.caster(), c.staff());
		c.around(area(c, 10), 2, false, Entity::isInWaterOrRain).forEach(e -> {
			s.affect(e, 1);
			c.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 15, 0.4, 0.5, 0.4, 0.2);
		});
	});

	public static final Magick COLLECT = Magick.register("Collect", "SEES", 100, c -> {
		Vec3 p = c.pos();
		c.around(6 * c.power(), 1, false, e -> e instanceof ItemEntity || e instanceof ExperienceOrb).forEach(e -> e.teleportTo(p.x, p.y, p.z));
	});

	public static void bolt(ServerLevel level, Vec3 at) {
		LightningBolt b = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (b == null) return;
		b.snapTo(at);
		level.addFreshEntity(b);
	}

	/** Spends mana (players only, creative is free) and casts. Returns false if mana is short. */
	public static boolean cast(Magick m, MagickContext c) {
		double cost = m.baseCost() * c.staff().consume();
		if (c.caster() instanceof Player p && !p.getAbilities().instabuild) {
			if (ro.avi.minegicka3.spell.Mana.consume(p, cost, true, true) < 1) return false;
		}
		m.action().cast(c);
		return true;
	}
}
