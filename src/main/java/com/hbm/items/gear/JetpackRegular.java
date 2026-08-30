package com.hbm.items.gear;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.ArmorUtil;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.lib.HBMSoundHandler;
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
 * <p><b>Not ported</b> (documented TODO, per {@code docs/phase3/fsb_armor_and_jetpacks.md} Deferred
 * scope): CE's {@code AuxParticlePacketNT}/{@code HbmEffectNT.Jetpack} thruster particle-trail
 * packet - no particle-packet system exists yet in this port. Purely cosmetic; the physics below are
 * ported in full.
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

        // TODO(particle system): CE spawns a HbmEffectNT.Jetpack AuxParticlePacketNT here while
        // thrusting server-side - see class javadoc.

        if (getFuel(stack) > 0 && props.isJetpackActive()) {
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
