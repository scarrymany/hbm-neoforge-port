package com.hbm.items.weapon.sedna.impl;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunDataComponents;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.content.XFactoryTool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Exact CE {@code ItemGunChargeThrower} reel-in ({@code :25-69}). {@code KEY_LASTHOOK} is
 * {@link GunDataComponents#LAST_HOOK}. Client orchestra rope VFX stays skipped.
 */
public class ItemGunChargeThrower extends ItemGunBaseNT {

    public ItemGunChargeThrower(Item.Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties, quality, cfg);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (getState(stack, 0) == GunState.RELOADING) {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }

        if (isSelected && entity instanceof Player player) {
            Entity e = level.getEntity(getLastHook(stack));
            HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);
            if (e != null && e.isAlive() && e instanceof EntityBulletBaseMK4 bullet
                    && bullet.config == XFactoryTool.ct_hook && bullet.velocity < 0.01) {
                Vec3 vec = new Vec3(e.getX() - player.getX(), e.getY() - player.getY() - player.getEyeHeight(), e.getZ() - player.getZ());
                double line = vec.length();
                if (props.getKeyPressed(EnumKeybind.GUN_PRIMARY)) {
                    Vec3 pull = vec.normalize().scale(0.1);
                    Vec3 mot = player.getDeltaMovement();
                    player.setDeltaMovement(mot.x + pull.x, mot.y + pull.y + 0.04, mot.z + pull.z);
                    if (!level.isClientSide && line < 2) e.discard();
                } else if (!props.getKeyPressed(EnumKeybind.GUN_SECONDARY)) {
                    Vec3 mot = player.getDeltaMovement();
                    Vec3 nextPos = new Vec3(player.getX() + mot.x, player.getY() + player.getEyeHeight() + mot.y, player.getZ() + mot.z);
                    Vec3 delta = new Vec3(e.getX() - nextPos.x, e.getY() - nextPos.y, e.getZ() - nextPos.z);
                    if (delta.length() > line) {
                        delta = delta.normalize().scale(line);
                        Vec3 newNext = new Vec3(e.getX() - delta.x, e.getY() - delta.y, e.getZ() - delta.z);
                        Vec3 vel = new Vec3(newNext.x - player.getX(), newNext.y - player.getY() - player.getEyeHeight(), newNext.z - player.getZ());
                        if (vel.length() < 3) {
                            player.setDeltaMovement(vel);
                        }
                    }
                } else {
                    player.setDeltaMovement(player.getDeltaMovement().scale(0.5));
                }

                if (player.getDeltaMovement().y > -0.1) player.fallDistance = 0;
                ArmorUtil.resetFlightTime(player);
            }
        } else {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }
    }

    public static int getLastHook(ItemStack stack) {
        return stack.getOrDefault(GunDataComponents.LAST_HOOK.get(), 0);
    }

    public static void setLastHook(ItemStack stack, int value) {
        stack.set(GunDataComponents.LAST_HOOK.get(), value);
    }
}
