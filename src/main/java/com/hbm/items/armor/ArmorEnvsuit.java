package com.hbm.items.armor;

import com.hbm.main.MainRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorEnvsuit} (142 lines) - the Envsuit
 * environmental power-armor set. Beyond client-model/renderer plumbing (Phase 5), CE's non-
 * rendering behavior (chestplate-only, mirroring CE's own {@code this != ModItems.envsuit_plate ->
 * return} guard) is a +10% sprint-speed bonus while the full set is worn (ported with the same live
 * {@code AttributeInstance} pattern as {@link ArmorNCRPA}) and a water-breathing/night-vision
 * package while submerged.
 *
 * <p><b>Simplified relative to CE</b> (documented, not silently dropped): CE also nudges the
 * player along their look vector scaled by the raw {@code EntityPlayer#moveForward} input field
 * while swimming (an extra forward-propulsion boost on top of vanilla swim speed) - this port could
 * not confirm a 1.21.1 Mojang-mapped equivalent accessor for that raw per-tick forward-input value
 * on a generic {@link Player} in this pass, so that one motion nudge is left out; water-breathing,
 * the night-vision package, and the sprint-speed bonus (the mechanically load-bearing parts) are
 * all ported in full. The {@code ItemModNightVision} helmet-mod check that would otherwise keep
 * night vision after surfacing is stubbed to "always remove" (TODO below) since that item does not
 * exist in this port yet.
 */
public class ArmorEnvsuit extends ArmorFSBPowered {

    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "envsuit_sprint_speed");

    public ArmorEnvsuit(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                         long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        super.onArmorTick(level, player, stack);

        if (this.getType() != Type.CHESTPLATE) return;

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
            if (hasFSBArmor(player) && player.isSprinting()) {
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, 0.1, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        if (!hasFSBArmor(player)) return;

        if (player.isInWater()) {
            if (!level.isClientSide()) {
                player.setAirSupply(300);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0));
            }
            // TODO(unconfirmed 1.21.1 accessor): CE also nudges the player along their look vector
            // scaled by EntityPlayer#moveForward here - see class javadoc.
        } else {
            // TODO(ItemModNightVision not yet ported): CE keeps night vision active if the helmet's
            // mod slot 0 holds an ItemModNightVision; that item doesn't exist in this port yet, so
            // night vision is always removed on surfacing, matching CE's own fallback branch.
            if (!level.isClientSide()) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
