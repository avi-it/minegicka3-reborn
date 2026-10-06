package ro.avi.minegicka3.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.Minegicka;
import ro.avi.minegicka3.spell.StaffStats;

public final class ModItems {
	public static final List<Item> ALL = new ArrayList<>();

	public static final Item STAFF = staff("staff", new StaffStats(1, 1, 1, 1, 4), null, Rarity.COMMON);
	public static final Item STAFF_GRAND = staff("staff_grand", new StaffStats(2, 2, 0.5, 1.5, 5), null, Rarity.UNCOMMON);
	public static final Item STAFF_SUPER = staff("staff_super", new StaffStats(4, 4, 0.25, 2, 6), null, Rarity.RARE);
	public static final Item STAFF_HEMMY = staff("staff_hemmy", new StaffStats(2.5, 20, 0.1, 10, 13), StaffAbilities.HEMMY, Rarity.EPIC);
	public static final Item STAFF_BLESSING = staff("staff_blessing", new StaffStats(0.75, 1.5, 1.25, 1.25, 5), StaffAbilities.BLESSING, Rarity.UNCOMMON);
	public static final Item STAFF_DESTRUCTION = staff("staff_destruction", new StaffStats(1.5, 0.75, 0.8, 0.8, 5), StaffAbilities.DESTRUCTION, Rarity.UNCOMMON);
	public static final Item STAFF_TELEKINESIS = staff("staff_telekinesis", new StaffStats(0.5, 2, 0.5, 2, 5), StaffAbilities.TELEKINESIS, Rarity.UNCOMMON);
	public static final Item STAFF_MANIPULATION = staff("staff_manipulation", new StaffStats(1, 1, 0.5, 2, 5), StaffAbilities.MANIPULATION, Rarity.UNCOMMON);

	public static final Item THINGY = register("thingy", Item::new);
	public static final Item THINGY_GOOD = register("thingy_good", p -> new Item(p.rarity(Rarity.UNCOMMON)));
	public static final Item THINGY_GREAT = register("thingy_great", p -> new Item(p.rarity(Rarity.RARE)));
	public static final Item STICK = register("stick", Item::new);
	public static final Item STICK_GOOD = register("stick_good", p -> new Item(p.rarity(Rarity.UNCOMMON)));
	public static final Item STICK_GREAT = register("stick_great", p -> new Item(p.rarity(Rarity.RARE)));

	public static final Item MAGIC_APPLE = register("magic_apple", p -> new SimpleItems.ManaFood(p.rarity(Rarity.UNCOMMON), 5, true));
	public static final Item MAGIC_GOLDEN_APPLE = register("magic_golden_apple", p -> new SimpleItems.ManaFood(glint(p).rarity(Rarity.RARE), 100, true));
	public static final Item MAGIC_GREAT_APPLE = register("magic_great_apple", p -> new SimpleItems.ManaFood(glint(p).rarity(Rarity.EPIC), 2000, true));
	public static final Item MAGIC_COOKIE = register("magic_cookie", p -> new SimpleItems.ManaFood(p, 20, false));
	public static final Item MAGIC_GOOD_COOKIE = register("magic_good_cookie", p -> new SimpleItems.ManaFood(p.rarity(Rarity.UNCOMMON), 120, false));
	public static final Item MAGIC_GREAT_COOKIE = register("magic_great_cookie", p -> new SimpleItems.ManaFood(p.rarity(Rarity.RARE), 960, false));

	public static final Map<Element, Item> ESSENCES = new EnumMap<>(Element.class);
	static {
		for (Element e : Element.values()) {
			ESSENCES.put(e, register(e.name().toLowerCase() + "_essence", p -> new SimpleItems.Essence(p, e)));
		}
	}
	public static final Item RESISTANCE_ESSENCE = register("resistance_essence", p -> new Item(p.rarity(Rarity.RARE)));

	public static final Item HAT = hat("hat", 1, 1, Rarity.COMMON);
	public static final Item HAT_RISK = hat("hat_risk", 2, 3, Rarity.UNCOMMON);
	public static final Item HAT_RESISTANCE = hat("hat_resistance", 0.5, 4, Rarity.RARE);
	public static final Item HAT_IMMUNITY = hat("hat_immunity", 0, 0, Rarity.EPIC);

	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Minegicka.id("main"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.minegicka3"))
			.icon(() -> new ItemStack(STAFF_SUPER))
			.displayItems((params, out) -> ALL.forEach(out::accept))
			.build());

	private ModItems() {
	}

	public static void init() {
	}

	private static Item.Properties glint(Item.Properties p) {
		return p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
	}

	static Item staff(String name, StaffStats stats, StaffItem.Ability ability, Rarity rarity) {
		return register(name, p -> new StaffItem(p.rarity(rarity), stats, ability));
	}

	static Item hat(String name, double all, double life, Rarity rarity) {
		return register(name, p -> new SimpleItems.Hat(p.rarity(rarity).equippable(EquipmentSlot.HEAD), all, life));
	}

	public static Item register(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Minegicka.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
		ALL.add(item);
		return item;
	}
}
