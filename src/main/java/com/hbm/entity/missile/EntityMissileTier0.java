package com.hbm.entity.missile;

import com.hbm.blocks.bomb.BombBlocks;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.special.ScatteredMilitaryItems;
import com.hbm.items.weapon.MissileItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileTier0} (171 lines, read in full) -
 * the abstract base for the 6 lowest-tier standard-missile presets, plus its 6 nested concrete
 * subclasses. {@link EntityMissileTest} has no matching {@code ItemMissileStandard} registry entry
 * at all (confirmed by grep of CE's {@code ModItems.java}) - a leftover dev-only entity, not a
 * porting gap.
 * <p>
 * Several concrete debris/rare-drop items this file's CE original references
 * ({@code ModItems.wire_fine}/{@code shell}/{@code ducttape}, {@code ModItems.ammo_standard} +
 * {@code GunFactory.EnumAmmo.NUKE_HIGH}, {@code ModBlocks.taint}/{@code sellafield_slaked}) are not
 * registered anywhere in this port yet - each such call site is a documented TODO below, the real
 * explosion/damage effect is kept wherever its own target already exists.
 * {@link EntityMissileBHole}'s {@code EntityBlackHole} spawn/{@code ModItems.black_hole} debris drop
 * are now wired, per docs/phase4/entities_vortex_gravity_wells.md - both dependencies (the entity
 * family, and Phase 3's {@code ScatteredMilitaryItems.BLACK_HOLE}) now exist in this port.
 */
public abstract class EntityMissileTier0 extends EntityMissileBaseNT {

    protected EntityMissileTier0(EntityType<? extends EntityMissileTier0> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getDebris() {
        // TODO(wire_fine/shell/ducttape, not yet registered in this port): CE drops 4x wire_fine
        // (aluminium), 4x plate_titanium, 2x shell (aluminium), 1x ducttape here.
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(com.hbm.items.PlateCrystalWasteItems.PLATE_TITANIUM.get(), 4));
        return list;
    }

    public static class EntityMissileTest extends EntityMissileTier0 {
        public EntityMissileTest(EntityType<? extends EntityMissileTest> type, Level level) {
            super(type, level);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_MICRO.get());
        }

        /**
         * TODO({@code ModBlocks.sellafield_slaked}, not yet ported): CE chars a 50-block-radius
         * sphere into a variable-charring "sellafield_slaked" block, and clears everything else non-
         * air. That block family does not exist in this port yet - preserved as a documented no-op
         * rather than approximated with an unrelated block.
         */
        @Override
        public void onMissileImpact(HitResult mop) {
        }
    }

    public static class EntityMissileMicro extends EntityMissileTier0 {
        public EntityMissileMicro(EntityType<? extends EntityMissileMicro> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            if (!level().isClientSide()) {
                ExplosionNukeSmall.explode(level(), getX(), getY() + 0.5, getZ(), ExplosionNukeSmall.PARAMS_HIGH);
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            // TODO(ModItems.ammo_standard / GunFactory.EnumAmmo.NUKE_HIGH, not yet ported).
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_MICRO.get());
        }
    }

    public static class EntityMissileSchrabidium extends EntityMissileTier0 {
        public EntityMissileSchrabidium(EntityType<? extends EntityMissileSchrabidium> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            if (!level().isClientSide()) {
                EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(level(), getX(), getY(), getZ(), BombConfig.ASCHRAB_RADIUS.get());
                if (!ex.isRemoved()) {
                    level().addFreshEntity(ex);
                    EntityCloudFleija cloud = EntityCloudFleija.create(level(), getX(), getY(), getZ(), 100);
                    level().addFreshEntity(cloud);
                }
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return null;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_SCHRABIDIUM.get());
        }
    }

    public static class EntityMissileBHole extends EntityMissileTier0 {
        public EntityMissileBHole(EntityType<? extends EntityMissileBHole> type, Level level) {
            super(type, level);
        }

        /**
         * CE: {@code world.createExplosion(this, x, y, z, 1.5F, true)} then spawns an
         * {@code EntityBlackHole(world, 1.5F)} at the same position - now wired, per
         * docs/phase4/entities_vortex_gravity_wells.md.
         */
        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 1.5F, true, Level.ExplosionInteraction.TNT);

            if (!level().isClientSide()) {
                EntityBlackHole blackHole = new EntityBlackHole(level(), 1.5F);
                blackHole.setPos(getX(), getY(), getZ());
                level().addFreshEntity(blackHole);
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ScatteredMilitaryItems.BLACK_HOLE.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_BHOLE.get());
        }
    }

    public static class EntityMissileTaint extends EntityMissileTier0 {
        public EntityMissileTaint(EntityType<? extends EntityMissileTaint> type, Level level) {
            super(type, level);
        }

        /** TODO({@code ModBlocks.taint}, not yet ported): CE seeds ~100 taint blocks in a 10x10x10 cube here. */
        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 10.0F, true, Level.ExplosionInteraction.TNT);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(BilletPowderItems.POWDER_SPARK_MIX.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_TAINT.get());
        }
    }

    public static class EntityMissileEMP extends EntityMissileTier0 {
        public EntityMissileEMP(EntityType<? extends EntityMissileEMP> type, Level level) {
            super(type, level);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            ExplosionNukeGeneric.empBlast(level(), getOwner(), (int) getX(), (int) getY(), (int) getZ(), 50);
            EntityEMPBlast wave = EntityEMPBlast.create(level(), getX(), getY(), getZ(), 50);
            level().addFreshEntity(wave);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(BombBlocks.EMP_BOMB.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(MissileItems.MISSILE_EMP.get());
        }
    }
}
