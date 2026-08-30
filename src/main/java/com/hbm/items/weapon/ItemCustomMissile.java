package com.hbm.items.weapon;

import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemMissile.FuelType;
import com.hbm.items.weapon.ItemMissile.WarheadType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.weapon.ItemCustomMissile} (119 lines, read in full) - the
 * {@code missile_custom} item: {@link #buildMissile} composes 4-5 {@link ItemMissile} parts into
 * one stack, {@link #getStruct} reads them back out for {@code EntityMissileCustom}'s constructor.
 * <p>
 * CE stores each part as a raw {@code Item.getIdFromItem} registry int in 4-5 flat NBT keys;
 * {@link MissileMultipartComponent} (a Data Component, see its own javadoc) replaces that,
 * resolving each part's {@link ResourceLocation} back against {@link BuiltInRegistries#ITEM} on
 * read exactly as {@code docs/phase3/missile_framework.md} recommends.
 */
public class ItemCustomMissile extends Item {

    public ItemCustomMissile(Properties properties) {
        super(properties);
    }

    public static ItemStack buildMissile(Item chip, Item warhead, Item fuselage, @Nullable Item stability, Item thruster) {
        return buildMissile(new ItemStack(chip), new ItemStack(warhead), new ItemStack(fuselage),
                stability == null ? ItemStack.EMPTY : new ItemStack(stability), new ItemStack(thruster));
    }

    public static ItemStack buildMissile(ItemStack chip, ItemStack warhead, ItemStack fuselage, @Nullable ItemStack stability, ItemStack thruster) {
        ItemStack missile = new ItemStack(MissileItems.MISSILE_CUSTOM.get());

        missile.set(MissileDataComponents.MISSILE_PARTS.get(), new MissileMultipartComponent(
                keyOf(chip.getItem()),
                keyOf(warhead.getItem()),
                keyOf(fuselage.getItem()),
                (stability == null || stability.isEmpty()) ? null : keyOf(stability.getItem()),
                keyOf(thruster.getItem())));

        return missile;
    }

    private static ResourceLocation keyOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    @Nullable
    public static MissileMultipartComponent getParts(ItemStack stack) {
        return stack.get(MissileDataComponents.MISSILE_PARTS.get());
    }

    @Nullable
    static ItemMissile resolve(@Nullable ResourceLocation id) {
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item instanceof ItemMissile im ? im : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MissileMultipartComponent parts = getParts(stack);
        if (parts == null) return;

        ItemMissile chip = resolve(parts.chip());
        ItemMissile warhead = resolve(parts.warhead());
        ItemMissile fuselage = resolve(parts.fuselage());
        ItemMissile stability = parts.fins() == null ? null : resolve(parts.fins());
        ItemMissile thruster = resolve(parts.thruster());

        // CE: catches ClassCastException around this whole block ("Drillgon200: Why is this even
        // necessary, JEI?") - the port-equivalent failure mode is a missing/renamed registry entry
        // resolving to null, guarded the same way: bail out silently rather than throw.
        if (chip == null || warhead == null || fuselage == null || thruster == null) return;

        WarheadType warheadType = (WarheadType) warhead.attributes[0];
        FuelType fuelType = (FuelType) fuselage.attributes[0];

        tooltip.add(bold("Warhead: ").append(gray(fuselage.getWarhead(warheadType))));
        tooltip.add(bold("Strength: ").append(Component.literal(String.valueOf(warhead.attributes[1])).withStyle(ChatFormatting.RED)));
        tooltip.add(bold("Fuel Type: ").append(gray(fuselage.getFuel(fuelType))));
        tooltip.add(bold("Fuel amount: ").append(gray(fuselage.attributes[1] + "l")));
        tooltip.add(bold("Chip inaccuracy: ").append(gray(((Float) chip.attributes[0]) * 100 + "%")));

        if (stability != null) {
            tooltip.add(bold("Fin inaccuracy: ").append(gray(((Float) stability.attributes[0]) * 100 + "%")));
        } else {
            tooltip.add(bold("Fin inaccuracy: ").append(gray("100%")));
        }

        tooltip.add(bold("Size: ").append(gray(fuselage.getSize(fuselage.top) + "/" + fuselage.getSize(fuselage.bottom))));

        float health = warhead.health + fuselage.health + thruster.health;
        if (stability != null) health += stability.health;

        tooltip.add(bold("Health: ").append(Component.literal(health + "HP").withStyle(ChatFormatting.GREEN)));
    }

    private static Component bold(String s) {
        return Component.literal(s).withStyle(ChatFormatting.BOLD);
    }

    private static Component gray(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GRAY);
    }

    /**
     * CE never reads {@code chip} back into {@code MissileStruct} (see that record's own javadoc) -
     * preserved here exactly: this method resolves warhead/fuselage/fins/thruster only.
     */
    @Nullable
    public static MissileStruct getStruct(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemCustomMissile)) return null;

        MissileMultipartComponent parts = getParts(stack);
        if (parts == null) return null;

        ItemMissile warhead = resolve(parts.warhead());
        ItemMissile fuselage = resolve(parts.fuselage());
        ItemMissile fins = parts.fins() == null ? null : resolve(parts.fins());
        ItemMissile thruster = resolve(parts.thruster());

        if (warhead == null || fuselage == null || thruster == null) return null;

        return new MissileStruct(warhead, fuselage, fins, thruster);
    }
}
