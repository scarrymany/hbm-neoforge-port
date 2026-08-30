package com.hbm.items.armor;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorDNT} (205 lines) - the "DNS" (Deep Null Suit)
 * top-tier power-armor set. Beyond client-model/renderer plumbing (Phase 5), CE's non-rendering
 * behavior, all ported:
 * <ul>
 *     <li>chestplate-only (CE: {@code this != ModItems.dns_plate -> return}) +0.25 flat
 *     sprint-speed bonus, same live {@link AttributeInstance} pattern as {@link ArmorNCRPA}/
 *     {@link ArmorEnvsuit};</li>
 *     <li>chestplate-only jetpack hover/glide flight (same shape as {@link ArmorBJJetpack}, scaled
 *     up: faster vertical ramp, plus a passive backpack-glide mode when airborne and not sneaking
 *     while the player's backpack toggle is enabled);</li>
 *     <li>near-total damage/knockback immunity while the full set is worn: attacks are cancelled
 *     outright (except explosions, which still knock back) and incoming damage is zeroed (except
 *     explosions, reduced to 0.1%) - CE: {@code handleAttack}/{@code handleHurt}.</li>
 * </ul>
 * <b>Not ported</b> (documented TODO): the passive-glide branch's extra forward-look-vector nudge
 * scaled by the raw {@code EntityPlayer#moveForward} input - same unconfirmed-accessor gap as
 * {@link ArmorEnvsuit} (see its javadoc); the rest of the glide branch (fall-speed clamp, drag,
 * thruster sound) is ported in full. CE's {@code AuxParticlePacketNT}/{@code HbmEffectNT.
 * Jetpack_DNS} particle trail is likewise stubbed (no particle-packet system yet).
 */
public class ArmorDNT extends ArmorFSBPowered {

    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "dns_sprint_speed");

    public ArmorDNT(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                     long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        super.onArmorTick(level, player, stack);

        if (this.getType() != Type.CHESTPLATE) return;

        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
            if (player.isSprinting()) {
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, 0.25, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        boolean gliding = !player.onGround() && !player.isShiftKeyDown() && props.getEnableBackpack();

        // TODO(particle system): CE spawns a HbmEffectNT.Jetpack_DNS AuxParticlePacketNT here
        // server-side while jetting/gliding - see class javadoc.

        if (!ArmorFSB.hasFSBArmor(player)) return;

        ArmorUtil.resetFlightTime(player);

        if (props.isJetpackActive()) {

            if (player.getDeltaMovement().y < 0.6D) {
                player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.2D, player.getDeltaMovement().z);
            }
            player.fallDistance = 0F;

            if (level.getGameTime() % 4 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.immolatorShoot.get(), SoundSource.PLAYERS, 0.125F, 1.5F);
            }

        } else if (gliding) {
            player.fallDistance = 0F;

            double vy = player.getDeltaMovement().y;
            if (vy < -1D) vy += 0.4D;
            else if (vy < -0.1D) vy += 0.2D;
            else if (vy < 0D) vy = 0D;

            player.setDeltaMovement(player.getDeltaMovement().x * 1.05D, vy, player.getDeltaMovement().z * 1.05D);

            // TODO(unconfirmed 1.21.1 accessor): CE also adds lookVec * 0.25 * moveForward here -
            // see class javadoc.

            if (level.getGameTime() % 4 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.immolatorShoot.get(), SoundSource.PLAYERS, 0.125F, 1.5F);
            }
        }

        if (player.isShiftKeyDown() && !player.onGround()) {
            player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y - 0.1D, player.getDeltaMovement().z);
        }
    }

    @Override
    public void handleAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player && ArmorFSB.hasFSBArmor(player)) {
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) return;
            SoundEvent breakSound = SoundEvents.ITEM_BREAK.value();
            HbmPlayerAttachment.plink(player, breakSound, 5F, 1.0F + entity.getRandom().nextFloat() * 0.5F);
            event.setCanceled(true);
        }
    }

    @Override
    public void handleHurt(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && ArmorFSB.hasFSBArmor(player)) {
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                event.setNewDamage(event.getNewDamage() * 0.001F);
                return;
            }
            event.setNewDamage(0F);
        }
    }
}
