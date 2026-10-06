package ro.avi.minegicka3.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import ro.avi.minegicka3.spell.Spell;

public class ShieldBlockEntity extends MagicBlockEntity {
	public ShieldBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.SHIELD_BE, pos, state);
	}

	/** Life = 200 × staff power ticks (at least 40). */
	public void setup(Spell s) {
		spell = s;
		elements.clear();
		elements.addAll(s.elements);
		life = Math.max(200 * s.staff.power(), 40);
		setChanged();
	}
}
