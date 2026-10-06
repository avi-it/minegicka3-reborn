package ro.avi.minegicka3.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import ro.avi.minegicka3.Element;
import ro.avi.minegicka3.spell.CastType;
import ro.avi.minegicka3.spell.Spell;
import ro.avi.minegicka3.spell.StaffStats;

public class WallBlockEntity extends MagicBlockEntity {
	public WallBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.WALL_BE, pos, state);
	}

	/** Life = Earth·150 + √Ice·15 ticks (at least 40). */
	public void setup(Spell s) {
		spell = s;
		elements.clear();
		elements.addAll(s.elements);
		life = (int)Math.max(s.count(Element.EARTH) * 150 + Math.sqrt(s.count(Element.ICE)) * 15, 40);
		setChanged();
	}

	/** Walls with anything besides Earth (ice spikes, fire...) hurt whoever touches them. */
	public void touch(Entity e) {
		if (elements.stream().allMatch(x -> x == Element.EARTH)) return;
		if (spell == null) {
			spell = new Spell(elements, CastType.SINGLE, null, null, StaffStats.DEFAULT);
			spell.fixedLevel = (net.minecraft.server.level.ServerLevel)e.level();
		}
		spell.affect(e, (int)(80 / spell.staff.atkSpeed()));
	}
}
