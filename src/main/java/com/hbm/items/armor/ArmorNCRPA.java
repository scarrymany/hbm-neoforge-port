package com.hbm.items.armor;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.items.gear.ArmorFSB;
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
 * Ported from CE's {@code com.hbm.items.armor.ArmorNCRPA} (114 lines) - the NCR power-armor set.
 * Beyond client-model/renderer plumbing (Phase 5), CE's real body has two per-tick mechanics
 * (dispatched from the chestplate piece only, mirroring CE's own {@code this != ModItems.ncrpa_plate
 * -> return} guard) and the {@link IPAWeaponsProvider} wiring:
 * <ul>
 *     <li>a live +0.1 movement-speed bonus while sprinting, recomputed every tick via
 *     {@code AttributeInstance#addTransientModifier}/{@code #removeModifier} - the same live-modifier
 *     pattern already established by {@code com.hbm.items.weapon.ItemCrucible} for exactly this
 *     "CE recomputed this per-query, 1.21's component is static" problem;</li>
 *     <li>a refreshed 15-second Night Vision pulse every 20 ticks while the full set is worn, gated
 *     on the player's HUD toggle (CE: {@code HbmCapability.getData(player).getEnableHUD()} - this
 *     port's {@link HbmPlayerAttachment#getEnableHUD()}).</li>
 * </ul>
 */
public class ArmorNCRPA extends ArmorFSBPowered implements IPAWeaponsProvider {

    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "ncrpa_sprint_speed");

    private static final ArmorNCRPAMelee MELEE_COMPONENT = new ArmorNCRPAMelee();
    private static final ArmorNCRPARanged RANGED_COMPONENT = new ArmorNCRPARanged();

    public ArmorNCRPA(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
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
            if (player.isSprinting()) {
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, 0.1, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        if (ArmorFSB.hasFSBArmor(player)) {
            if (level.getGameTime() % 20 != 0) return;
            if (HbmPlayerAttachment.getData(player).getEnableHUD()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
            }
        }
    }

    @Override
    public IPAMelee getMeleeComponent(Player entity) {
        return hasFSBArmorIgnoreCharge(entity) ? MELEE_COMPONENT : null;
    }

    @Override
    public IPARanged getRangedComponent(Player entity) {
        return hasFSBArmorIgnoreCharge(entity) ? RANGED_COMPONENT : null;
    }
}
