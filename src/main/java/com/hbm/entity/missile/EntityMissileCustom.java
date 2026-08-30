package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.items.weapon.ItemMissile.PartSize;
import com.hbm.items.weapon.ItemMissile.WarheadType;
import com.hbm.items.weapon.MissileItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileCustom} (320 lines, read in full) -
 * the composable-missile entity assembled from up to 5 {@link ItemMissile} parts by {@code
 * ItemCustomMissile.buildMissile}. {@code onMissileImpact}'s {@link WarheadType} switch is the
 * actual warhead-to-explosion-engine dispatch table (see per-case comments below for what's real
 * vs. stubbed).
 * <p>
 * <b>Synced fields</b>: CE syncs 4 raw {@code Item.getIdFromItem} registry ints (WARHEAD/FUSELAGE/
 * FINS/THRUSTER) plus a HEALTH int nothing in CE ever reads back (grep-confirmed - a render-only
 * field with no live consumer in this codebase, dropped entirely). The 4 part ids are re-expressed
 * as {@link EntityDataSerializers#STRING}-synced registry path strings, resolved back against
 * {@link BuiltInRegistries#ITEM} on read, per {@code docs/phase3/missile_framework.md}'s
 * recommendation (Forge-1.12's {@code Item.getIdFromItem}/{@code getItemById} numeric ids don't
 * exist in 1.21.1 at all). An empty string means "no fins" (CE's {@code 0} sentinel for the
 * optional fins slot).
 */
public class EntityMissileCustom extends EntityMissileBaseNT {

    private static final EntityDataAccessor<String> WARHEAD_ID = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FUSELAGE_ID = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FINS_ID = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> THRUSTER_ID = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);

    public float fuel;
    public float consumption;

    // MIRV dispersion, 1.12.2 exclusive thanks to seven (CE's own comment, preserved).
    private static final double[] MIRV_OFF_X = {0.0, 0.45, -0.45, 0.15, -0.15, 0.15, -0.15};
    private static final double[] MIRV_OFF_Z = {0.0, 0.00, 0.00, 0.30, -0.30, -0.30, 0.30};

    public EntityMissileCustom(EntityType<? extends EntityMissileCustom> type, Level level) {
        super(type, level);
    }

    /**
     * CE: {@code EntityMissileCustom(World, float x, float y, float z, int a, int b, MissileStruct
     * template)} - the real programmatic-spawn path (CE's own two real call sites,
     * {@code TileEntityLaunchTable}/{@code TileEntityCompactLauncher}, are explicitly out of this
     * pass's scope, but this factory is exactly what a future launch-infra pass calls into, and
     * what this pass's own smoke-testing spawns directly).
     */
    public static EntityMissileCustom spawn(Level level, double x, double y, double z, int targetX, int targetZ, MissileStruct template) {
        EntityMissileCustom missile = new EntityMissileCustom(MissileEntityTypes.CUSTOM.get(), level);
        missile.initTrajectory(x, y, z, targetX, targetZ);

        missile.entityData.set(WARHEAD_ID, keyOf(template.warhead()));
        missile.entityData.set(FUSELAGE_ID, keyOf(template.fuselage()));
        missile.entityData.set(THRUSTER_ID, keyOf(template.thruster()));
        missile.entityData.set(FINS_ID, template.fins() != null ? keyOf(template.fins()) : "");

        missile.fuel = template.fuselage().getTankSize();
        missile.consumption = (Float) template.thruster().attributes[1];

        return missile;
    }

    private static String keyOf(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "" : id.toString();
    }

    @Nullable
    private ItemMissile resolve(EntityDataAccessor<String> key) {
        String id = this.entityData.get(key);
        if (id.isEmpty()) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
        return item instanceof ItemMissile im ? im : null;
    }

    @Override
    protected void killMissile() {
        if (!this.isRemoved()) {
            this.discard();
            Level level = level();
            Entity detonator = getOwner();
            ExplosionLarge.explode(level, detonator, getX(), getY(), getZ(), 5, true, false, true);
            Vec3 motion = getDeltaMovement();
            ExplosionLarge.spawnShrapnelShower(level, getX(), getY(), getZ(), motion.x, motion.y, motion.z, 15, 0.075);
        }
    }

    @Override
    public void tick() {
        ItemMissile warhead = resolve(WARHEAD_ID);
        if (warhead != null && warhead.attributes != null) {
            WarheadType type = (WarheadType) warhead.attributes[0];
            if (type != null && type.updateCustom != null) {
                type.updateCustom.accept(this);
                if (!level().isClientSide() && this.isRemoved()) return;
            }
        }

        if (!level().isClientSide() && hasPropulsion()) {
            this.fuel -= this.consumption;
        }

        super.tick();
    }

    @Override
    public boolean hasPropulsion() {
        return this.fuel > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WARHEAD_ID, "");
        builder.define(FUSELAGE_ID, "");
        builder.define(FINS_ID, "");
        builder.define(THRUSTER_ID, "");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        fuel = nbt.getFloat("fuel");
        consumption = nbt.getFloat("consumption");
        this.entityData.set(WARHEAD_ID, nbt.getString("warhead"));
        this.entityData.set(FUSELAGE_ID, nbt.getString("fuselage"));
        this.entityData.set(FINS_ID, nbt.getString("fins"));
        this.entityData.set(THRUSTER_ID, nbt.getString("thruster"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("fuel", fuel);
        nbt.putFloat("consumption", consumption);
        nbt.putString("warhead", this.entityData.get(WARHEAD_ID));
        nbt.putString("fuselage", this.entityData.get(FUSELAGE_ID));
        nbt.putString("fins", this.entityData.get(FINS_ID));
        nbt.putString("thruster", this.entityData.get(THRUSTER_ID));
    }

    /**
     * {@code onMissileImpact}'s warhead dispatch (CE's actual explosion-engine call surface, per
     * {@code docs/phase3/missile_framework.md}'s "Key design/API decisions"). Every case whose
     * target already exists in this port is wired for real; {@code VOLCANO}/{@code CLUSTER} are
     * genuine CE no-ops (confirmed by reading this switch in full - CE's own {@code default: break;}
     * silently swallows both, and {@code isCluster} is never set to {@code true} anywhere reachable
     * from this class), preserved deliberately for parity, not "fixed".
     */
    @Override
    public void onMissileImpact(HitResult mop) {
        ItemMissile warhead = resolve(WARHEAD_ID);
        if (warhead == null || warhead.attributes == null) return;

        WarheadType type = (WarheadType) warhead.attributes[0];
        float strength = (Float) warhead.attributes[1];
        Level level = level();
        Entity detonator = getOwner();
        double x = getX(), y = getY(), z = getZ();

        if (type.impactCustom != null) {
            type.impactCustom.accept(this);
            return;
        }

        switch (type) {
            case HE -> {
                ExplosionLarge.explode(level, detonator, x, y, z, strength, true, false, true);
                ExplosionLarge.jolt(level, detonator, x, y, z, strength, (int) (strength * 50), 0.25);
            }
            case INC -> {
                ExplosionLarge.explodeFire(level, detonator, x, y, z, strength, true, false, true);
                ExplosionLarge.jolt(level, detonator, x, y, z, strength * 1.5, (int) (strength * 50), 0.25);
            }
            case CLUSTER -> {
                // CE: `case CLUSTER: break;` - a confirmed real no-op (isCluster is never set true
                // anywhere reachable from EntityMissileCustom). Preserved deliberately, not "fixed".
            }
            case BUSTER -> ExplosionLarge.buster(level, detonator, x, y, z, getDeltaMovement(), strength, strength * 4);
            case NUCLEAR, TX, MIRV -> {
                level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, (int) strength, x, y, z).setDetonator(detonator));
                EntityNukeTorex.statFac(level, x, y, z, strength);
            }
            case BALEFIRE -> {
                EntityBalefire bf = new EntityBalefire(NukeEntityTypes.BALEFIRE.get(), level);
                bf.setPos(x, y, z);
                bf.setDetonator(detonator);
                bf.destructionRange = (int) strength;
                level.addFreshEntity(bf);
                EntityNukeTorex.statFacBale(level, x, y, z, strength);
            }
            case N2 -> {
                level.addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level, (int) strength, x, y, z).setDetonator(detonator));
                EntityNukeTorex.statFac(level, x, y, z, strength);
            }
            case TAINT -> {
                // TODO(BlockTaint, not yet ported): CE randomly seeds `strength*10` taint blocks in a
                // strength-sized cube around the impact via ModBlocks.taint. Not ported - that block
                // family doesn't exist in this port yet (see missile_framework.md's dependency audit).
            }
            case CLOUD -> {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.levelEvent(2002, BlockPos.containing(x, y, z), 0);
                }
                // TODO(ExplosionChaos, not yet ported): CE spawns a chlorine gas cloud
                // (ExplosionChaos.spawnChlorine(level, x - motionX, ..., 750, 2.5, 2)) here.
            }
            case TURBINE -> {
                ExplosionLarge.explode(level, detonator, x, y, z, 10, true, false, true);
                // TODO(EntityBulletBaseNT/BulletConfigSyncingUtil.TURBINE, not yet ported): CE spawns
                // `strength` rotating turbine-blade bullet entities in a radial fan here.
            }
            case VOLCANO -> {
                // CE: no `case VOLCANO` at all despite VOLCANO being a real, registered warhead type -
                // falls through to the no-op `default: break;`. Preserved deliberately for parity, per
                // this project's documented preference for explicit calls over silent "fixes" (see
                // docs/phase3/missile_framework.md's Open questions).
            }
            default -> {
                // every other WarheadType (SCHRAB/APOLLO/SATELLITE/CUSTOM0-9): CE's own no-op default.
            }
        }
    }

    /** CE: {@code mirvSplit()}, the {@code WarheadType.MIRV} {@code updateCustom} hook. */
    public void mirvSplit() {
        if (level().isClientSide()) return;
        if (this.isRemoved()) return;

        Vec3 motion = getDeltaMovement();
        if (motion.y < -1D) {
            LivingEntity thrower = getOwnerAsLiving();
            for (int i = 0; i < 7; i++) {
                EntityMIRV child = new EntityMIRV(MissileEntityTypes.MIRV.get(), level());
                child.setPos(getX(), getY(), getZ());
                child.setDeltaMovement(motion.x + MIRV_OFF_X[i], motion.y, motion.z + MIRV_OFF_Z[i]);

                if (thrower != null) {
                    child.setThrower(thrower);
                }
                level().addFreshEntity(child);
            }
            discard();
        }
    }

    @Nullable
    private LivingEntity getOwnerAsLiving() {
        Entity owner = getOwner();
        return owner instanceof LivingEntity living ? living : null;
    }

    @Override
    public String getTranslationKey() {
        ItemMissile fuselage = resolve(FUSELAGE_ID);
        if (fuselage == null) return "radar.target.custom";

        PartSize top = fuselage.top;
        PartSize bottom = fuselage.bottom;

        if (top == PartSize.SIZE_10 && bottom == PartSize.SIZE_10) return "radar.target.custom10";
        if (top == PartSize.SIZE_10 && bottom == PartSize.SIZE_15) return "radar.target.custom1015";
        if (top == PartSize.SIZE_15 && bottom == PartSize.SIZE_15) return "radar.target.custom15";
        if (top == PartSize.SIZE_15 && bottom == PartSize.SIZE_20) return "radar.target.custom1520";
        if (top == PartSize.SIZE_20 && bottom == PartSize.SIZE_20) return "radar.target.custom20";

        return "radar.target.custom";
    }

    @Override
    public int getBlipLevel() {
        ItemMissile fuselage = resolve(FUSELAGE_ID);
        if (fuselage == null) return IRadarDetectableNT.TIER1;

        PartSize top = fuselage.top;
        PartSize bottom = fuselage.bottom;

        if (top == PartSize.SIZE_10 && bottom == PartSize.SIZE_10) return IRadarDetectableNT.TIER10;
        if (top == PartSize.SIZE_10 && bottom == PartSize.SIZE_15) return IRadarDetectableNT.TIER10_15;
        if (top == PartSize.SIZE_15 && bottom == PartSize.SIZE_15) return IRadarDetectableNT.TIER15;
        if (top == PartSize.SIZE_15 && bottom == PartSize.SIZE_20) return IRadarDetectableNT.TIER15_20;
        if (top == PartSize.SIZE_20 && bottom == PartSize.SIZE_20) return IRadarDetectableNT.TIER20;

        return IRadarDetectableNT.TIER1;
    }

    @Override
    public List<ItemStack> getDebris() {
        return new ArrayList<>();
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return null;
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(MissileItems.MISSILE_CUSTOM.get());
    }
}
