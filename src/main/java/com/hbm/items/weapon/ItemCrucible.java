package com.hbm.items.weapon;

import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.weapon.ItemCrucible} - a charge-up "blender" melee weapon:
 * full attack damage/movement speed while charged, a weak flat-damage fallback once discharged,
 * recharged externally (CE: a crafting-station recipe, out of this class's scope either way).
 * <p>
 * <b>Charge-dependent attack damage/movement, ported with a deliberate mechanism change (not a
 * behavior change):</b> CE recomputes {@code getAttributeModifiers} fresh every time the game asks
 * for it (1.12's per-query API), so the charged/uncharged switch is always perfectly live. 1.21's
 * {@link net.minecraft.world.item.component.ItemAttributeModifiers} is a static data component
 * baked once (at {@link com.hbm.items.tool.ItemSwordAbility#createAttributes} construction time
 * here) - mutating that component's value on an already-equipped stack does not get live-rediffed
 * by the game the way CE's per-query call did. This class instead manages the charge-dependent
 * *delta* directly on the wielder's own {@link AttributeInstance}s (the same
 * {@code addTransientModifier}/{@code removeModifier} pattern already established by
 * {@code com.hbm.capability.HbmLivingProps#setDigamma} for the same "recompute a live modifier
 * outside the static item-component system" problem), refreshed every tick this stack is the
 * wielder's held mainhand item via {@link #inventoryTick}. {@code addTransientModifier} (not
 * {@code addPermanentModifier}) is used deliberately: this modifier is fully recomputed every tick
 * from the stack's current charge count and torn down the moment the item is no longer the equipped
 * mainhand stack, so there is nothing here that needs to survive a save/reload the way the
 * permanently-tracked digamma exposure does.
 * <p>
 * Not ported (documented, not silently dropped):
 * <ul>
 *     <li>The on-kill-while-charged "blender" particle burst ({@code HbmEffectNT.VanillaBurst_BlockDust})
 *     - purely cosmetic VFX riding CE's unported generic effect-dispatch table (see
 *     {@code docs/phase3/weapon_animation_hooks.md}); the discharge-on-hit gameplay itself still
 *     works without it.</li>
 *     <li>The {@code doSpecialClick} client-only alternate-attack toggle and its first-person
 *     lightning-particle GUI feedback ({@code updateClient}, {@code ParticleCrucibleLightning}) -
 *     Phase 5 rendering scope; this port's crucible always follows CE's "not doSpecialClick"
 *     (default/common) code path, which is the one that actually swings and discharges.</li>
 *     <li>Defaulting a freshly-crafted stack to max charge in the creative tab ({@code getSubItems})
 *     - creative-tab population for this item is whoever registers it, not this class; a fresh
 *     stack simply starts at 0 charges (discharged) until charged, matching this stack's declared
 *     component default.</li>
 * </ul>
 */
public class ItemCrucible extends ItemSwordCutter {

    /**
     * CE: {@code GeneralConfig.crucibleMaxCharges} (config key {@code "1.33_crucible_max_charges"},
     * default {@code 16}). This port has no {@code GeneralConfig} equivalent for this value yet
     * (adding it is a one-line {@code HbmConfig.java} change - see this package's wiring notes);
     * hardcoded to CE's own default so the item is fully functional today.
     */
    public static final int MAX_CHARGES = 16;

    private static final ResourceLocation CHARGE_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "crucible_charge_damage");
    private static final ResourceLocation CHARGE_MOVEMENT_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "crucible_charge_movement");

    public ItemCrucible(float damage, double movement, Tier tier, Properties properties) {
        super(damage, movement, tier, properties);
    }

    public static int getCharges(ItemStack stack) {
        return stack.getOrDefault(WeaponDataComponents.CRUCIBLE_CHARGES.get(), 0);
    }

    public static ItemStack charge(ItemStack stack) {
        stack.set(WeaponDataComponents.CRUCIBLE_CHARGES.get(), MAX_CHARGES);
        return stack;
    }

    public static void discharge(ItemStack stack) {
        stack.set(WeaponDataComponents.CRUCIBLE_CHARGES.get(), Math.max(0, getCharges(stack) - 1));
    }

    @Override
    public byte getTexId() {
        return 1;
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (getCharges(stack) == 0) {
            return;
        }
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.cDeploy.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        super.onEntitySwing(stack, entity);
        if (getCharges(stack) > 0) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), HBMSoundHandler.crucibleSwing.get(), SoundSource.PLAYERS, 1F, 1F);
        }
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        discharge(stack);
        // CE's on-kill "blender" particle burst (HbmEffectNT-based) is deferred - see class javadoc.
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }

        boolean mainhand = living.getMainHandItem() == stack;
        if (isSelected && mainhand) {
            applyChargeAttributes(living, stack);
        } else {
            clearChargeAttributes(living);
        }
    }

    /** See class javadoc's "Charge-dependent attack damage/movement" note. */
    private void applyChargeAttributes(LivingEntity attacker, ItemStack stack) {
        AttributeInstance damageAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speedAttr = attacker.getAttribute(Attributes.MOVEMENT_SPEED);
        if (damageAttr == null || speedAttr == null) {
            return;
        }

        boolean charged = getCharges(stack) > 0;

        // CE: charged ? this.damage : 5 (flat). this.damage is already contributed by the base
        // ItemSwordAbility attributes baked into this stack's component at construction; this
        // modifier only carries the *delta* needed to land on CE's exact charged/uncharged totals.
        double damageDelta = (charged ? this.damage : 5.0) - this.damage;
        // CE: charged ? movement : movement * 0.8, both ADD_MULTIPLIED_BASE (CE operation 1) - the
        // base modifier already contributes `movement`, so the uncharged delta is -0.2 * movement.
        double movementDelta = (charged ? this.movement : this.movement * 0.8) - this.movement;

        damageAttr.removeModifier(CHARGE_DAMAGE_ID);
        speedAttr.removeModifier(CHARGE_MOVEMENT_ID);
        damageAttr.addTransientModifier(new AttributeModifier(CHARGE_DAMAGE_ID, damageDelta, AttributeModifier.Operation.ADD_VALUE));
        speedAttr.addTransientModifier(new AttributeModifier(CHARGE_MOVEMENT_ID, movementDelta, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private void clearChargeAttributes(LivingEntity attacker) {
        AttributeInstance damageAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speedAttr = attacker.getAttribute(Attributes.MOVEMENT_SPEED);
        if (damageAttr != null) damageAttr.removeModifier(CHARGE_DAMAGE_ID);
        if (speedAttr != null) speedAttr.removeModifier(CHARGE_MOVEMENT_ID);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharges(stack) / MAX_CHARGES);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int charges = getCharges(stack);
        StringBuilder charge = new StringBuilder("Charge [");
        for (int i = 0; i < MAX_CHARGES; i++) {
            charge.append(charges > i ? "||||||" : "   ");
        }
        charge.append("]");
        tooltipComponents.add(Component.literal(charge.toString()).withStyle(ChatFormatting.RED));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
