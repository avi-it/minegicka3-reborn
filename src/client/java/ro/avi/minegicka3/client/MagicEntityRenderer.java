package ro.avi.minegicka3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ro.avi.minegicka3.entity.MagicEntity;

/**
 * Draws spell entities as a scaled, tumbling block: rock for boulders, packed ice for icicles,
 * a glowing core for mines. Storms are invisible (their spell draws them).
 */
public class MagicEntityRenderer extends EntityRenderer<MagicEntity, MagicEntityRenderer.State> {
	public static class State extends FallingBlockRenderState {
		float size;
		int look;
		float spin;
		int id;
	}

	public MagicEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		shadowRadius = 0.3f;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	private static BlockState blockFor(int look) {
		return switch (look) {
			case MagicEntity.LOOK_ICE -> Blocks.PACKED_ICE.defaultBlockState();
			case MagicEntity.LOOK_ARCANE -> Blocks.REDSTONE_BLOCK.defaultBlockState();
			case MagicEntity.LOOK_LIFE -> Blocks.EMERALD_BLOCK.defaultBlockState();
			default -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		};
	}

	@Override
	public void extractRenderState(MagicEntity e, State s, float pt) {
		super.extractRenderState(e, s, pt);
		s.size = e.size();
		s.look = e.look();
		s.id = e.getId();
		double speed = e.getDeltaMovement().length();
		s.spin = (float)((e.tickCount + pt) * (speed * 40 + (s.look == MagicEntity.LOOK_ARCANE || s.look == MagicEntity.LOOK_LIFE ? 6 : 0)));
		BlockPos pos = BlockPos.containing(e.getX(), e.getBoundingBox().maxY, e.getZ());
		s.movingBlockRenderState.randomSeedPos = BlockPos.ZERO;
		s.movingBlockRenderState.blockPos = pos;
		s.movingBlockRenderState.blockState = blockFor(s.look);
		if (e.level() instanceof ClientLevel cl) {
			s.movingBlockRenderState.biome = cl.getBiome(pos);
			s.movingBlockRenderState.cardinalLighting = cl.cardinalLighting();
			s.movingBlockRenderState.lightEngine = cl.getLightEngine();
		}
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		if (s.look == MagicEntity.LOOK_NONE) return;
		pose.pushPose();
		float sz = s.size;
		boolean mine = s.look == MagicEntity.LOOK_ARCANE || s.look == MagicEntity.LOOK_LIFE;
		if (mine) sz *= 0.5f;
		pose.translate(0, s.size / 2, 0);
		pose.mulPose(Axis.YP.rotationDegrees(s.spin + s.id * 37));
		if (!mine) pose.mulPose(Axis.XP.rotationDegrees(s.spin * 0.7f));
		if (s.look == MagicEntity.LOOK_ICE && !mine) pose.scale(sz * 0.6f, sz * 0.6f, sz * 2.2f);
		else pose.scale(sz, sz, sz);
		pose.translate(-0.5, -0.5, -0.5);
		out.submitMovingBlock(pose, s.movingBlockRenderState, s.outlineColor);
		pose.popPose();
		super.submit(s, pose, out, camera);
	}
}
