package com.hbm.entity.effect;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Port of CE's {@code com.hbm.entity.effect.EntityFireLingering} (112 lines, read in full) - a
 * stationary rectangular area-denial entity spawned by incendiary/napalm/white-phosphorus/balefire
 * weapon payloads. Per {@code docs/phase4/entities_orbital_and_beam_payloads.md}'s headline finding
 * #2, its server tick is the real gameplay mechanic (not decorative Phase 5 scope, despite several
 * already-committed forward-reference javadocs elsewhere in this tree over-generalizing it as such):
 * every tick it scans its own {@code width x height} box and applies real, ongoing burn state via
 * {@link HbmLivingAttachment#setFire}/{@link HbmLivingAttachment#setBalefire}.
 * <p>
 * <b>Deliberate simplification vs. CE's real hitbox</b>: CE calls {@code this.setSize(width,
 * height)} every tick, mutating the entity's own actual collision bounding box to match the synced
 * area. This port instead builds the scan {@link AABB} directly from the synced width/height fields
 * without touching the entity's registered hitbox/{@code EntityDimensions} - this entity has no
 * collision behavior of its own (nothing ever collides with it, it never moves), so the only thing
 * that actually matters is the box used for the per-tick entity scan, which is reproduced exactly.
 * <p>
 * <b>{@code TYPE_BLACK}</b> is preserved as CE's own unfinished, functionally inert constant (CE's
 * own {@code // TODO implement black fire} comment on the field) - no behavior is invented for it,
 * matching {@code docs/phase4/entities_orbital_and_beam_payloads.md}'s Open questions section.
 * <p>
 * <b>Not ported</b>: CE's client-side {@code FlameCreator.composeEffectClient} particle simulation
 * and the {@code @AutoRegister(sendVelocityUpdates = false)} bandwidth flag (no equivalent knob
 * found anywhere in this port's {@link EntityType.Builder} usage - same open question already
 * raised by {@code docs/phase3/grenades.md} and independently re-confirmed by the Phase 4 report
 * above). Both are documented Phase 5 / non-blocking gaps, not silently dropped behavior.
 */
public class EntityFireLingering extends Entity {

    public static final int TYPE_DIESEL = 0;
    public static final int TYPE_BALEFIRE = 1;
    public static final int TYPE_PHOSPHORUS = 2;
    public static final int TYPE_OXY = 3;
    /** CE's own unfinished "black fire" constant - instantiable but functionally inert, see class javadoc. */
    public static final int TYPE_BLACK = 4;

    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.INT);

    public int maxAge = 150;
    /**
     * CE's {@code ticksExisted}. Tracked manually rather than relying on vanilla's
     * {@link Entity#tickCount} - {@link #tick()} deliberately never calls {@code super.tick()}/
     * {@code baseTick()} (matching CE's own onEntityUpdate() fully overriding the base class), and
     * {@code tickCount} is only ever incremented from inside {@code baseTick()} - the same
     * manual-counter pattern this file's sibling {@code EntityCloudFleija}/{@code
     * EntityCloudSolinium}/{@code EntityEMPBlast} already use, for the same reason.
     */
    private int age;

    public EntityFireLingering(EntityType<? extends EntityFireLingering> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
        builder.define(WIDTH, 0F);
        builder.define(HEIGHT, 0F);
    }

    public EntityFireLingering setArea(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        return this;
    }

    public EntityFireLingering setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    public EntityFireLingering setType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    public int getLingeringType() {
        return this.entityData.get(TYPE);
    }

    @Override
    public void tick() {
        // No super.tick() call - matches CE's onEntityUpdate() fully overriding Entity#onEntityUpdate()
        // with no super call: this is a purely synthetic, non-physical area-denial marker with no
        // vanilla movement/collision/fire-tick behavior of its own.

        if (!level().isClientSide()) {

            if (this.age >= maxAge) {
                this.discard();
                return;
            }
            this.age++;

            float width = this.entityData.get(WIDTH);
            float height = this.entityData.get(HEIGHT);
            int type = getLingeringType();

            AABB scan = new AABB(
                    getX() - width / 2D, getY(), getZ() - width / 2D,
                    getX() + width / 2D, getY() + height, getZ() + width / 2D);

            for (Entity e : level().getEntities(this, scan, entity -> true)) {
                if (e instanceof LivingEntity living) {
                    HbmLivingAttachment props = HbmLivingAttachment.getData(living);
                    boolean changed = false;

                    if (type == TYPE_DIESEL && props.getFire() < 60) {
                        props.setFire(60);
                        changed = true;
                    }
                    if (type == TYPE_PHOSPHORUS && props.getFire() < 300) {
                        props.setFire(300);
                        changed = true;
                    }
                    if (type == TYPE_BALEFIRE && props.getBalefire() < 100) {
                        props.setBalefire(100);
                        changed = true;
                    }

                    if (changed) {
                        living.setData(ModAttachments.LIVING_ATTACHMENT, props);
                    }
                } else {
                    e.igniteForSeconds(4);
                }
            }
        } else {
            // TODO(Phase 5): CE's client-side FlameCreator.composeEffectClient ground-fire particle
            // sim lives here (purely cosmetic, no gameplay effect - see class javadoc).
        }
    }

    /** Ephemeral marker entity - never meant to survive a save/load cycle, matching CE's
     *  {@code writeToNBTOptional() == false} / no-op {@code writeEntityToNBT} / dead-on-load
     *  {@code readEntityFromNBT}. */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean save(CompoundTag tag) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    /**
     * Convenience factory matching CE's real call shape (every actual spawn site constructs, then
     * chains {@code setArea/setType/setDuration}, then spawns) - not a 1:1 CE method, but the
     * natural landing spot for the several forward-reference call sites across the weapons content
     * classes once they wire this entity in.
     */
    public static EntityFireLingering spawn(Level level, double x, double y, double z, float width, float height, int type, int duration) {
        EntityFireLingering fire = new EntityFireLingering(EffectEntityTypes.FIRE_LINGERING.get(), level)
                .setArea(width, height)
                .setType(type)
                .setDuration(duration);
        fire.setPos(x, y, z);
        level.addFreshEntity(fire);
        return fire;
    }
}
