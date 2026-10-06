package ro.avi.minegicka3.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import ro.avi.minegicka3.spell.Spell;

/**
 * Base of the spell entities. They carry a live Spell on the server and are never saved: after a reload there is
 * no spell to run, so they vanish. Size and look are synced for the client renderer.
 */
public abstract class MagicEntity extends Entity {
	/** Visual kinds for MagicEntityRenderer. */
	public static final int LOOK_ROCK = 0, LOOK_ICE = 1, LOOK_ARCANE = 2, LOOK_LIFE = 3, LOOK_NONE = 4;
	private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MagicEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.defineId(MagicEntity.class, EntityDataSerializers.INT);

	public Spell spell;

	protected MagicEntity(EntityType<? extends MagicEntity> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(SIZE, 0.25f);
		b.define(LOOK, LOOK_ROCK);
	}

	public float size() {
		return entityData.get(SIZE);
	}

	public void setSize(float s) {
		entityData.set(SIZE, s);
		refreshDimensions();
	}

	public int look() {
		return entityData.get(LOOK);
	}

	public void setLook(int l) {
		entityData.set(LOOK, l);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (SIZE.equals(key)) refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return EntityDimensions.scalable(size(), size());
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && spell == null) discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
