package com.hbm.entity.projectile;

import com.hbm.blocks.generic.FalloutBlocks;
import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.capability.HbmLivingProps;
import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Playable subset of CE {@code EntityChemical} (571 lines) —
 * {@code @AutoRegister(name = "entity_chemthrower_splash", trackingRange = 1000)}.
 * Fluid sync + gas AoE {@code affect()} + fire/acid/rad. IRepairable extinguish / armor-degrade
 * extras that need unported TEs are skipped. CE file: {@code entity/projectile/EntityChemical.java}.
 */
public class EntityChemical extends EntityThrowableNT {

    private static final EntityDataAccessor<Integer> FLUID_TYPE =
            SynchedEntityData.defineId(EntityChemical.class, EntityDataSerializers.INT);

    public EntityChemical(EntityType<? extends EntityChemical> type, Level level) {
        super(type, level);
    }

    public EntityChemical(Level level, LivingEntity thrower) {
        super(Phase9TailEntityTypes.CHEMICAL.get(), level, thrower);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUID_TYPE, 0);
    }

    public EntityChemical setFluid(FluidType fluid) {
        this.entityData.set(FLUID_TYPE, fluid.getID());
        return this;
    }

    public FluidType getFluidType() {
        return Fluids.fromID(this.entityData.get(FLUID_TYPE));
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            if (this.tickCount > getMaxAge()) {
                this.discard();
                return;
            }
            FluidType type = getFluidType();
            if (type.hasTrait(Fluids.GASEOUS.getClass()) || type.hasTrait(Fluids.EVAP.getClass())) {
                double intensity = 1D - (double) this.tickCount / (double) getMaxAge();
                AABB box = this.getBoundingBox().inflate(intensity * 2.5);
                for (Entity e : this.level().getEntities(this, box)) {
                    if (e == this.getOwner()) continue;
                    if (e instanceof Player p && (p.isSpectator() || p.isCreative())) continue;
                    affect(e, intensity);
                }
            }
        }
        super.tick();
    }

    protected void affect(Entity e, double intensity) {
        if (e instanceof Player p && (p.isSpectator() || p.isCreative())) return;
        ChemicalStyle style = getStyle();
        FluidType type = getFluidType();
        LivingEntity living = e instanceof LivingEntity l ? l : null;

        switch (style) {
            case LIQUID, BURNING -> intensity = 1D;
            case AMAT -> {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(e, e.damageSources().source(ModDamageTypes.RADIATION), 1F);
                if (living != null) {
                    ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 50F * (float) intensity);
                }
                return;
            }
            case LIGHTNING -> {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(e, e.damageSources().source(ModDamageTypes.ELECTRICITY), 0.5F);
                if (living != null) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9));
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9));
                }
                return;
            }
            default -> {
            }
        }

        if (type.temperature >= 500) {
            e.igniteForSeconds(10);
        }
        if ((style == ChemicalStyle.LIQUID || style == ChemicalStyle.GAS) && type.hasTrait(Fluids.DELICIOUS.getClass()) && living != null && living.isAlive()) {
            living.heal(2F * (float) intensity);
        }
        if (style == ChemicalStyle.LIQUID && type.hasTrait(FT_Flammable.class) && living != null) {
            HbmLivingProps.setOil(living, 300);
        }
        if (isExtinguishing()) {
            e.clearFire();
        }
        if (style == ChemicalStyle.BURNING) {
            e.igniteForSeconds(5);
        }
        if (style == ChemicalStyle.GASFLAME) {
            e.igniteForSeconds((int) Math.ceil(5 * intensity));
        }
        if (type.hasTrait(FT_Corrosive.class) && living != null) {
            FT_Corrosive trait = type.getTrait(FT_Corrosive.class);
            EntityDamageUtil.attackEntityFromIgnoreIFrame(living, living.damageSources().source(ModDamageTypes.ACID), trait.getRating() / 50F);
            if (living instanceof Player player) {
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                    ArmorUtil.damageSuit(player, slot, trait.getRating() / 40);
                }
            }
        }
        if (type.hasTrait(FT_VentRadiation.class)) {
            FT_VentRadiation trait = type.getTrait(FT_VentRadiation.class);
            if (living != null) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, trait.getRadPerMB() * 5);
            }
            ChunkRadiationManager.proxy.incrementRad(this.level(), this.blockPosition(), trait.getRadPerMB() * 5);
        }
        if (type.hasTrait(FT_Poison.class) && living != null) {
            FT_Poison trait = type.getTrait(FT_Poison.class);
            living.addEffect(new MobEffectInstance(trait.isWithering() ? MobEffects.WITHER : MobEffects.POISON, (int) (5 * 20 * intensity)));
        }
        if (type.hasTrait(FT_Toxin.class) && living != null) {
            type.getTrait(FT_Toxin.class).affect(living, intensity);
        }
        if (type.hasTrait(FT_Pheromone.class) && living != null) {
            FT_Pheromone pheromone = type.getTrait(FT_Pheromone.class);
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 2));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5 * 60 * 20, 1));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2 * 60 * 20, 4));
            if (living instanceof EntityGlyphid && pheromone.getType() == 1) {
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 5 * 60 * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60 * 20, 0));
                living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 19));
            } else if (living instanceof Player && pheromone.getType() == 2) {
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 2));
            }
        }
        if (type == Fluids.XPJUICE && e instanceof Player player) {
            player.giveExperiencePoints(1);
            this.discard();
        }
        if (type == Fluids.ENDERJUICE && e instanceof LivingEntity livingEnder) {
            livingEnder.randomTeleport(
                    this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D,
                    this.getY() + this.random.nextInt(64) - 32,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D,
                    true);
        }
    }

    protected boolean isExtinguishing() {
        return getStyle() == ChemicalStyle.LIQUID && getFluidType().temperature < 50 && !getFluidType().hasTrait(FT_Flammable.class);
    }

    @Override
    protected void onImpact(HitResult result) {
        if (this.level().isClientSide) return;
        if (result instanceof EntityHitResult ehr) {
            affect(ehr.getEntity(), 1D);
            this.discard();
            return;
        }
        if (!(result instanceof BlockHitResult bhr)) return;
        BlockPos pos = bhr.getBlockPos();
        FluidType type = getFluidType();
        ChemicalStyle style = getStyle();
        if (style == ChemicalStyle.BURNING || style == ChemicalStyle.GASFLAME) {
            for (Direction dir : Direction.values()) {
                BlockPos off = pos.relative(dir);
                if (this.level().getBlockState(off).isAir()) {
                    this.level().setBlockAndUpdate(off, Blocks.FIRE.defaultBlockState());
                }
            }
        }
        if (isExtinguishing()) {
            for (Direction dir : Direction.values()) {
                BlockPos off = pos.relative(dir);
                if (this.level().getBlockState(off).is(Blocks.FIRE)) {
                    this.level().removeBlock(off, false);
                }
            }
        }
        if (type == Fluids.WATER || type == Fluids.HEAVYWATER || type == Fluids.COOLANT) {
            for (int i = -2; i <= 2; i++) {
                for (int j = 0; j <= 1; j++) {
                    for (int k = -2; k <= 2; k++) {
                        BlockPos check = pos.offset(i, j, k);
                        if (this.level().getBlockState(check).is(FalloutBlocks.FALLOUT.get())) {
                            this.level().removeBlock(check, false);
                        }
                    }
                }
            }
        }
        if (type == Fluids.SEEDSLURRY) {
            BlockState state = this.level().getBlockState(pos);
            if (state.is(Blocks.DIRT) || state.is(PlantBlocks.WASTE_EARTH.get())
                    || state.is(PlantBlocks.DIRT_DEAD.get()) || state.is(PlantBlocks.DIRT_OILY.get())) {
                this.level().setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
            }
            if (state.is(Blocks.COBBLESTONE)) {
                this.level().setBlockAndUpdate(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            }
        }
        this.discard();
    }

    @Override
    protected float getAirDrag() {
        return switch (getStyle()) {
            case AMAT, LIGHTNING -> 1F;
            case GAS -> 0.95F;
            default -> 0.99F;
        };
    }

    @Override
    protected float getWaterDrag() {
        return switch (getStyle()) {
            case AMAT, LIGHTNING, GAS -> 1F;
            default -> 0.8F;
        };
    }

    public int getMaxAge() {
        return switch (getStyle()) {
            case LIGHTNING -> 5;
            case GASFLAME -> 20;
            case GAS -> 60;
            case BURNING, LIQUID -> 600;
            default -> 100;
        };
    }

    @Override
    public double getGravityVelocity() {
        return switch (getStyle()) {
            case AMAT, LIGHTNING, GAS -> 0D;
            case GASFLAME -> -0.01D;
            default -> 0.03D;
        };
    }

    public ChemicalStyle getStyle() {
        return getStyleFromType(getFluidType());
    }

    public static ChemicalStyle getStyleFromType(FluidType type) {
        if (type == Fluids.IONGEL) return ChemicalStyle.LIGHTNING;
        if (type.isAntimatter()) return ChemicalStyle.AMAT;
        if (type.hasTrait(Fluids.GASEOUS.getClass()) || type.hasTrait(Fluids.EVAP.getClass())) {
            if (type.hasTrait(FT_Flammable.class) || type.hasTrait(FT_Combustible.class)) {
                return ChemicalStyle.GASFLAME;
            }
            return ChemicalStyle.GAS;
        }
        if (type.hasTrait(Fluids.LIQUID.getClass())) {
            return type.hasTrait(FT_Combustible.class) ? ChemicalStyle.BURNING : ChemicalStyle.LIQUID;
        }
        return ChemicalStyle.NULL;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("fluid", this.entityData.get(FLUID_TYPE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(FLUID_TYPE, tag.getInt("fluid"));
    }

    public enum ChemicalStyle {
        AMAT, LIGHTNING, LIQUID, GAS, GASFLAME, BURNING, NULL
    }
}
