package com.hbm.items.armor;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorBJJetpack} (100 lines) - the jetpack-equipped
 * Blackjack chestplate variant ({@code bj_plate_jetpack}). Extends {@link ArmorBJ} so the helmet
 * failsafe still applies to a full set built around this chestplate. CE's per-tick flight logic,
 * all ported using already-confirmed-real APIs ({@link HbmPlayerAttachment#isJetpackActive()},
 * {@link ArmorUtil#resetFlightTime}):
 * <ul>
 *     <li>hover thrust while the jetpack key is held (clamped vertical speed ramp-up, fall reset,
 *     looping thruster sound);</li>
 *     <li>a sneak-to-brake glide (damps downward fall speed toward zero while sneaking and not
 *     thrusting).</li>
 * </ul>
 * <b>Not ported</b> (documented TODO): CE's {@code AuxParticlePacketNT}/{@code HbmEffectNT.
 * Jetpack_BJ} thruster particle-trail packet - this port has no confirmed particle-packet system
 * yet (see {@code docs/phase3/armor_equippable_framework.md} Open questions #6); purely cosmetic,
 * no gameplay effect lost.
 */
public class ArmorBJJetpack extends ArmorBJ {

    public ArmorBJJetpack(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                           long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        super.onArmorTick(level, player, stack);

        if (!ArmorFSB.hasFSBArmor(player)) return;

        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);

        // TODO(particle system): CE spawns a HbmEffectNT.Jetpack_BJ AuxParticlePacketNT here while
        // thrusting server-side - see class javadoc.

        ArmorUtil.resetFlightTime(player);

        if (props.isJetpackActive()) {

            if (player.getDeltaMovement().y < 0.4D) {
                player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.1D, player.getDeltaMovement().z);
            }
            player.fallDistance = 0F;

            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.immolatorShoot.get(), SoundSource.PLAYERS, 0.125F, 1.5F);

        } else if (player.isShiftKeyDown()) {

            if (player.getDeltaMovement().y < -0.08D) {
                double mo = player.getDeltaMovement().y * -0.4D;
                double lx = player.getLookAngle().x * mo;
                double lz = player.getLookAngle().z * mo;
                player.setDeltaMovement(player.getDeltaMovement().x + lx, player.getDeltaMovement().y + mo, player.getDeltaMovement().z + lz);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        components.add(Component.literal("§c  + " + I18nUtil.resolveKey("armor.electricJetpack")));
        components.add(Component.literal("§7  + " + I18nUtil.resolveKey("armor.glider")));
    }
}
