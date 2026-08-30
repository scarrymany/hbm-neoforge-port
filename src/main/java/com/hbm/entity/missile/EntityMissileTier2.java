package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
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
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileTier2} (104 lines, read in full) - 5
 * nested concrete tier-2 presets. {@code com.hbm.entity.logic.EntityEMP} ({@code
 * EntityMissileEMPStrong}'s payload - distinct from the already-ported {@code EntityEMPBlast} VFX
 * entity Tier0's EMP missile uses) and {@code ExplosionChaos.cluster}/{@code flameDeath} are not
 * ported anywhere in this port yet; each such call site is a documented TODO.
 */
public abstract class EntityMissileTier2 extends EntityMissileBaseNT {

    protected EntityMissileTier2(EntityType<? extends EntityMissileTier2> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_STEEL.get(), 10));
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_TITANIUM.get(), 6));
        // TODO(ModItems.thruster_medium, not yet registered in this port).
        return list;
    }

    @Override
    public String getTranslationKey() {
        return "radar.target.tier2";
    }

    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.TIER2;
    }

    public static class EntityMissileStrong extends EntityMissileTier2 {
        public EntityMissileStrong(EntityType<? extends EntityMissileStrong> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            this.explodeStandard(30F, 32, false);
            // TODO(ExplosionCreator.composeEffectStandard, Phase 5): VFX only.
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_generic_medium, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_STRONG.get());
        }
    }

    public static class EntityMissileIncendiaryStrong extends EntityMissileTier2 {
        public EntityMissileIncendiaryStrong(EntityType<? extends EntityMissileIncendiaryStrong> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            this.explodeStandard(30F, 32, true);
            // TODO(ExplosionCreator.composeEffectStandard, Phase 5): VFX only.
            // TODO(ExplosionChaos.flameDeath, not yet ported): CE ignites a 25-block-radius area here.
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_incendiary_medium, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_INCENDIARY_STRONG.get());
        }
    }

    public static class EntityMissileClusterStrong extends EntityMissileTier2 {
        public EntityMissileClusterStrong(EntityType<? extends EntityMissileClusterStrong> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 15F, true, Level.ExplosionInteraction.TNT);
            // TODO(ExplosionChaos.cluster, not yet ported): CE scatters 50 sub-munitions here.
        }

        @Override
        public void cluster() {
            this.onMissileImpact(null);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_cluster_medium, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_CLUSTER_STRONG.get());
        }
    }

    public static class EntityMissileBusterStrong extends EntityMissileTier2 {
        public EntityMissileBusterStrong(EntityType<? extends EntityMissileBusterStrong> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            for (int i = 0; i < 20; i++) {
                level().explode(this, getX(), getY() - i, getZ(), 7.5F, true, Level.ExplosionInteraction.TNT);
            }
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 8);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 8);
            ExplosionLarge.spawnRubble(level(), getX(), getY(), getZ(), 8);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_buster_medium, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_BUSTER_STRONG.get());
        }
    }

    public static class EntityMissileEMPStrong extends EntityMissileTier2 {
        public EntityMissileEMPStrong(EntityType<? extends EntityMissileEMPStrong> type, Level level) {
            super(type, level);
        }

        /** TODO({@code com.hbm.entity.logic.EntityEMP}, not yet ported): CE spawns that payload entity here. */
        @Override
        public void onMissileImpact(HitResult mop) {
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_generic_medium, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_EMP_STRONG.get());
        }
    }
}
