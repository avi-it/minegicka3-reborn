package ro.avi.minegicka3.block;

import java.util.function.Function;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import ro.avi.minegicka3.Minegicka;

public final class ModBlocks {
	public static final Block SHIELD = register("shield", ShieldBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.COLOR_YELLOW).strength(-1, 5).noLootTable().noOcclusion().lightLevel(s -> 5)
		.sound(SoundType.AMETHYST).pushReaction(PushReaction.IMMOVEABLE).isValidSpawn((s, l, p, t) -> false)
		.isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false));
	public static final Block WALL = register("wall", WallBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.DIRT).strength(-1, 5).noLootTable().noOcclusion().lightLevel(s -> 3)
		.sound(SoundType.STONE).pushReaction(PushReaction.IMMOVEABLE).isValidSpawn((s, l, p, t) -> false));

	public static final BlockEntityType<ShieldBlockEntity> SHIELD_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Minegicka.id("shield"), FabricBlockEntityTypeBuilder.create(ShieldBlockEntity::new, SHIELD).build());
	public static final BlockEntityType<WallBlockEntity> WALL_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Minegicka.id("wall"), FabricBlockEntityTypeBuilder.create(WallBlockEntity::new, WALL).build());

	private ModBlocks() {
	}

	public static void init() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties props) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Minegicka.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, f.apply(props.setId(key)));
	}
}
