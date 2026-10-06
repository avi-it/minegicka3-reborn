package ro.avi.minegicka3.client.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.client.MinegickaClient;

/**
 * Visual check in the real client (./gradlew runClientGametest): builds a test world, shows the items and HUD,
 * casts a few spells and saves screenshots. It drives the game internally, not the OS mouse/keyboard.
 */
public class LookClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> {
			var repo = mc.getResourcePackRepository();
			repo.reload();
			if (repo.addPack("file/Minegicka3-Original-Look.zip")) mc.options.updateResourcePacks(repo);
		});
		ctx.waitTicks(60);
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			TestServerContext s = sp.getServer();
			TestInput in = ctx.getInput();
			s.runCommand("gamemode creative @a");
			s.runCommand("time set noon");
			s.runCommand("weather clear");
			s.runCommand("execute as @p at @s run fill ~-12 ~-1 ~-12 ~12 ~-1 ~12 minecraft:grass_block");
			s.runCommand("execute as @p at @s run fill ~-12 ~ ~-12 ~12 ~12 ~12 minecraft:air");
			String[] bar = {"staff", "staff_grand", "staff_super", "staff_blessing", "staff_destruction", "staff_telekinesis",
				"staff_manipulation", "staff_hemmy", "hat"};
			for (int i = 0; i < bar.length; i++) s.runCommand("item replace entity @p hotbar." + i + " with minegicka3:" + bar[i]);
			in.lookAt(0, 10);
			ctx.waitTicks(40);
			ctx.takeScreenshot("01_staves_hotbar");

			String[] bar2 = {"thingy", "thingy_good", "thingy_great", "stick", "fire_essence", "water_essence", "arcane_essence",
				"hat_risk", "hat_immunity"};
			for (int i = 0; i < bar2.length; i++) s.runCommand("item replace entity @p hotbar." + i + " with minegicka3:" + bar2[i]);
			in.pressKey(o -> o.keyHotbarSlots[0]);
			ctx.waitTicks(10);
			ctx.takeScreenshot("02_gems_hotbar");

			s.runCommand("item replace entity @p hotbar.0 with minegicka3:staff_super");
			s.runCommand("execute as @p at @s run summon minecraft:pig ~ ~ ~6 {NoAI:1b}");
			s.runCommand("execute as @p at @s run summon minecraft:pig ~2 ~ ~8 {NoAI:1b}");
			in.lookAt(0, 8);
			ctx.waitTicks(10);
			queue(ctx, Element.FIRE, Element.FIRE, Element.ARCANE);
			ctx.takeScreenshot("03_held_staff_and_queue");

			in.holdKey(o -> o.keyUse);
			ctx.waitTicks(12);
			ctx.takeScreenshot("04_arcane_fire_beam");
			in.releaseKey(o -> o.keyUse);
			ctx.waitTicks(5);

			queue(ctx, Element.FIRE, Element.FIRE, Element.FIRE);
			in.holdKey(o -> o.keyUse);
			ctx.waitTicks(10);
			ctx.takeScreenshot("05_fire_spray");
			in.releaseKey(o -> o.keyUse);
			ctx.waitTicks(20);

			queue(ctx, Element.LIGHTNING, Element.LIGHTNING, Element.LIGHTNING);
			in.holdKey(o -> o.keyUse);
			ctx.waitTicks(8);
			ctx.takeScreenshot("06_lightning");
			in.releaseKey(o -> o.keyUse);
			ctx.waitTicks(5);

			queue(ctx, Element.EARTH, Element.EARTH, Element.EARTH);
			in.holdKey(o -> o.keyUse);
			ctx.waitTicks(40);
			in.releaseKey(o -> o.keyUse);
			ctx.waitTicks(3);
			ctx.takeScreenshot("07_boulder");
			ctx.waitTicks(30);

			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			queue(ctx, Element.SHIELD);
			in.holdShift();
			in.holdKey(o -> o.keyUse);
			ctx.waitTicks(4);
			in.releaseKey(o -> o.keyUse);
			in.releaseShift();
			ctx.waitTicks(10);
			ctx.takeScreenshot("08_shield_dome");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	private static void queue(ClientGameTestContext ctx, Element... els) {
		for (Element e : els) {
			ctx.getInput().pressKey(MinegickaClient.ELEMENT_KEYS.get(e));
			ctx.waitTick();
		}
		ctx.waitTicks(3);
	}
}
