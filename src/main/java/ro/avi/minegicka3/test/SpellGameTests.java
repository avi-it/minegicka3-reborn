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
}
