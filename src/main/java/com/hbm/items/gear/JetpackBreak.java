package com.hbm.items.gear;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.ArmorUtil;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.HbmEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.gear.JetpackBreak} (84 lines) - {@code jetpack_break}. Adds
 * a passive glide/auto-hover mode on top of {@link JetpackRegular}'s active thrust: even with the
 * jetpack key not held, falling while airborne, not sneaking, and with the backpack toggle enabled
 * ({@link HbmPlayerAttachment#getEnableBackpack()}) triggers a gentler tiered fall-speed clamp, at a
 * cheaper fuel rate than active thrust. Same passive-glide idiom already established by
 * {@code com.hbm.items.armor.ArmorDNT}'s own glide branch (read for the confirmed
 * {@code player.onGround()}/{@code getEnableBackpack()} accessor shapes). Registered at
 * {@code new JetpackBreak(properties, Fluids.KEROSENE, 12000)}.
 *
 * <p>CE's thruster particle packet ({@link com.hbm.particle.HbmEffect#JETPACK}) is wired, gated on
 * the same thrusting-or-gliding condition as the physics below
 * ({@code upstream/hbm-ce/.../JetpackBreak.java:46-51}) - see {@link JetpackRegular}'s identical note.
 */
public class JetpackBreak extends JetpackFueledBase {

    public JetpackBreak(Item.Properties properties, FluidType fuelType, int maxFuel) {
        super(properties, fuelType, maxFuel);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("Regular jetpack that will automatically hover mid-air."));
        components.add(Component.literal("Sneaking will stop hover mode."));
        components.add(Component.literal("Hover mode will consume less fuel and increase air-mobility."));
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);

        if (getFuel(stack) <= 0) return;

        if (props.isJetpackActive() || (!player.onGround() && !player.isShiftKeyDown() && props.getEnableBackpack())) {
            CompoundTag data = new CompoundTag();
            data.putInt("player", player.getId());
            HbmEffect.sendPacket(level, HbmEffect.JETPACK, player.getX(), player.getY(), player.getZ(), 100, data);
        }

        if (props.isJetpackActive()) {
            player.fallDistance = 0F;

            if (player.getDeltaMovement().y < 0.4D) {
                player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.1D, player.getDeltaMovement().z);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.flamethrowerShoot.get(), SoundSource.PLAYERS, 0.25F, 1.5F);
            this.useUpFuel(player, stack, 5);

        } else if (!player.isShiftKeyDown() && !player.onGround() && props.getEnableBackpack()) {
            player.fallDistance = 0F;

            double vy = player.getDeltaMovement().y;
            if (vy < -1D) {
                player.setDeltaMovement(player.getDeltaMovement().x, vy + 0.2D, player.getDeltaMovement().z);
            } else if (vy < -0.1D) {
                player.setDeltaMovement(player.getDeltaMovement().x, vy + 0.1D, player.getDeltaMovement().z);
            } else if (vy < 0D) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0D, player.getDeltaMovement().z);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.flamethrowerShoot.get(), SoundSource.PLAYERS, 0.25F, 1.5F);
            this.useUpFuel(player, stack, 10);
        }

        ArmorUtil.resetFlightTime(player);
    }
}
