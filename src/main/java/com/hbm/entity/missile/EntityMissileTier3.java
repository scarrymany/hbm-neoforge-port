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
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileTier3} (112 lines, read in full) - 4
 * nested concrete tier-3 presets. {@code ExplosionChaos.burn}/{@code flameDeath} and {@code
 * ExplosionNT}/{@code ExplosionNT.ExAttrib} (a different, older explosion-engine family than the
 * {@code vanillant} one this port has ported - not read/scoped by this pass, see {@code
 * docs/phase3/missile_framework.md}) are not ported anywhere in this port yet; {@link
 * EntityMissileDrill}'s erosion tunnel substitutes {@link ExplosionLarge#jolt} (this package's own
 * confirmed real "digs a tunnel" primitive) for the unavailable {@code ExplosionNT.ExAttrib.ERRODE}
 * mechanic, which is a close behavioral analogue, not a literal translation - documented, not
 * silent.
 * <p>
 * {@code spawnContrail} (CE's 4-point exhaust-plume VFX override) is not ported - purely cosmetic,
 * see {@link EntityMissileBaseNT}'s javadoc.
 */
public abstract class EntityMissileTier3 extends EntityMissileBaseNT {

    protected EntityMissileTier3(EntityType<? extends EntityMissileTier3> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_STEEL.get(), 16));
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_TITANIUM.get(), 10));
        // TODO(ModItems.thruster_large, not yet registered in this port).
        return list;
    }

    @Override
    public String getTranslationKey() {
        return "radar.target.tier3";
    }

    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.TIER3;
    }

    public static class EntityMissileBurst extends EntityMissileTier3 {
        public EntityMissileBurst(EntityType<? extends EntityMissileBurst> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            this.explodeStandard(50F, 48, false);
            // TODO(ExplosionCreator.composeEffectLarge, Phase 5): VFX only.
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_generic_large, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_BURST.get());
        }
    }

    public static class EntityMissileInferno extends EntityMissileTier3 {
        public EntityMissileInferno(EntityType<? extends EntityMissileInferno> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            this.explodeStandard(50F, 48, true);
            // TODO(ExplosionCreator.composeEffectLarge, Phase 5): VFX only.
            // TODO(ExplosionChaos.burn/flameDeath, not yet ported): CE burns/ignites a 25-block-radius area here.
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_incendiary_large, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_INFERNO.get());
        }
    }

    public static class EntityMissileRain extends EntityMissileTier3 {
        public EntityMissileRain(EntityType<? extends EntityMissileRain> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 25F, true, Level.ExplosionInteraction.TNT);
            // TODO(ExplosionChaos.cluster, not yet ported): CE scatters 100 sub-munitions here.
        }

        @Override
        public void cluster() {
            this.onMissileImpact(null);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_cluster_large, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_RAIN.get());
        }
    }

    public static class EntityMissileDrill extends EntityMissileTier3 {
        public EntityMissileDrill(EntityType<? extends EntityMissileDrill> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            // TODO(ExplosionNT/ExAttrib.ERRODE, not ported - older explosion family, see class
            // javadoc): CE builds 30 stacked ExplosionNT instances descending from impact here,
            // each with the ERRODE attribute (erodes terrain along its blast). Not approximated -
            // ExplosionLarge.jolt below is CE's own separate, real, already-present call in this
            // same method, not a substitute for the missing loop.
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 25);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 12);
            ExplosionLarge.jolt(level(), getOwner(), getX(), getY(), getZ(), 10, 50, 1);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.warhead_buster_large, not yet registered in this port).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_DRILL.get());
        }
    }
}
