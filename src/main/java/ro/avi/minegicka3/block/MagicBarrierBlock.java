package ro.avi.minegicka3.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Unbreakable timed barrier; hitting it chips 2.5 × (2 + weapon damage) off its life. */
public abstract class MagicBarrierBlock extends BaseEntityBlock {
	protected MagicBarrierBlock(Properties props) {
		super(props);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MagicBlockEntity be) {
			be.damage(2.5 * (2 + weaponDamage(player.getMainHandItem())));
			if (be.life <= 0) level.removeBlock(pos, false);
		}
	}

	private static double weaponDamage(ItemStack stack) {
		ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		double[] d = {0};
		mods.modifiers().forEach(m -> {
			if (m.attribute().is(Attributes.ATTACK_DAMAGE)) d[0] += m.modifier().amount();
		});
		return d[0];
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : (l, p, s, be) -> MagicBlockEntity.serverTick(l, p, s, (MagicBlockEntity)be);
	}
}
