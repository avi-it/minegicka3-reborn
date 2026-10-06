package ro.avi.minegicka3.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import ro.avi.minegicka3.spell.Mana;
import ro.avi.minegicka3.spell.StaffStats;

/** A staff: casting focus whose stats scale every spell cast while it is held. Some have an active and a passive. */
public class StaffItem extends Item {
	/** Special powers: active fires on right-click with an empty queue, passive ticks while held. */
	public interface Ability {
		double activeCost();

		void active(ServerLevel level, Player user);

		default void passive(ServerLevel level, Player user) {
		}

		String activeText();

		default String passiveText() {
			return null;
		}
	}

	public final StaffStats stats;
	public final @Nullable Ability ability;

	public StaffItem(Properties props, StaffStats stats, @Nullable Ability ability) {
		super(props.stacksTo(1));
		this.stats = stats;
		this.ability = ability;
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

	/** Server: runs the active ability if there is one and the mana is there. */
	public void triggerAbility(ServerLevel level, Player p) {
		if (ability == null) return;
		if (Mana.consume(p, ability.activeCost(), true, true) < 1) return;
		ability.active(level, p);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (ability != null && slot == EquipmentSlot.MAINHAND && owner instanceof Player p) ability.passive(level, p);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Power " + fmt(stats.power())).withStyle(ChatFormatting.RED));
		tooltip.accept(Component.literal("Attack rate " + fmt(stats.atkSpeed())).withStyle(ChatFormatting.GREEN));
		tooltip.accept(Component.literal("Mana consume " + fmt(stats.consume())).withStyle(ChatFormatting.BLUE));
		tooltip.accept(Component.literal("Mana recover " + fmt(stats.recover())).withStyle(ChatFormatting.LIGHT_PURPLE));
		tooltip.accept(Component.literal(stats.queue() + "-element combo").withStyle(ChatFormatting.GRAY));
		if (ability != null) {
			tooltip.accept(Component.literal("A: " + ability.activeText()).withStyle(ChatFormatting.GOLD));
			if (ability.passiveText() != null) tooltip.accept(Component.literal("P: " + ability.passiveText()).withStyle(ChatFormatting.GOLD));
		}
	}

	private static String fmt(double d) {
		return d == Math.rint(d) ? "x" + (int)d : "x" + d;
	}
}
