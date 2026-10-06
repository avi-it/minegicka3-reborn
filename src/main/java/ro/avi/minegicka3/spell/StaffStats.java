package ro.avi.minegicka3.spell;

/** Multipliers a staff gives to spells cast with it, and how many elements it can queue. */
public record StaffStats(double power, double atkSpeed, double consume, double recover, int queue) {
	public static final StaffStats DEFAULT = new StaffStats(1, 1, 1, 1, 5);
}
