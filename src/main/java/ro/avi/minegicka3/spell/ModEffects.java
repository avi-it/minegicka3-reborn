package ro.avi.minegicka3.spell;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import ro.avi.minegicka3.Minegicka;

/** Minegicka's ward effects: each level cuts that element's damage by 15% (Life Effective boosts healing by 15%). */
public final class ModEffects {
	public static final Holder<MobEffect> COLD_RESISTANCE = register("cold_resistance", 0xEEEEFF);
	public static final Holder<MobEffect> LIFE_BOOST = register("life_boost", 0x00FF00);
	public static final Holder<MobEffect> ARCANE_RESISTANCE = register("arcane_resistance", 0xEE0000);
	public static final Holder<MobEffect> LIGHTNING_RESISTANCE = register("lightning_resistance", 0xFF22FF);

	private ModEffects() {
	}

	public static void init() {
	}

	private static Holder<MobEffect> register(String name, int color) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Minegicka.id(name), new Ward(color));
	}

	private static class Ward extends MobEffect {
		Ward(int color) {
			super(MobEffectCategory.BENEFICIAL, color);
		}
	}
}
