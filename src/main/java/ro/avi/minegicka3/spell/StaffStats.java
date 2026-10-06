package ro.avi.minegicka3.spell;

/** Multipliers a staff gives to spells cast with it (Minegicka's "Staff" NBT tag). */
public record StaffStats(double power, double atkSpeed, double consume, double recover) {
	public static final StaffStats DEFAULT = new StaffStats(1, 1, 1, 1);
}
