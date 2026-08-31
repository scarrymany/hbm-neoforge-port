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
 * Ported from CE's {@code com.hbm.items.gear.JetpackRegular} (63 lines) - {@code jetpack_fly}, the
 * simplest jetpack: pure vertical thrust while the jetpack key is held and airborne, no hover, no
 * lateral vector. Registered at {@code new JetpackRegular(properties, Fluids.KEROSENE, 12000)},
 * confirmed against CE's own {@code ModItems} call site.
 *
 * <p>CE's {@code AuxParticlePacketNT}/{@code HbmEffectNT.Jetpack} thruster particle-trail packet is
 * wired via {@link com.hbm.particle.HbmEffect#JETPACK}, radius 100, matching CE's own call site 1:1
 * ({@code upstream/hbm-ce/.../JetpackRegular.java:47-49}). The physics below are ported in full.
 */
public class JetpackRegular extends JetpackFueledBase {

    public JetpackRegular(Item.Properties properties, FluidType fuelType, int maxFuel) {
        super(properties, fuelType, maxFuel);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("Regular jetpack for simple upwards momentum."));
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);

        if (getFuel(stack) > 0 && props.isJetpackActive()) {
            CompoundTag data = new CompoundTag();
            data.putInt("player", player.getId());
            HbmEffect.sendPacket(level, HbmEffect.JETPACK, player.getX(), player.getY(), player.getZ(), 100, data);

            player.fallDistance = 0F;

            if (player.getDeltaMovement().y < 0.4D) {
                player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.1D, player.getDeltaMovement().z);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.flamethrowerShoot.get(), SoundSource.PLAYERS, 0.25F, 1.5F);
            this.useUpFuel(player, stack, 5);
            ArmorUtil.resetFlightTime(player);
        }
    }
}
