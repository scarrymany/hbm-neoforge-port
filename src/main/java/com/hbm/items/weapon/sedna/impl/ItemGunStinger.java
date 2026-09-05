package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunDataComponents;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Exact CE {@code ItemGunStinger} lock-on ({@code :36-76}, {@code getLockonTarget :87-124}).
 * HUD {@code renderStingerLockon} / client {@code prevLockon} bar stay skipped.
 */
public class ItemGunStinger extends ItemGunBaseNT {

    public ItemGunStinger(Item.Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties, quality, cfg);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (entity instanceof Player player) {
            if (!level.isClientSide && !isSelected && getIsLockingOn(stack)) {
                setIsLockingOn(stack, false);
            }

            if (!level.isClientSide) {
                int prevTarget = getLockonTarget(stack);
                if (isSelected && getIsLockingOn(stack) && getIsAiming(stack)
                        && getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, player.getInventory()) > 0) {
                    int newLockonTarget = getLockonTarget(player, 150D, 10D);

                    if (newLockonTarget == -1) {
                        if (!getIsLockedOn(stack)) resetLockon(level, stack);
                    } else {
                        if (!getIsLockedOn(stack) && newLockonTarget != prevTarget) {
                            resetLockon(level, stack);
                            setLockonTarget(stack, newLockonTarget);
                        }
                        progressLockon(level, stack);

                        if (getLockonProgress(stack) >= 60 && !getIsLockedOn(stack)) {
                            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                                    HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1F, 1F);
                            setIsLockedOn(stack, true);
                        }
                    }
                } else {
                    resetLockon(level, stack);
                }
            }
        }
    }

    public void resetLockon(Level level, ItemStack stack) {
        setLockonProgress(stack, 0);
        setIsLockedOn(stack, false);
    }

    public void progressLockon(Level level, ItemStack stack) {
        setLockonProgress(stack, getLockonProgress(stack) + 1);
    }

    /** Exact CE {@code ItemGunStinger.java:87-124}. {@code Vec3NT} frustum via vanilla {@link Vec3}. */
    public static int getLockonTarget(Player player, double distance, double angleThreshold) {
        if (player == null) return -1;

        double x = player.getX();
        double y = player.getY() + player.getEyeHeight();
        double z = player.getZ();

        Vec3 lookDir = player.getViewVector(1F).scale(distance);
        Vec3 look = lookDir.add(x, y, z);
        Vec3 left = rotateAroundYDeg(lookDir.add(x, y, z), -angleThreshold).add(0, 10, 0);
        Vec3 right = rotateAroundYDeg(lookDir.add(x, y, z), angleThreshold).add(0, -10, 0);
        Vec3 pos = new Vec3(x, y, z);

        AABB aabb = new AABB(
                min4(look.x, left.x, right.x, pos.x), min4(look.y, left.y, right.y, pos.y), min4(look.z, left.z, right.z, pos.z),
                max4(look.x, left.x, right.x, pos.x), max4(look.y, left.y, right.y, pos.y), max4(look.z, left.z, right.z, pos.z));
        List<Entity> entities = player.level().getEntities(player, aabb);
        Entity closestEntity = null;
        double closestAngle = 360D;

        for (Entity entity : entities) {
            if (entity.getBbHeight() < 0.5F || !entity.isPickable()) continue;
            Vec3 toEntity = new Vec3(entity.getX() - x, entity.getY() + entity.getBbHeight() / 2D - y, entity.getZ() - z);

            double vecProd = toEntity.x * lookDir.x + toEntity.y * lookDir.y + toEntity.z * lookDir.z;
            double bot = toEntity.length() * lookDir.length();
            double angle = Math.abs(Math.acos(vecProd / bot) * 180 / Math.PI);

            if (angle < closestAngle && angle < angleThreshold) {
                closestAngle = angle;
                closestEntity = entity;
            }
        }

        return closestEntity == null ? -1 : closestEntity.getId();
    }

    /** CE {@code Vec3NT.rotateAroundYDeg} → {@code rotateAroundYRad} ({@code :363-367}, {@code :384-385}). */
    private static Vec3 rotateAroundYDeg(Vec3 v, double deg) {
        double a = Math.toRadians(deg);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
    }

    private static double min4(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static double max4(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    public static boolean getIsLockingOn(ItemStack stack) {
        return stack.getOrDefault(GunDataComponents.LOCKING_ON.get(), false);
    }

    public static void setIsLockingOn(ItemStack stack, boolean value) {
        stack.set(GunDataComponents.LOCKING_ON.get(), value);
    }

    public static int getLockonProgress(ItemStack stack) {
        return stack.getOrDefault(GunDataComponents.LOCKON_PROGRESS.get(), 0);
    }

    public static void setLockonProgress(ItemStack stack, int value) {
        stack.set(GunDataComponents.LOCKON_PROGRESS.get(), value);
    }
}
