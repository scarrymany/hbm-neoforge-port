package com.hbm.items.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm.handler.ArmorModHandler;
import com.hbm.interfaces.IArmorModDash;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ItemModV1} (46 lines, read in full) - MaskMan's
 * boss-loot armor mod-chip trophy ({@code docs/phase4/entities_bosses.md}). Occupies the "extra"
 * mod-slot index but is applicable to chestplate pieces only (CE:
 * {@code super(ArmorModHandler.extra, false, true, false, false, s)}).
 * <p>
 * {@link #getModifiers} carries CE's real +50% multiplicative movement-speed bonus ("V1 SPEED" - CE's
 * {@code AttributeModifier.Operation} value {@code 2} is 1.12's {@code MULTIPLY_TOTAL}, the direct
 * predecessor of this port's {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}, same ordinal
 * position and semantics). Dispatched from {@code CommonTickEvents#tickArmorMods} Exact CE
 * {@code ModEventHandler:1220-1241}.
 */
public class ItemModV1 extends ItemArmorMod implements IArmorModDash {

    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "v1_speed");

    public ItemModV1(Properties properties) {
        super(properties, ArmorModHandler.extra, false, true, false, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("BLOOD IS FUEL").withStyle(ChatFormatting.RED));
        components.add(Component.literal(""));
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(Component.literal("  ").append(stack.getHoverName()).append(" (BLOOD IS FUEL)").withStyle(ChatFormatting.RED));
    }

    @Override
    public int getDashes() {
        return 3;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Multimap<K, V> getModifiers(ItemStack armor) {
        Multimap<Holder<Attribute>, AttributeModifier> map = ImmutableMultimap.of(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(SPEED_MODIFIER_ID, 0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return (Multimap<K, V>) (Multimap<?, ?>) map;
    }
}
