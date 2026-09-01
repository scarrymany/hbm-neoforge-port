package com.hbm.items.weapon;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Flatten of one CE {@code ItemAmmoArty} meta. Tooltips from {@code ItemAmmoArty.java:119-173}.
 */
public class ItemArtyShell extends ItemBase {

    public ItemArtyShell(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int type = ArtilleryAmmo.typeOfArty(stack.getItem());
        switch (type) {
            case ArtilleryAmmo.ARTY_NORMAL -> {
                tooltip.add(Component.literal("Strength: 10").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Damage modifier: 3x").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Does not destroy blocks").withStyle(ChatFormatting.BLUE));
            }
            case ArtilleryAmmo.ARTY_CLASSIC -> {
                tooltip.add(Component.literal("Strength: 15").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Damage modifier: 5x").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Does not destroy blocks").withStyle(ChatFormatting.BLUE));
            }
            case ArtilleryAmmo.ARTY_HE -> {
                tooltip.add(Component.literal("Strength: 15").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Damage modifier: 3x").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Destroys blocks").withStyle(ChatFormatting.RED));
            }
            case ArtilleryAmmo.ARTY_PHOSPHORUS -> {
                tooltip.add(Component.literal("Strength: 10").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Damage modifier: 3x").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Phosphorus splash").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("Does not destroy blocks").withStyle(ChatFormatting.BLUE));
            }
            case ArtilleryAmmo.ARTY_PHOSPHORUS_MULTI ->
                    tooltip.add(Component.literal("Splits x10").withStyle(ChatFormatting.RED));
            case ArtilleryAmmo.ARTY_MINI_NUKE -> {
                tooltip.add(Component.literal("Strength: 20").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Deals nuclear damage").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("Destroys blocks").withStyle(ChatFormatting.RED));
            }
            case ArtilleryAmmo.ARTY_MINI_NUKE_MULTI ->
                    tooltip.add(Component.literal("Splits x5").withStyle(ChatFormatting.RED));
            case ArtilleryAmmo.ARTY_NUKE -> {
                tooltip.add(Component.literal("☠").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("(that is the best skull and crossbones").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("minecraft's unicode has to offer)").withStyle(ChatFormatting.RED));
            }
            case ArtilleryAmmo.ARTY_CARGO -> {
                ItemStack cargo = ArtilleryAmmo.getCargo(stack, context.registries());
                if (cargo.isEmpty()) {
                    tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.RED));
                } else {
                    tooltip.add(cargo.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
                }
            }
            default -> {
            }
        }
    }

    public static CompoundTag cargoTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }
}
