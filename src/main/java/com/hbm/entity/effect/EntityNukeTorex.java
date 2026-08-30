package com.hbm.entity.effect;

import com.hbm.interfaces.IConstantRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityNukeTorex} (616 lines, read in full) - the
 * mushroom-cloud "Toroidial Convection Simulation" VFX entity spawned alongside most nuke
 * detonations. This is the class {@code com.hbm.hazard.type.HazardTypeUnstable} already imports
 * and calls {@link #statFac} on (see that file and {@code docs/phase3/melee_weapons.md}'s headline
 * finding #5) - the signature below matches its 2 existing call sites exactly.
 * <p>
 * Per this pass's task brief: CE's own {@code onUpdate} body is entirely {@code if
 * (world.isRemote) { ...cloudlet particle simulation... }} plus one server-side max-age despawn
 * check (confirmed by reading the entity in full - zero block-mutation/world-state logic anywhere
 * in this class), so only entity registration, the server-side lifetime/despawn check, and the
 * {@link #statFac}/{@link #statFacBale} factory signatures are ported here. The ~500-line
 * client-only {@code Cloudlet} particle simulation (toroidal convection motion, ring/condensation/
 * shockwave sub-clouds, color/alpha interpolation - all inside {@code if (world.isRemote)} and
 * consumed only by Phase 5's renderer) is a documented Phase 5 TODO rather than ported dead weight,
 * per the task brief's explicit "stub pure-rendering method bodies" instruction; {@link #getScale}/
 * {@link #getType} are kept since the (stubbed) renderer will need them, and {@link #setScale}
 * keeps only the one line that has a real gameplay effect ({@code maxAge}, which the despawn check
 * below actually consults) rather than the four purely-cosmetic {@code coreHeight}/{@code
 * convectionHeight}/{@code torusWidth}/{@code rollerSize} field scalings CE also performs there.
 */
public class EntityNukeTorex extends Entity implements IConstantRenderer {

    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.BYTE);

    public int maxAge = 1000;

    public EntityNukeTorex(EntityType<? extends EntityNukeTorex> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0F);
        builder.define(TYPE, (byte) 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.contains("scale")) setScale(nbt.getFloat("scale"));
        if (nbt.contains("type")) this.entityData.set(TYPE, nbt.getByte("type"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putFloat("scale", this.entityData.get(SCALE));
        nbt.putByte("type", this.entityData.get(TYPE));
    }

    /** CE returns {@code false} from both {@code writeToNBTOptional}/{@code writeToNBTAtomically} - this ephemeral VFX entity is never meant to survive a save/load cycle. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        // TODO(Phase 5): CE's entire client-side cloudlet particle simulation (toroidal convection
        // motion, ring/condensation/shockwave sub-clouds, per-cloudlet color/alpha interpolation -
        // ~500 lines, all inside `if (world.isRemote)`) lives here. Zero gameplay effect; see class
        // javadoc. Not ported in this pass.

        if (!level().isClientSide() && this.tickCount > maxAge) {
            this.discard();
        }
    }

    public EntityNukeTorex setScale(float scale) {
        if (!level().isClientSide()) {
            this.entityData.set(SCALE, scale);
        }
        this.maxAge = (int) (45 * 20 * scale);
        return this;
    }

    public EntityNukeTorex setType(int type) {
        this.entityData.set(TYPE, (byte) type);
        return this;
    }

    public double getScale() {
        return this.entityData.get(SCALE);
    }

    public byte getType() {
        return this.entityData.get(TYPE);
    }

    /** Spawns a standard Torex. Matches {@code HazardTypeUnstable}'s existing call sites exactly. */
    public static void statFac(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(EffectEntityTypes.TOREX.get(), level)
                .setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F));
        torex.setPos(x, y, z);
        level.addFreshEntity(torex);
    }

    /** Spawns a Torex, balefire variant. */
    public static void statFacBale(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(EffectEntityTypes.TOREX.get(), level)
                .setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F))
                .setType(1);
        torex.setPos(x, y, z);
        level.addFreshEntity(torex);
    }
}
