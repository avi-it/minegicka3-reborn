package ro.avi.minegicka3.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.spell.Mana;

/** Small items: mana food, wizard hats, element essences. */
public final class SimpleItems {
	private SimpleItems() {
	}

	/** Magic apples raise max mana; magic cookies refill mana. Eaten instantly on right click. */
	public static class ManaFood extends Item {
		private final double amount;
		private final boolean raisesMax;

		public ManaFood(Properties p, double amount, boolean raisesMax) {
			super(p);
			this.amount = amount;
			this.raisesMax = raisesMax;
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (level instanceof ServerLevel sl) {
				if (raisesMax) Mana.raiseMax(player, amount);
				else Mana.set(player, Mana.get(player) + amount);
				player.getFoodData().eat(4, 0.3f);
				sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 1.2f);
				player.getItemInHand(hand).consume(1, player);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
			tooltip.accept(Component.literal((raisesMax ? "+" + (int)amount + " max mana" : "+" + (int)amount + " mana")).withStyle(ChatFormatting.BLUE));
		}
	}

	/**
	 * Wizard hats: worn on the head, they scale the element damage the wearer takes
	 * (all elements by `all`, healing by `life`).
	 */
	public static class Hat extends Item {
		public final double all, life;

		public Hat(Properties p, double all, double life) {
			super(p.stacksTo(1));
			this.all = all;
			this.life = life;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
			if (all != 1) tooltip.accept(Component.literal("Magic taken x" + all).withStyle(all < 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
			if (life != 1) tooltip.accept(Component.literal("Healing taken x" + life).withStyle(ChatFormatting.GREEN));
		}
	}

	/** Crafting material carrying an element's essence. */
	public static class Essence extends Item {
		public final Element element;

		public Essence(Properties p, Element element) {
			super(p);
			this.element = element;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
			tooltip.accept(Component.literal(element.shortName()).withStyle(element.chat));
		}
	}
}
