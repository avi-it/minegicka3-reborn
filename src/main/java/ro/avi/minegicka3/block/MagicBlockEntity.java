package ro.avi.minegicka3.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.spell.Spell;

/** Shared state of shield and wall blocks: remaining life (ticks) and the spell's elements. Saved with the world. */
public abstract class MagicBlockEntity extends BlockEntity {
	public double life = 40;
	public final List<Element> elements = new ArrayList<>();
	/** The live spell while the caster's session lasts; walls fall back to an ownerless copy. */
	public Spell spell;

	protected MagicBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void damage(double amount) {
		life -= amount;
		setChanged();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, MagicBlockEntity be) {
		be.life -= 1;
		if (be.spell != null) be.spell.tickCooldowns();
		be.onTick((ServerLevel)level);
		if (be.life <= 0) level.removeBlock(pos, false);
	}

	protected void onTick(ServerLevel level) {
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		life = in.getDoubleOr("Life", 40);
		elements.clear();
		in.getIntArray("Elements").ifPresent(a -> {
			for (int i : a) {
				Element e = Element.byId(i);
				if (e != null) elements.add(e);
			}
		});
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("Life", life);
		out.putIntArray("Elements", elements.stream().mapToInt(Enum::ordinal).toArray());
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
