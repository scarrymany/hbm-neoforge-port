package com.hbm.entity.logic;

import com.hbm.entity.item.EntityParachuteCrate;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsC130;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.entity.logic.EntityC130} (101 lines, read in full) - a scripted supply
 * plane, per both {@code docs/phase4/entities_vehicles_aircraft.md} and
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md} (jointly authoritative, no discrepancy
 * per the second report's own cross-check).
 * <p>
 * At exactly the halfway point of its lifetime ({@code tickCount == getLifetime() / 2}), spawns one
 * {@link EntityParachuteCrate} loaded with items drawn from {@link ItemPoolsC130} depending on {@link
 * #payload}: {@code SUPPLIES} rolls {@code POOL_SUPPLIES} 5 times; {@code WEAPONS} rolls 1-2 from
 * {@code POOL_WEAPONS} plus 6 from {@code POOL_AMMO}. {@code A_FUCKING_FUEL_TRUCK} is CE's own dead
 * enum value (confirmed by grep of CE's source - zero code path ever sets it) - no behavior is
 * implemented for it here either, matching CE exactly.
 * <p>
 * Spawned via plain {@code level.addFreshEntity(...)}, not CE's {@code WorldUtil.
 * loadAndSpawnEntityInWorld} chunk-preload loop - {@link EntityPlaneBase}'s own {@link IChunkLoader}
 * implementation already keeps the plane's current chunk force-loaded, making CE's manual 5x5 preload
 * unnecessary (both reports' confirmed finding). {@code TrackerUtil.setTrackingRange} is dropped
 * entirely per the same established precedent (Headline finding #5 of the orbital-payloads report).
 */
public class EntityC130 extends EntityPlaneBase {

    public C130PayloadType payload = C130PayloadType.SUPPLIES;

    public EntityC130(EntityType<? extends EntityC130> type, Level level) {
        super(type, level);
    }

    public EntityC130(Level level) {
        this(PlaneEntityTypes.C130.get(), level);
    }

    @Override
    public void tick() {
        super.tick();

        Level level = this.level();
        if (level.isClientSide() || this.isRemoved()) return;

        if (this.tickCount == this.getLifetime() / 2 && this.health > 0) {
            Vec3 motion = this.getDeltaMovement();
            EntityParachuteCrate crate = new EntityParachuteCrate(level, getX() - motion.x * 7, getY() - 10, getZ() - motion.z * 7);
            RandomSource rand = this.random;

            if (this.payload == C130PayloadType.SUPPLIES) {
                for (int i = 0; i < 5; i++) {
                    crate.items.add(ItemPool.getStack(ItemPool.getPool(ItemPoolsC130.POOL_SUPPLIES), rand));
                }
            } else if (this.payload == C130PayloadType.WEAPONS) {
                int amount = 1 + rand.nextInt(2);
                for (int i = 0; i < amount; i++) {
                    crate.items.add(ItemPool.getStack(ItemPool.getPool(ItemPoolsC130.POOL_WEAPONS), rand));
                }
                for (int i = 0; i < 6; i++) {
                    crate.items.add(ItemPool.getStack(ItemPool.getPool(ItemPoolsC130.POOL_AMMO), rand));
                }
            }
            // A_FUCKING_FUEL_TRUCK: CE dead enum value, no behavior - see class javadoc.

            level.addFreshEntity(crate);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int ordinal = tag.getInt("payload");
        C130PayloadType[] values = C130PayloadType.values();
        this.payload = ordinal >= 0 && ordinal < values.length ? values[ordinal] : C130PayloadType.SUPPLIES;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("payload", this.payload.ordinal());
    }

    /**
     * CE: {@code fac(World, x, y, z, C130PayloadType)} - positions the plane ~100 blocks upwind of the
     * target at Y+100, aimed inward, matching CE's own random-direction spawn vector.
     */
    public void fac(Level level, double x, double y, double z, C130PayloadType payload) {
        Vec3 vector = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize().scale(2);

        this.payload = payload;
        this.moveTo(x - vector.x * 100, y + 100, z - vector.z * 100, 0F, 0F);
        this.setDeltaMovement(vector.x, 0D, vector.z);
        this.rotation();
    }

    public enum C130PayloadType {
        SUPPLIES,
        WEAPONS,
        /** CE dead enum value - see class javadoc, never set by any real code path. */
        A_FUCKING_FUEL_TRUCK
    }
}
