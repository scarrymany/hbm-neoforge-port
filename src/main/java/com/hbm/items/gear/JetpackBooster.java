package com.hbm.items.gear;

import com.hbm.capability.HbmPlayerAttachment;
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
 * Ported from CE's {@code com.hbm.items.gear.JetpackBooster} (104 lines) - {@code jetpack_boost},
 * the highest-tier jetpack: full look-vector-directional thrust (moves in whatever direction the
 * player looks, not just up) at the highest fuel tier ({@code Fluids.BALEFIRE}) and consumption of
 * the 4 fueled variants. Registered at {@code new JetpackBooster(properties, Fluids.BALEFIRE, 32000)}.
 * CE's {@code Vec3NT.createVectorHelper(...).length()} vector-magnitude helper is just
 * {@code Vec3#length()} on this port's own {@code player.getDeltaMovement()} - no port-side
 * equivalent needed.
 *
 * <p>Unlike {@link JetpackRegular}/{@link JetpackBreak}/{@link JetpackVectorized}, CE's own
 * {@code onArmorTick} for this class never calls {@code ArmorUtil.resetFlightTime} directly (no
 * such import in the CE source) - not a port omission; {@link com.hbm.items.armor.JetpackBase}'s
 * {@code modUpdate}/{@code inventoryTick} call it unconditionally after every {@code onArmorTick}
 * regardless of leaf, which is what covers this class.
 *
 * <p><b>Not ported</b> (documented TODO): CE's thruster particle packet - see
 * {@link JetpackRegular}'s identical note.
 */
public class JetpackBooster extends JetpackFueledBase {

    public JetpackBooster(Item.Properties properties, FluidType fuelType, int maxFuel) {
        super(properties, fuelType, maxFuel);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("High-powered vectorized jetpack."));
        components.add(Component.literal("Highly increased fuel consumption."));
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);

        // TODO(particle system): CE spawns a HbmEffectNT.Jetpack (mode 1) AuxParticlePacketNT here
        // while thrusting server-side - see class javadoc.

        if (getFuel(stack) <= 0 || !props.isJetpackActive()) return;

        if (player.getDeltaMovement().y < 0.6D) {
            player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.1D, player.getDeltaMovement().z);
        }

        Vec3 look = player.getLookAngle();
        if (player.getDeltaMovement().length() < 5D) {
            player.setDeltaMovement(player.getDeltaMovement().add(look.x * 0.25D, look.y * 0.25D, look.z * 0.25D));

            if (look.y > 0) {
                player.fallDistance = 0F;
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.flamethrowerShoot.get(), SoundSource.PLAYERS, 0.25F, 1.0F);
        this.useUpFuel(player, stack, 1);
    }
}
