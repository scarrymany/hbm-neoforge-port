package com.hbm.items.gear;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.gear.JetpackVectorized} (82 lines) - {@code jetpack_vector},
 * a mid-tier look-vector-directional jetpack: same idea as {@link JetpackBooster} but capped to a
 * much lower speed threshold, at a mid-tier fuel rate between {@link JetpackRegular} and
 * {@link JetpackBooster}. Registered at {@code new JetpackVectorized(properties, Fluids.KEROSENE, 16000)}.
 *
 * <p>{@code props.getKeyPressed(EnumKeybind.JETPACK) && props.isJetpackActive()} is CE's own
 * redundant double-check ({@link HbmPlayerAttachment#isJetpackActive()} already implies the key is
 * down) - kept exactly as CE wrote it, not "fixed", matching
 * {@code docs/phase3/fsb_armor_and_jetpacks.md}'s own read of this class.
 *
 * <p><b>Not ported</b> (documented TODO): CE's thruster particle packet - see
 * {@link JetpackRegular}'s identical note.
 */
public class JetpackVectorized extends JetpackFueledBase {

    public JetpackVectorized(Item.Properties properties, FluidType fuelType, int maxFuel) {
        super(properties, fuelType, maxFuel);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("High-mobility jetpack."));
        components.add(Component.literal("Higher fuel consumption."));
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);

        // TODO(particle system): CE spawns a HbmEffectNT.Jetpack (mode 1) AuxParticlePacketNT here
        // while thrusting server-side - see class javadoc.

        if (getFuel(stack) <= 0 || !props.getKeyPressed(EnumKeybind.JETPACK) || !props.isJetpackActive()) return;

        if (player.getDeltaMovement().y < 0.4D) {
            player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.1D, player.getDeltaMovement().z);
        }

        Vec3 look = player.getLookAngle();
        if (player.getDeltaMovement().length() < 2D) {
            player.setDeltaMovement(player.getDeltaMovement().add(look.x * 0.1D, look.y * 0.1D, look.z * 0.1D));

            if (look.y > 0) {
                player.fallDistance = 0F;
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.flamethrowerShoot.get(), SoundSource.PLAYERS, 0.25F, 1.5F);
        this.useUpFuel(player, stack, 3);
        ArmorUtil.resetFlightTime(player);
    }
}
