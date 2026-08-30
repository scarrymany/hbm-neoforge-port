package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.weapon.MissileItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileTier4} (156 lines, read in full) - 6
 * nested concrete tier-4 (nuclear-class) presets, the highest-tier standard missiles.
 * {@code ModBlocks.volcanic_lava_block}/{@code volcano_core} (Volcano's terraforming payload) are
 * not registered anywhere in this port yet - the real {@link ExplosionLarge#explode} call is kept,
 * the block placement is a documented TODO. {@code WorldUtil.loadAndSpawnEntityInWorld} (CE's
 * chunk-safe spawn helper) is replaced with a plain {@code level.addFreshEntity(...)} - every real
 * nuke entity spawned here ({@link EntityNukeExplosionMK5}) already handles its own chunk-loading
 * via {@code EntityExplosionChunkloading}, so no behavior is lost.
 * <p>
 * {@code ROT_IDX} (CE's synced byte driving {@code spawnContrail}'s exhaust-plume rotation) is not
 * ported - purely cosmetic, see {@link EntityMissileBaseNT}'s javadoc.
 */
public abstract class EntityMissileTier4 extends EntityMissileBaseNT {

    protected EntityMissileTier4(EntityType<? extends EntityMissileTier4> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_TITANIUM.get(), 16));
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_STEEL.get(), 20));
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_ALUMINIUM.get(), 12));
        // TODO(ModItems.thruster_large, not yet registered in this port).
        return list;
    }

    @Override
    public String getTranslationKey() {
        return "radar.target.tier4";
    }

    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.TIER4;
    }

    public static class EntityMissileNuclear extends EntityMissileTier4 {
        public EntityMissileNuclear(EntityType<? extends EntityMissileNuclear> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            int radius = BombConfig.MISSILE_RADIUS.get();
            level().addFreshEntity(EntityNukeExplosionMK5.statFac(level(), radius, getX(), getY(), getZ()).setDetonator(getOwner()));
            EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_nuclear, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_NUCLEAR.get());
        }
    }

    public static class EntityMissileMirv extends EntityMissileTier4 {
        public EntityMissileMirv(EntityType<? extends EntityMissileMirv> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            int radius = BombConfig.MISSILE_RADIUS.get() * 2;
            level().addFreshEntity(EntityNukeExplosionMK5.statFac(level(), radius, getX(), getY(), getZ()).setDetonator(getOwner()));
            EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
        }

        @Override
        public List<ItemStack> getDebris() {
            List<ItemStack> list = new ArrayList<>();
            list.add(new ItemStack(PlateCrystalWasteItems.PLATE_TITANIUM.get(), 16));
            list.add(new ItemStack(PlateCrystalWasteItems.PLATE_STEEL.get(), 20));
            list.add(new ItemStack(PlateCrystalWasteItems.PLATE_ALUMINIUM.get(), 12));
            return list;
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_mirv, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_NUCLEAR_CLUSTER.get());
        }
    }

    public static class EntityMissileVolcano extends EntityMissileTier4 {
        public EntityMissileVolcano(EntityType<? extends EntityMissileVolcano> type, Level level) {
            super(type, level);
        }

        /** TODO({@code ModBlocks.volcanic_lava_block}/{@code volcano_core}, not yet ported): CE seeds a 3x3x3 lava/core patch here. */
        @Override
        public void onMissileImpact(HitResult mop) {
            ExplosionLarge.explode(level(), getOwner(), getX(), getY(), getZ(), 10.0F, true, true, true);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_volcano, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_VOLCANO.get());
        }
    }

    public static class EntityMissileDoomsday extends EntityMissileTier4 {
        public EntityMissileDoomsday(EntityType<? extends EntityMissileDoomsday> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            int radius = BombConfig.MISSILE_RADIUS.get() * 2;
            level().addFreshEntity(EntityNukeExplosionMK5.statFac(level(), radius, getX(), getY(), getZ()).moreFallout(100).setDetonator(getOwner()));
            EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
        }

        @Override
        public List<ItemStack> getDebris() {
            return null;
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return null;
        }

        @Override
        public String getTranslationKey() {
            return "radar.target.doomsday";
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_DOOMSDAY.get());
        }
    }

    public static class EntityMissileDoomsdayRusted extends EntityMissileDoomsday {
        public EntityMissileDoomsdayRusted(EntityType<? extends EntityMissileDoomsdayRusted> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            int radius = BombConfig.MISSILE_RADIUS.get();
            level().addFreshEntity(EntityNukeExplosionMK5.statFac(level(), radius, getX(), getY(), getZ()).moreFallout(100).setDetonator(getOwner()));
            EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_DOOMSDAY_RUSTED.get());
        }
    }

    /** {@code //mlbv: n2 missile does not exist in upstream} - CE-CE-original content, ported as-is. */
    public static class EntityMissileN2 extends EntityMissileTier4 {
        public EntityMissileN2(EntityType<? extends EntityMissileN2> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            int radius = (BombConfig.N2_RADIUS.get() / 12) * 5;
            level().addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level(), radius, getX(), getY(), getZ()).setDetonator(getOwner()));
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_n2, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_N2.get());
        }
    }
}
