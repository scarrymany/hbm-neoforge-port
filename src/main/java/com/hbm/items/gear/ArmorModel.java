package com.hbm.items.gear;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Ported from CE's {@code com.hbm.items.gear.ArmorModel} - the base class for CE's small family of
 * "wears like armor but has its own hand-modeled shape" pieces (goggles, damp/piss rag masks, the
 * radiation/gasmask/schrabidium capes, the top hat). CE's version did three things by {@code ==}
 * identity dispatch against {@code ModItems} fields: (1) picked a custom {@code ModelBiped}
 * replacement per item via {@code getArmorModel}, (2) drew a full-screen helmet overlay (the
 * goggle motion-blur PNG cycle) via {@code renderHelmetOverlay}, and (3) randomly shrank+dropped
 * two specific mask items via {@code onUpdate} ("decay").
 *
 * <p>None of the concrete leaf items this class dispatched on on ({@code goggles}, {@code
 * mask_damp}/{@code mask_piss}, the capes, {@code hat}) exist in this port yet - they are a later
 * Phase 3 content package's job (see {@code docs/phase3/armor_equippable_framework.md}'s Deferred
 * scope: "All client-side rendering ... belong to Phase 5"). Per this package's task brief (item 8),
 * this class ships only the confirmed-real 1.21.1 <b>hook points</b> those future leaves will use,
 * with no invented {@code Model} subclass and no per-item identity dispatch (CE's {@code ==}-chain
 * style has no place left once concrete items are proper metadata-flattened registry entries -
 * each leaf item can simply override {@link #getGenericArmorModel} and {@link #tickDecay} itself):
 *
 * <ul>
 *     <li>{@link #initializeClient(Consumer)} / {@link #getGenericArmorModel} - the confirmed real
 *     replacement for CE's {@code getArmorModel(EntityLivingBase, ItemStack, EntityEquipmentSlot,
 *     ModelBiped)}, via {@link IClientItemExtensions#getGenericArmorModel(LivingEntity, ItemStack,
 *     EquipmentSlot, HumanoidModel)} - confirmed real via Neo Edition's {@code ArmorNo9}. The
 *     default implementation returns {@code original} (no custom model) until a Phase 5 leaf
 *     override supplies one.</li>
 *     <li>{@link #tickDecay} - the extension point for CE's {@code onUpdate} random per-tick decay
 *     (CE: {@code mask_damp}/{@code mask_piss}, 1-in-8192 chance per tick to shrink by 1 and drop a
 *     {@code mask_rag}). Default no-op; a later leaf overrides it to opt in with its own byproduct
 *     item once that item exists. CE's {@code onUpdate} had no "is this actually worn" guard (1.12's
 *     {@code Item#onUpdate} already fired for every inventory stack including armor slots, exactly
 *     like this port's {@link #inventoryTick}), so none is added here either - this mirrors CE
 *     exactly, not a simplification.</li>
 * </ul>
 *
 * The goggle motion-blur overlay ({@code renderHelmetOverlay}) is pure GL-immediate-mode rendering
 * with no 1.21 client-item-extension equivalent surveyed in this pass - left entirely to Phase 5,
 * per the Deferred-scope note above; no hook or field for it is invented here.
 */
public class ArmorModel extends ArmorItem {

    public ArmorModel(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                return ArmorModel.this.getGenericArmorModel(livingEntity, itemStack, equipmentSlot, original);
            }
        });
    }

    /**
     * CE: {@code ArmorModel#getArmorModel(EntityLivingBase, ItemStack, EntityEquipmentSlot,
     * ModelBiped)}. Overridden by a Phase 5 leaf item to swap in a custom {@link Model}
     * (goggles/mask/cape/hat); the base class has no model of its own to offer.
     */
    @OnlyIn(Dist.CLIENT)
    protected Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        return original;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        tickDecay(stack, level, player);
    }

    /**
     * CE: {@code ArmorModel#onUpdate}'s random decay-and-drop chance. No-op by default; see class
     * javadoc.
     */
    protected void tickDecay(ItemStack stack, Level level, Player player) {
    }
}
