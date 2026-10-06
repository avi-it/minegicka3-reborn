package ro.avi.minegicka3.test;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Mana;
import ro.avi.minegicka3.spell.SpellManager;

/** Headless checks of the spell system: run with ./gradlew runGametest. */
public class SpellGameTests {
	private static void check(GameTestHelper h, boolean ok, String msg) {
		if (!ok) throw h.assertionException(Component.literal(msg));
	}

	/** A frozen caster at local (1,2,1) looking along +X (yaw -90), with a frozen target `dist` blocks ahead. */
	private static Mob caster(GameTestHelper h) {
		Mob c = h.spawn(EntityTypes.HUSK, new Vec3(1.5, 2, 4.5));
		freeze(c);
		c.setYRot(-90);
		c.setYHeadRot(-90);
		c.setXRot(0);
		return c;
	}

	private static <T extends Mob> T freeze(T m) {
		m.setNoAi(true);
		m.setNoGravity(true);
		m.setSilent(true);
		m.setPersistenceRequired();
		return m;
	}

	private static Mob target(GameTestHelper h, double x) {
		Mob t = h.spawn(EntityTypes.PIG, new Vec3(x, 2, 4.5));
		return freeze(t);
	}

	/** Pitch the caster down so its look ray passes through the target's centre. */
	private static void aimAt(Mob c, Mob t) {
		Vec3 d = t.getBoundingBox().getCenter().subtract(c.getEyePosition());
		c.setXRot((float)Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
	}

	@GameTest
	public void queueRules(GameTestHelper h) {
		List<Element> q = new ArrayList<>();
		Element.pushToQueue(q, Element.WATER, 5, (a, b) -> {});
		Element.pushToQueue(q, Element.COLD, 5, (a, b) -> {});
		check(h, q.equals(List.of(Element.ICE)), "water+cold should be ice: " + q);
		Element.pushToQueue(q, Element.FIRE, 5, (a, b) -> {});
		check(h, q.equals(List.of(Element.WATER)), "fire melts ice into water: " + q);
		Element.pushToQueue(q, Element.LIGHTNING, 5, (a, b) -> {});
		check(h, q.isEmpty(), "lightning cancels water: " + q);
		for (int i = 0; i < 7; i++) Element.pushToQueue(q, Element.ARCANE, 5, (a, b) -> {});
		check(h, q.size() == 5, "queue caps at 5: " + q);
		Element.pushToQueue(q, Element.LIFE, 5, (a, b) -> {});
		check(h, q.size() == 4, "life cancels one arcane: " + q);
		h.succeed();
	}

	@GameTest(maxTicks = 100)
	public void fireSprayBurns(GameTestHelper h) {
		Mob c = caster(h);
		Mob t = target(h, 4.5);
		float hp = t.getHealth();
		SpellManager.start(c, List.of(Element.FIRE, Element.FIRE, Element.FIRE), CastType.SINGLE);
		h.succeedWhen(() -> {
			check(h, t.getHealth() < hp, "target not damaged by fire spray");
			check(h, t.isOnFire(), "target not burning");
			SpellManager.stop(c);
		});
	}

	@GameTest(maxTicks = 60)
	public void arcaneBeamHits(GameTestHelper h) {
		Mob c = caster(h);
		Mob t = target(h, 6.5);
		aimAt(c, t);
		float hp = t.getHealth();
		SpellManager.start(c, List.of(Element.ARCANE, Element.ARCANE), CastType.SINGLE);
		h.succeedWhen(() -> {
			check(h, t.getHealth() < hp, "beam did not damage target");
			SpellManager.stop(c);
		});
	}

	@GameTest(maxTicks = 60)
	public void lifeBeamHeals(GameTestHelper h) {
		Mob c = caster(h);
		Mob t = target(h, 6.5);
		t.setHealth(2);
		aimAt(c, t);
		SpellManager.start(c, List.of(Element.LIFE, Element.LIFE), CastType.SINGLE);
		h.succeedWhen(() -> {
			check(h, t.getHealth() > 2, "life beam did not heal");
			SpellManager.stop(c);
		});
	}

	@GameTest(maxTicks = 60)
	public void lightningChains(GameTestHelper h) {
		Mob c = caster(h);
		Mob t1 = target(h, 4.5);
		Mob t2 = h.spawn(EntityTypes.PIG, new Vec3(6.5, 2, 4.5));
		freeze(t2);
		float hp1 = t1.getHealth(), hp2 = t2.getHealth();
		SpellManager.start(c, List.of(Element.LIGHTNING, Element.LIGHTNING), CastType.SINGLE);
		h.succeedWhen(() -> {
			check(h, t1.getHealth() < hp1, "first target not shocked");
			check(h, t2.getHealth() < hp2, "lightning did not chain to second target");
			SpellManager.stop(c);
		});
	}

	@GameTest(maxTicks = 100)
	public void coldFreezesWater(GameTestHelper h) {
		BlockPos water = new BlockPos(4, 1, 4);
		for (int x = 2; x <= 6; x++) h.setBlock(new BlockPos(x, 1, 4), Blocks.WATER);
		Mob c = caster(h);
		c.setXRot(35); // look down at the water
		SpellManager.start(c, List.of(Element.COLD, Element.COLD), CastType.SINGLE);
		h.succeedWhen(() -> {
			boolean ice = false;
			for (int x = 2; x <= 6; x++) ice |= h.getBlockState(new BlockPos(x, 1, 4)).is(Blocks.ICE);
			check(h, ice, "cold spray did not freeze water");
			SpellManager.stop(c);
		});
	}

	@GameTest(maxTicks = 40)
	public void sprayCostsMana(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.SURVIVAL);
		p.snapTo(h.absoluteVec(new Vec3(1.5, 2, 4.5)), -90, 0);
		Mana.set(p, Mana.MAX);
		SpellManager.start(p, List.of(Element.WATER, Element.WATER), CastType.SINGLE);
		h.runAfterDelay(20, () -> {
			double m = Mana.get(p);
			SpellManager.stop(p);
			check(h, m < Mana.MAX - 20, "mana not consumed: " + m);
			h.succeed();
		});
	}

	private static int countBlocks(GameTestHelper h, net.minecraft.world.level.block.Block b, int r) {
		int n = 0;
		BlockPos c = h.absolutePos(new BlockPos(1, 2, 4));
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
			if (h.getLevel().getBlockState(p).is(b)) n++;
		}
		return n;
	}

	@GameTest(maxTicks = 20)
	public void shieldDome(GameTestHelper h) {
		Mob c = caster(h);
		SpellManager.start(c, List.of(Element.SHIELD), CastType.AREA);
		h.runAfterDelay(1, () -> {
			int n = countBlocks(h, ro.avi.minegicka3.block.ModBlocks.SHIELD, 7);
			check(h, n > 50, "dome too small: " + n);
			h.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void earthWall(GameTestHelper h) {
		Mob c = caster(h);
		SpellManager.start(c, List.of(Element.SHIELD, Element.EARTH), CastType.SINGLE);
		h.runAfterDelay(1, () -> {
			int n = countBlocks(h, ro.avi.minegicka3.block.ModBlocks.WALL, 8);
			check(h, n > 5, "wall too small: " + n);
			h.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void fireWard(GameTestHelper h) {
		Mob c = caster(h);
		SpellManager.start(c, List.of(Element.SHIELD, Element.FIRE), CastType.SELF);
		check(h, c.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), "no fire resistance");
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void minesAndStorms(GameTestHelper h) {
		Mob c = caster(h);
		SpellManager.start(c, List.of(Element.SHIELD, Element.ARCANE), CastType.SINGLE);
		SpellManager.start(c, List.of(Element.SHIELD, Element.FIRE), CastType.SINGLE);
		h.runAfterDelay(1, () -> {
			net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(16);
			int mines = h.getLevel().getEntities(ro.avi.minegicka3.entity.ModEntities.MINE, box, e -> true).size();
			int storms = h.getLevel().getEntities(ro.avi.minegicka3.entity.ModEntities.STORM, box, e -> true).size();
			check(h, mines > 0, "no mines");
			check(h, storms > 0, "no storms");
			h.succeed();
		});
	}

	@GameTest(maxTicks = 120)
	public void boulderHits(GameTestHelper h) {
		Mob c = caster(h);
		Mob t = target(h, 5.5);
		aimAt(c, t);
		float hp = t.getHealth();
		SpellManager.start(c, List.of(Element.EARTH, Element.EARTH, Element.EARTH), CastType.SINGLE);
		h.runAfterDelay(40, () -> SpellManager.stop(c));
		h.succeedWhen(() -> check(h, t.getHealth() < hp, "boulder did not hurt target"));
	}

	@GameTest(maxTicks = 120)
	public void iciclesHit(GameTestHelper h) {
		Mob c = caster(h);
		Mob t = target(h, 5.5);
		aimAt(c, t);
		float hp = t.getHealth();
		SpellManager.start(c, List.of(Element.ICE, Element.ICE), CastType.SINGLE);
		h.runAfterDelay(60, () -> SpellManager.stop(c));
		h.succeedWhen(() -> check(h, t.getHealth() < hp, "icicles did not hurt target"));
	}

	@GameTest(maxTicks = 20)
	public void magickComboMatch(GameTestHelper h) {
		var m = ro.avi.minegicka3.magick.Magick.match(List.of(Element.ICE, Element.ARCANE, Element.ICE, Element.SHIELD, Element.ICE));
		check(h, m == ro.avi.minegicka3.magick.Magicks.VORTEX, "IAIDI should be Vortex: " + m);
		check(h, ro.avi.minegicka3.magick.Magick.ALL.size() == 16, "expected 16 magicks");
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void hasteAndFreeze(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.CREATIVE);
		p.snapTo(h.absoluteVec(new Vec3(1.5, 2, 4.5)), -90, 0);
		Mob t = target(h, 4.5);
		t.setDeltaMovement(0, 1, 0);
		var ctx = new ro.avi.minegicka3.magick.MagickContext(h.getLevel(), p, ro.avi.minegicka3.spell.StaffStats.DEFAULT);
		ro.avi.minegicka3.magick.Magicks.cast(ro.avi.minegicka3.magick.Magicks.HASTE, ctx);
		ro.avi.minegicka3.magick.Magicks.cast(ro.avi.minegicka3.magick.Magicks.FREEZE_MOTION, ctx);
		check(h, p.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "haste gave no speed");
		check(h, t.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "freeze motion did not slow");
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void recipesLoaded(GameTestHelper h) {
		var rm = h.getLevel().getServer().getRecipeManager();
		for (String r : List.of("staff", "staff_super", "thingy", "fire_essence", "hat")) {
			var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, ro.avi.minegicka3.Minegicka.id(r));
			check(h, rm.byKey(key).isPresent(), "missing recipe " + r);
		}
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void manaAppleRaisesMax(GameTestHelper h) {
		Player p = h.makeMockPlayer(GameType.SURVIVAL);
		double before = Mana.max(p);
		Mana.raiseMax(p, 100);
		check(h, Mana.max(p) == before + 100, "max mana not raised");
		h.succeed();
	}

	/** Regression: a nova detonating a mine spawns a new nova mid-tick (crashed with ConcurrentModificationException). */
	@GameTest(maxTicks = 80)
	public void mineChainReaction(GameTestHelper h) {
		for (int x = -8; x <= 10; x++) for (int z = -5; z <= 13; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		Mob c = caster(h);
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(24);
		java.util.function.IntSupplier mines = () -> h.getLevel().getEntities(ro.avi.minegicka3.entity.ModEntities.MINE, box, e -> true).size();
		SpellManager.start(c, List.of(Element.SHIELD, Element.ARCANE), CastType.AREA);
		int[] before = {0};
		h.runAfterDelay(20, () -> {
			before[0] = mines.getAsInt();
			SpellManager.start(c, List.of(Element.ARCANE), CastType.AREA);
		});
		h.runAfterDelay(60, () -> {
			int after = mines.getAsInt();
			check(h, before[0] > 0 && after < before[0] - 1, "chain reaction did not spread: " + before[0] + " -> " + after);
			h.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void arcaneNova(GameTestHelper h) {
		Mob c = caster(h);
		Mob t = target(h, 5.5);
		float hp = t.getHealth();
		SpellManager.start(c, List.of(Element.ARCANE), CastType.AREA);
		h.succeedWhen(() -> check(h, t.getHealth() < hp, "nova did not hurt target"));
	}
}
