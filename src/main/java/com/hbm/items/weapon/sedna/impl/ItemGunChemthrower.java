package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Exact CE {@code ItemGunChemthrower}: {@code IFillableItem} ({@code :29-70}) plus
 * {@code LAMBDA_CAN_FIRE}/{@code LAMBDA_FIRE} ({@code :77-99}) spawning registered
 * {@link EntityChemical}. Orchestra / smoke skipped.
 */
public class ItemGunChemthrower extends ItemGunBaseNT implements IFillableItem {

    public static final int CONSUMPTION = 3;

    public ItemGunChemthrower(Item.Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties, quality, cfg);
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return getFluidType(stack) == type || getMagCount(stack) == 0;
    }

    public static final int transferSpeed = 50;

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;
        if (getMagCount(stack) == 0) setMagType(stack, type.getID());

        int fill = getMagCount(stack);
        int req = this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getCapacity(stack) - fill;
        int toFill = Math.min(amount, req);
        toFill = Math.min(toFill, transferSpeed);
        setMagCount(stack, fill + toFill);

        return amount - toFill;
    }

    public FluidType getFluidType(ItemStack stack) {
        return Fluids.fromID(getMagType(stack));
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return getFluidType(stack) == type;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        int fill = getMagCount(stack);
        int toUnload = Math.min(fill, amount);
        toUnload = Math.min(toUnload, transferSpeed);
        setMagCount(stack, fill - toUnload);
        return toUnload;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return Fluids.fromID(getMagType(stack));
    }

    @Override
    public int getFill(ItemStack stack) {
        return getMagCount(stack);
    }

    /** CE {@code KEY_MAG_TYPE + 0} via mag-state type (decimal fluid id). Absent ≡ {@code 0}. */
    public static int getMagType(ItemStack stack) {
        String type = IMagazine.magState(stack, 0).type();
        if (type.isEmpty()) return 0;
        try {
            return Integer.parseInt(type);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void setMagType(ItemStack stack, int value) {
        IMagazine.updateMagState(stack, 0, s -> s.withType(String.valueOf(value)));
    }

    public static int getMagCount(ItemStack stack) {
        return IMagazine.magState(stack, 0).amount();
    }

    public static void setMagCount(ItemStack stack, int value) {
        IMagazine.updateMagState(stack, 0, s -> s.withAmount(value));
    }

    public static final BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE =
            (stack, ctx) -> ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) >= CONSUMPTION;

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, ctx) -> {
        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.CYCLE, ctx.configIndex);

        Receiver primary = ctx.config.getReceivers(stack)[0];
        IMagazine<?> mag = primary.getMagazine(stack);

        Vec3 offset = primary.getProjectileOffset(stack);
        double forwardOffset = offset.x;
        double heightOffset = offset.y;
        double sideOffset = offset.z;

        EntityChemical chem = new EntityChemical(entity.level(), entity, sideOffset, heightOffset, forwardOffset);
        chem.setFluid(Fluids.fromID(getMagType(stack)));
        entity.level().addFreshEntity(chem);

        mag.useUpAmmo(stack, ctx.inventory, CONSUMPTION);
        ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + 1F, ctx.config.getDurability(stack)));
    };
}
