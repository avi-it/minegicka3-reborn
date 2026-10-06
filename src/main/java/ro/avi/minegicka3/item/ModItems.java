package ro.avi.minegicka3.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ro.avi.minegicka3.Minegicka;
import ro.avi.minegicka3.spell.StaffStats;

public final class ModItems {
	public static final List<Item> ALL = new ArrayList<>();

	public static final Item STAFF = staff("staff", new StaffStats(1, 1, 1, 1));
	public static final Item STAFF_GRAND = staff("staff_grand", new StaffStats(2, 2, 0.5, 1.5));
	public static final Item STAFF_SUPER = staff("staff_super", new StaffStats(4, 4, 0.25, 2));

	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Minegicka.id("main"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.minegicka3"))
			.icon(() -> new ItemStack(STAFF))
			.displayItems((params, out) -> ALL.forEach(out::accept))
			.build());

	private ModItems() {
	}

	public static void init() {
	}

	static Item staff(String name, StaffStats stats) {
		return register(name, p -> new StaffItem(p, stats));
	}

	public static Item register(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Minegicka.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
		ALL.add(item);
		return item;
	}
}
