package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorEnvsuit} (142 lines) - the Envsuit
 * environmental power-armor set. Beyond client-model/renderer plumbing (Phase 5), CE's non-
 * rendering behavior (chestplate-only, mirroring CE's own {@code this != ModItems.envsuit_plate ->
 * return} guard) is a +10% sprint-speed bonus while the full set is worn (ported with the same live
 * {@code AttributeInstance} pattern as {@link ArmorNCRPA}) and a water-breathing/night-vision
 * package while submerged. Swim look-nudge is {@code player.zza} (CE {@code moveForward});
 * surfaced NV stays if helmet-only holds {@link ItemModNightVision}.
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
            double mo = 0.1 * player.zza;
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(player.getDeltaMovement().add(look.x * mo, look.y * mo, look.z * mo));
        } else {
            boolean canRemoveNightVision = true;
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack helmetMod = ArmorModHandler.pryMod(helmet, ArmorModHandler.helmet_only);
            if (!helmetMod.isEmpty() && helmetMod.getItem() instanceof ItemModNightVision) {
                canRemoveNightVision = false;
            }

            if (!level.isClientSide() && canRemoveNightVision) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
