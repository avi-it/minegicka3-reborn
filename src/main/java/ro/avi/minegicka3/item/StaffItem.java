package ro.avi.minegicka3.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import ro.avi.minegicka3.spell.StaffStats;

/** A staff: casting focus whose stats scale every spell cast while it is held. */
public class StaffItem extends Item {
	public final StaffStats stats;

	public StaffItem(Properties props, StaffStats stats) {
		super(props.stacksTo(1));
		this.stats = stats;
	}

	/** Holding use channels the queued spell; the client tells the server when it starts and stops. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BLOCK;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Power: " + fmt(stats.power())).withStyle(ChatFormatting.GOLD));
		tooltip.accept(Component.literal("Attack speed: " + fmt(stats.atkSpeed())).withStyle(ChatFormatting.AQUA));
		tooltip.accept(Component.literal("Mana cost: " + fmt(stats.consume())).withStyle(ChatFormatting.BLUE));
		tooltip.accept(Component.literal("Mana recovery: " + fmt(stats.recover())).withStyle(ChatFormatting.GREEN));
	}

	private static String fmt(double d) {
		return d == Math.rint(d) ? "x" + (int)d : "x" + d;
	}
}
