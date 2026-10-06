package ro.avi.minegicka3.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import ro.avi.minegicka3.spell.Spell;
import ro.avi.minegicka3.spell.SpellManager;
import ro.avi.minegicka3.spell.exec.SpellExecute;

/** Shield + Spray/Lightning: a hovering point that keeps casting the spell straight down. */
public class StormEntity extends MagicEntity {
	private int maxTick = 100;
	private Spell running;

	public StormEntity(EntityType<? extends StormEntity> type, Level level) {
		super(type, level);
		setXRot(90); // look straight down
		setLook(LOOK_NONE);
	}

	public void begin(Spell parentSpell, java.util.List<ro.avi.minegicka3.Element> elements) {
		running = parentSpell.derive(elements, ro.avi.minegicka3.spell.CastType.SINGLE, this);
		spell = running;
		maxTick = 75 + 25 * running.count();
		SpellExecute.of(running).start(running);
		if (!running.finished) SpellManager.detach(running);
	}

	@Override
	public void tick() {
		setXRot(90);
		super.tick();
		if (isRemoved() || running == null) return;
		// spray storms only fire every other tick
		running.paused = running.type == ro.avi.minegicka3.spell.SpellType.SPRAY && (tickCount + getId()) % 2 == 1;
		if (tickCount > maxTick) {
			running.finished = true;
			discard();
		}
	}
}
