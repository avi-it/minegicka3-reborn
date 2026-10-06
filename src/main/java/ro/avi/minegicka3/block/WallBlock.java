package ro.avi.minegicka3.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** One block of an Earth/Ice wall ring. Icy (or otherwise imbued) walls hurt what touches them. */
public class WallBlock extends MagicBarrierBlock {
	public static final BooleanProperty ICY = BooleanProperty.create("icy");
	/** Slightly inset collision so entities pressing against the wall count as "inside". */
	private static final VoxelShape COLLISION = Block.box(0.5, 0, 0.5, 15.5, 16, 15.5);

	public WallBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(ICY, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(ICY);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WallBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return COLLISION;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise) {
		if (!level.isClientSide() && entity instanceof LivingEntity && level.getBlockEntity(pos) instanceof WallBlockEntity be) be.touch(entity);
	}
}
