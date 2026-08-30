package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.weapon.MissileItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileTier1} (90 lines, read in full) - 5
 * nested concrete tier-1 presets. {@code ExplosionCreator.composeEffect*} (a networked-particle-VFX
 * helper, not ported anywhere in this port - Phase 5) and {@code ExplosionChaos.cluster} (the
 * composable-explosion "cluster" helper, not ported - see {@code docs/phase3/missile_framework.md})
 * are the two dependencies this file needs that don't exist yet; each such call site below is a
 * documented TODO, the real destructive effect alongside it is kept.
 */
public abstract class EntityMissileTier1 extends EntityMissileBaseNT {

    protected EntityMissileTier1(EntityType<? extends EntityMissileTier1> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_TITANIUM.get(), 4));
        // TODO(ModItems.thruster_small, not yet registered in this port).
        return list;
    }

    public static class EntityMissileGeneric extends EntityMissileTier1 {
        public EntityMissileGeneric(EntityType<? extends EntityMissileGeneric> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            this.explodeStandard(15F, 24, false);
            // TODO(ExplosionCreator.composeEffectSmall, Phase 5): VFX only.
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_generic_small, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_GENERIC.get());
        }
    }

    public static class EntityMissileDecoy extends EntityMissileTier1 {
        public EntityMissileDecoy(EntityType<? extends EntityMissileDecoy> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 4F, false, Level.ExplosionInteraction.NONE);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(IngotNuggetItems.INGOT_STEEL.get());
        }

        @Override
        public String getTranslationKey() {
            return "radar.target.tier4";
        }

        @Override
        public int getBlipLevel() {
            return IRadarDetectableNT.TIER4;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_DECOY.get());
        }
    }

    public static class EntityMissileIncendiary extends EntityMissileTier1 {
        public EntityMissileIncendiary(EntityType<? extends EntityMissileIncendiary> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            this.explodeStandard(15F, 24, true);
            // TODO(ExplosionCreator.composeEffectSmall, Phase 5): VFX only.
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_incendiary_small, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_INCENDIARY.get());
        }
    }

    public static class EntityMissileCluster extends EntityMissileTier1 {
        public EntityMissileCluster(EntityType<? extends EntityMissileCluster> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 5F, true, Level.ExplosionInteraction.TNT);
            // TODO(ExplosionChaos.cluster, not yet ported): CE scatters 25 sub-munitions across a
            // 100-block radius here.
        }

        @Override
        public void cluster() {
            this.onMissileImpact(null);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_cluster_small, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_CLUSTER.get());
        }
    }

    public static class EntityMissileBunkerBuster extends EntityMissileTier1 {
        public EntityMissileBunkerBuster(EntityType<? extends EntityMissileBunkerBuster> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            for (int i = 0; i < 15; i++) {
                level().explode(this, getX(), getY() - i, getZ(), 5F, true, Level.ExplosionInteraction.TNT);
            }
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 5);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 5);
            ExplosionLarge.spawnRubble(level(), getX(), getY(), getZ(), 5);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_buster_small, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_BUSTER.get());
        }
    }
}
