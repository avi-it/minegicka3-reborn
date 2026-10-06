package ro.avi.minegicka3.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One cell of a shield dome: blocks movement, projectiles and nova line of sight until its life runs out. */
public class ShieldBlock extends MagicBarrierBlock {

	public ShieldBlock(Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShieldBlockEntity(pos, state);
	}
}
