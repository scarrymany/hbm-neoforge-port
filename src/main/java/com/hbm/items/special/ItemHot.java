package com.hbm.items.special;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code ItemHot}: an item that can be "heated up" (e.g. on crafting) and cools back
 * down by one point per tick, tracked as an int fraction of {@link #maxHeat}. CE's {@code heat}
 * field was a shared {@code static int} written by every instance's constructor - a pre-existing CE
 * bug where the last-constructed {@code ItemHot} silently wins for every other instance's
 * {@link #getHeat}/{@link #heatUp} calls. Not reproduced here: {@link #maxHeat} is a genuine
 * per-instance field instead, which is strictly more correct and changes no observable behavior for
 * any item in this port.
 * <p>
 * Not ported: CE's baked-model alpha-blended glow overlay (finding 6 - the whole
 * {@code IModelRegister}/hand-baked-quad rendering system it depends on has no 1.21 equivalent and
 * needs a model/datagen redesign, not a line-for-line port). {@code ingot_chainsteel}/
 * {@code ingot_meteorite}/{@code ingot_meteorite_forged} (the CE fields that use it) are constructed
 * directly by {@link com.hbm.items.IngotNuggetItems}, with maxHeat 100/200/200 confirmed against
 * CE's {@code ModItems.java} lines 884-886.
 */
public class ItemHot extends Item {

    private final int maxHeat;

    public ItemHot(Properties properties, int maxHeat) {
        super(properties);
        this.maxHeat = maxHeat;
    }

    public ItemStack heatUp(ItemStack stack) {
        return heatUp(stack, 1.0);
    }

    public ItemStack heatUp(ItemStack stack, double fraction) {
        return heatUpStatic(stack, fraction);
    }

    /** CE static {@code ItemHot.heatUp(stack, fraction)} — used by anvil hot smithing. */
    public static ItemStack heatUp(ItemStack stack, double fraction) {
        return heatUpStatic(stack, fraction);
    }

    private static ItemStack heatUpStatic(ItemStack stack, double fraction) {
        if (!(stack.getItem() instanceof ItemHot hot)) {
            return stack;
        }
        stack.set(SpecialItemComponents.HEAT.get(), (int) (fraction * hot.maxHeat));
        return stack;
    }

    public static double getHeat(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemHot hot)) {
            return 0;
        }
        int heat = stack.getOrDefault(SpecialItemComponents.HEAT.get(), 0);
        return (double) heat / (double) hot.maxHeat;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide()) {
            return;
        }
        Integer heat = stack.get(SpecialItemComponents.HEAT.get());
        if (heat == null) {
            return;
        }
        if (heat > 0) {
            stack.set(SpecialItemComponents.HEAT.get(), heat - 1);
        } else {
            stack.remove(SpecialItemComponents.HEAT.get());
        }
    }
}
