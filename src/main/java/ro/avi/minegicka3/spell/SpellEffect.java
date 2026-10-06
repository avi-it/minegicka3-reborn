package ro.avi.minegicka3.spell;

/** A server-side effect ticked by SpellManager; tick() returns false when it is done. */
public interface SpellEffect {
	boolean tick();
}
