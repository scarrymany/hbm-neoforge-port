package com.hbm.items.gear;

import com.hbm.handler.ArmorUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.items.gear.ArmorEuphemium} (live in current CE - see
 * {@code docs/phase3/armor_special_sets.md} Headline finding #1) - 4 items
 * ({@code euphemium_helmet/_plate/_legs/_boots}, {@link com.hbm.items.gear.SpecialArmorItems}),
 * full-set-bonus damage immunity gated on wearing all 4 exact items.
 *
 * <p><b>Damage immunity</b>: CE's {@code ISpecialArmor#getProperties} (full absorption,
 * {@code EntityPlayer}-only, full-set-gated) has no 1.21.1 successor interface - the confirmed
 * real replacement is a central {@code LivingIncomingDamageEvent} listener (see
 * {@code com.hbm.handler.ArmorDamageHandler#onIncomingDamage}, which dispatches to
 * {@link #isFullSetWorn} here rather than each armor set inventing its own listener). {@link #isFullSetWorn}
 * restores CE's exact gate: {@code ArmorUtil.checkArmor}'s 4-exact-item-per-slot equality check
 * (already ported), and CE's own {@code instanceof EntityPlayer} restriction (never granted to a
 * generic {@code EntityLivingBase} wearer).
 *
 * <p><b>Full-suit potion tick + soft landing</b> ({@link #inventoryTick}, CE:
 * {@code onArmorTick}): refreshes REGENERATION/RESISTANCE/FIRE_RESISTANCE/SATURATION at amplifier
 * 127 for 5 ticks (ambient, no particles) and clamps fall speed to {@code >= -0.25}/{@code fallDistance = 0}
 * every tick the full set is worn. CE's 1.12 {@code ItemArmor#onArmorTick} fires only for stacks
 * actually equipped in an armor slot; 1.21's {@code Item#inventoryTick} fires for every stack in
 * every inventory slot (hotbar, offhand, etc. included), so this override adds the
 * "is this stack the one actually worn in its own slot" guard {@code ArmorFSB#inventoryTick}
 * already established, to avoid triggering the tick for an unequipped piece sitting in a backpack -
 * not a CE behavior change, a like-for-like translation of the narrower 1.12 call surface. With the
 * gate in place, a full 4-piece set still refreshes 4 times per tick (once per equipped piece),
 * matching CE's own unmodified per-piece {@code onArmorTick} call shape exactly.
 *
 * <p><b>Indestructibility</b> (CE: {@code damageArmor} multiplies by {@code 0} plus hardcoded
 * {@code getDamage()=0}/{@code setDamage()} no-op/{@code getMaxDamage()=Integer.MAX_VALUE}):
 * reproduced by simply never giving these items a {@code DataComponents.MAX_DAMAGE} component at
 * registration ({@link com.hbm.items.gear.SpecialArmorItems} does not call
 * {@code Item.Properties#durability(int)} for the 4 Euphemium pieces) - {@code Item#isDamageable}
 * is then {@code false} unconditionally, the simplest faithful equivalent of "never shows or takes
 * durability damage," without needing the vanilla {@code Unbreakable} data component on top.
 *
 * <p>{@code getArmorDisplay} (a 1.12 GUI armor-icon count) and the plain 2-texture (helmet/chest/
 * boots vs. legs) split have no work to do here - see {@code com.hbm.items.gear.ModArmor}'s javadoc:
 * {@link ArmorMaterial.Layer} plus vanilla's own non-leggings/leggings texture-suffix resolution
 * already reproduces CE's {@code euphemium_1.png}/{@code euphemium_2.png} split automatically from
 * the material this item is constructed with.
 */
public class ArmorEuphemium extends ArmorItem {

    public ArmorEuphemium(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    /** Maps this item's 4 {@link Type} constants onto the matching {@link EquipmentSlot} - same
     * approach as {@code ArmorFSB#slotForType}, avoiding reliance on an unconfirmed inherited
     * accessor for the item's own equipment slot. */
    private static EquipmentSlot slotForType(Type type) {
        return switch (type) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            default -> EquipmentSlot.CHEST;
        };
    }

    /**
     * CE: {@code ArmorEuphemium#getProperties}'s gate, restored as a standalone predicate so
     * {@code ArmorDamageHandler}'s central {@code LivingIncomingDamageEvent} listener can dispatch
     * the full-cancel check without this armor set needing its own listener.
     */
    public static boolean isFullSetWorn(LivingEntity entity) {
        return entity instanceof Player
                && ArmorUtil.checkArmor(entity, SpecialArmorItems.EUPHEMIUM_HELMET.get(), SpecialArmorItems.EUPHEMIUM_PLATE.get(),
                        SpecialArmorItems.EUPHEMIUM_LEGS.get(), SpecialArmorItems.EUPHEMIUM_BOOTS.get());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        if (player.getItemBySlot(slotForType(this.getType())) != stack) return;
        if (!isFullSetWorn(player)) return;

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 5, 127, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 127, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5, 127, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 5, 127, true, false));

        if (player.getDeltaMovement().y < -0.25D) {
            player.setDeltaMovement(player.getDeltaMovement().x, -0.25D, player.getDeltaMovement().z);
            player.fallDistance = 0.0F;
        }
    }
}
