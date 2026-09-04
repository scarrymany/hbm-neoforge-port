package com.hbm.items.armor;

import com.google.common.collect.Multimap;
import com.hbm.handler.ArmorModHandler;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ItemArmorMod} - the base class for every "chip"
 * mod-slot insert AND every jetpack (a jetpack is dual-mode: a standalone chestplate <i>or</i> a
 * mod-slot-1 insert into another chestplate - see {@code docs/phase3/fsb_armor_and_jetpacks.md}
 * headline finding #4; concrete jetpack leaves are a later package's job).
 *
 * <p>Per this package's task brief item 5, ported against Neo Edition's already-confirmed-real
 * 1.21.1 shape: extends {@link Item} directly (CE extends {@code ItemCustomLore} purely for one
 * lore tooltip line - not worth carrying the inheritance for), and drops CE's
 * {@code offset(EntityPlayer, EntityPlayer, float)}/{@code copyRot(ModelBiped, ModelBiped)}
 * GL-immediate-mode render helpers entirely (Phase 5's job if a jetpack/mod render layer ever needs
 * equivalent math again - nothing in this port's confirmed API surface replaces
 * {@code GlStateManager.translate} 1:1, so inventing a placeholder here would be a guess).
 *
 * <p>{@link #modDamage} takes {@link LivingDamageEvent.Pre} (not CE's {@code LivingHurtEvent}) -
 * the same event {@link IDamageHandler} dispatches from, both centrally invoked by
 * {@code com.hbm.handler.ArmorDamageHandler}.
 */
public class ItemArmorMod extends Item {

    public final int type;
    public final boolean helmet;
    public final boolean chestplate;
    public final boolean leggings;
    public final boolean boots;

    public ItemArmorMod(Properties properties, int type, boolean helmet, boolean chestplate, boolean leggings, boolean boots) {
        super(properties.stacksTo(1));
        this.type = type;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        components.add(Component.literal(I18nUtil.resolveKey("armorMod.applicableTo")).withStyle(ChatFormatting.DARK_PURPLE));

        if (helmet && chestplate && leggings && boots) {
            components.add(Component.literal("  " + I18nUtil.resolveKey("armorMod.all")));
        } else {
            if (helmet) components.add(Component.literal("  " + I18nUtil.resolveKey("armorMod.helmets")));
            if (chestplate) components.add(Component.literal("  " + I18nUtil.resolveKey("armorMod.chestplates")));
            if (leggings) components.add(Component.literal("  " + I18nUtil.resolveKey("armorMod.leggings")));
            if (boots) components.add(Component.literal("  " + I18nUtil.resolveKey("armorMod.boots")));
        }
        components.add(Component.literal(I18nUtil.resolveKey("desc.applicableslot")).withStyle(ChatFormatting.DARK_PURPLE));

        String key = switch (this.type) {
            case ArmorModHandler.helmet_only -> "armorMod.type.helmet";
            case ArmorModHandler.plate_only -> "armorMod.type.chestplate";
            case ArmorModHandler.legs_only -> "armorMod.type.leggings";
            case ArmorModHandler.boots_only -> "armorMod.type.boots";
            case ArmorModHandler.servos -> "armorMod.type.servo";
            case ArmorModHandler.cladding -> "armorMod.type.cladding";
            case ArmorModHandler.kevlar -> "armorMod.type.insert";
            case ArmorModHandler.extra -> "armorMod.type.special";
            case ArmorModHandler.battery -> "armorMod.type.battery";
            default -> null;
        };
        if (key != null) components.add(Component.literal("  " + I18nUtil.resolveKey(key)));
    }

    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(stack.getHoverName());
    }

    /** CE: {@code ItemArmorMod#modUpdate(EntityLivingBase, ItemStack)} - the per-tick hook a mod-slot
     * item receives while installed, mirroring the tick it would get if worn standalone (see class
     * javadoc's jetpack dual-mode note). No-op base; overridden by leaves. */
    public void modUpdate(LivingEntity entity, ItemStack armor) {
    }

    /** CE: {@code ItemArmorMod#modDamage(LivingHurtEvent, ItemStack)}. No-op base; overridden by leaves. */
    public void modDamage(LivingDamageEvent.Pre event, ItemStack armor) {
    }

    /** CE: {@code ItemArmorMod#getModifiers(EntityEquipmentSlot, ItemStack)}. Slot dropped — no leaf uses it. Applied/removed from {@code CommonTickEvents#tickArmorMods}. */
    @Nullable
    public <K, V> Multimap<K, V> getModifiers(ItemStack armor) {
        return null;
    }
}
