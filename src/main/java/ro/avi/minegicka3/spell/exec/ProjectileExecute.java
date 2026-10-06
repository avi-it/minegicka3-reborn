package ro.avi.minegicka3.spell.exec;

import ro.avi.minegicka3.spell.Spell;

public class ProjectileExecute extends SpellExecute {
	@Override
	public void start(Spell s) {
		s.finished = true;
	}
}
