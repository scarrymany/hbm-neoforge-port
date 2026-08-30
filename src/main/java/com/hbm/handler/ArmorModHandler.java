package com.hbm.handler;

import com.hbm.items.armor.ItemArmorMod;
import com.hbm.main.MainRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Direct port of CE's {@code com.hbm.handler.ArmorModHandler}: the 9-slot armor-mod bookkeeping
 * engine (helmet/plate/legs/boots-only sockets, servos, cladding, kevlar insert, extra, battery).
 * Already a live, uncommented dependency of {@code util.ArmorRegistry#getProtectionFromItem}
 * ({@code hasMods}/{@code pryMods}) and {@code capability.HbmPlayerAttachment#getEffectiveMaxShield}
 * ({@code pryMods}/{@code kevlar}).
 *
 * <p>Storage rewrite (this area's research report, API decision #2): CE's
 * {@code MOD_COMPOUND_KEY}/{@code MOD_SLOT_KEY_<n>} nested-NBT-tag scheme becomes vanilla's own
 * {@link DataComponents#CONTAINER} / {@link ItemContainerContents} component, sized to
 * {@link #MOD_SLOTS} - exactly the pattern {@code items.ItemInventory} already uses for a
 * fixed-size {@code ItemStack} array on a stack. No new component registration is needed for this
 * class at all.
 */
public final class ArmorModHandler {

    private ArmorModHandler() {
    }

    public static final int helmet_only = 0;
    public static final int plate_only = 1;
    public static final int legs_only = 2;
    public static final int boots_only = 3;
    public static final int servos = 4;
    public static final int cladding = 5;
    public static final int kevlar = 6;
    public static final int extra = 7;
    public static final int battery = 8;

    public static final int MOD_SLOTS = 9;

    /**
     * Ported from CE's {@code ArmorModHandler.fixedUUIDs} - a per-<b>slot</b> (not per-item) fixed
     * identity every "always-on Armor modifier" attribute uses, so wearing several pieces that each
     * contribute one of these (CE: {@code ArmorDesh}/{@code ArmorDiesel}/{@code ArmorLiquidator})
     * stack additively across slots without colliding within a slot. 1.21's
     * {@code AttributeModifier} keys on {@link ResourceLocation} rather than CE's raw {@link
     * java.util.UUID}, but the role is identical: one fixed id per {@link EquipmentSlot}, shared by
     * whichever concrete item currently occupies it.
     */
    private static final ResourceLocation ARMOR_MODIFIER_HEAD =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "armor_modifier_head");
    private static final ResourceLocation ARMOR_MODIFIER_CHEST =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "armor_modifier_chest");
    private static final ResourceLocation ARMOR_MODIFIER_LEGS =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "armor_modifier_legs");
    private static final ResourceLocation ARMOR_MODIFIER_FEET =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "armor_modifier_feet");

    public static ResourceLocation getArmorSlotModifierId(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ARMOR_MODIFIER_HEAD;
            case CHEST -> ARMOR_MODIFIER_CHEST;
            case LEGS -> ARMOR_MODIFIER_LEGS;
            case FEET -> ARMOR_MODIFIER_FEET;
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };
    }

    /** The single-slot {@link EquipmentSlotGroup} matching one armor {@link EquipmentSlot}, for
     * building a static {@code ItemAttributeModifiers} component scoped to just that slot. */
    public static EquipmentSlotGroup getArmorSlotGroup(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };
    }

    /**
     * Checks if a mod can be applied to an armor piece. Needs to be used to prevent people from
     * inserting invalid items into the armor table.
     *
     * <p>{@code com.hbm.items.armor.ItemArmorMod} now exists (ported by the armor/FSB framework
     * package) - this restores CE's real body, confirmed against Neo Edition's own already-ported
     * {@code ArmorModHandler.isApplicable} for the 1.21.1 {@code ArmorItem.Type} shape.
     */
    public static boolean isApplicable(ItemStack armor, ItemStack mod) {
        if (armor.isEmpty() || !(armor.getItem() instanceof ArmorItem armorItem)) return false;
        if (mod.isEmpty() || !(mod.getItem() instanceof ItemArmorMod armorMod)) return false;

        ArmorItem.Type type = armorItem.getType();

        return (type == ArmorItem.Type.HELMET && armorMod.helmet) ||
                (type == ArmorItem.Type.CHESTPLATE && armorMod.chestplate) ||
                (type == ArmorItem.Type.LEGGINGS && armorMod.leggings) ||
                (type == ArmorItem.Type.BOOTS && armorMod.boots);
    }

    /**
     * Applies a mod to the given armor piece. Make sure to check {@link #isApplicable} first.
     * Will override present mods, so make sure to only use unmodded armor pieces.
     *
     * <p>{@code com.hbm.items.armor.ItemArmorMod} now exists - this restores CE's real body
     * ({@code ItemArmorMod#type} selects which of the {@link #MOD_SLOTS} slots the mod occupies),
     * against this class's own {@link ItemContainerContents}-backed storage rather than CE's raw
     * NBT compound.
     */
    public static void applyMod(ItemStack armor, ItemStack mod) {
        if (armor.isEmpty() || mod.isEmpty() || !(mod.getItem() instanceof ItemArmorMod armorMod)) return;

        NonNullList<ItemStack> slots = readMods(armor);
        slots.set(armorMod.type, mod.copyWithCount(1));
        armor.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
    }

    /**
     * Removes the mod from the given slot.
     */
    public static void removeMod(ItemStack armor, int slot) {
        if (armor.isEmpty()) return;

        NonNullList<ItemStack> slots = readMods(armor);
        slots.set(slot, ItemStack.EMPTY);
        writeModsOrClear(armor, slots);
    }

    /**
     * Removes ALL mods. Should be used when the armor piece is put in the armor table slot AFTER
     * the armor pieces have been separated.
     */
    public static void clearMods(ItemStack armor) {
        armor.remove(DataComponents.CONTAINER);
    }

    /**
     * Does what the name implies: true if at least one of the {@link #MOD_SLOTS} slots is
     * occupied.
     */
    public static boolean hasMods(ItemStack armor) {
        for (ItemStack mod : readMods(armor)) {
            if (!mod.isEmpty()) return true;
        }
        return false;
    }

    /**
     * Gets all the modifications in the provided armor. Empty slots come back as
     * {@link ItemStack#EMPTY} (never {@code null}), matching CE's own {@code pryMods} contract.
     */
    public static ItemStack[] pryMods(ItemStack armor) {
        return readMods(armor).toArray(new ItemStack[0]);
    }

    public static ItemStack pryMod(ItemStack armor, int slot) {
        return readMods(armor).get(slot);
    }

    private static NonNullList<ItemStack> readMods(ItemStack armor) {
        NonNullList<ItemStack> slots = NonNullList.withSize(MOD_SLOTS, ItemStack.EMPTY);
        ItemContainerContents contents = armor.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(slots);
        }
        return slots;
    }

    private static void writeModsOrClear(ItemStack armor, NonNullList<ItemStack> slots) {
        for (ItemStack mod : slots) {
            if (!mod.isEmpty()) {
                armor.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
                return;
            }
        }
        // No mod left in any slot - drop the component entirely rather than keep an all-empty
        // one around, matching CE's own clearMods-on-last-removal behavior.
        clearMods(armor);
    }
}
