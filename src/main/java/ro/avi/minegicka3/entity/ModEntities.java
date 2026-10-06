package ro.avi.minegicka3.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import ro.avi.minegicka3.Minegicka;

public final class ModEntities {
	public static final EntityType<BoulderEntity> BOULDER = register("boulder",
		EntityType.Builder.<BoulderEntity>of(BoulderEntity::new, MobCategory.MISC).sized(0.5f, 0.5f));
	public static final EntityType<IcicleEntity> ICICLE = register("icicle",
		EntityType.Builder.<IcicleEntity>of(IcicleEntity::new, MobCategory.MISC).sized(0.2f, 0.2f));
	public static final EntityType<MineEntity> MINE = register("mine",
		EntityType.Builder.<MineEntity>of(MineEntity::new, MobCategory.MISC).sized(0.5f, 0.5f));
	public static final EntityType<StormEntity> STORM = register("storm",
		EntityType.Builder.<StormEntity>of(StormEntity::new, MobCategory.MISC).sized(0.1f, 0.1f));

	private ModEntities() {
	}

	public static void init() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> b) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Minegicka.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.noSave().noSummon().clientTrackingRange(8).updateInterval(1).build(key));
	}
}
