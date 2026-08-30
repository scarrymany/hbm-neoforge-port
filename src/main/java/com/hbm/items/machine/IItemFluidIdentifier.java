package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ported unchanged from CE's {@code com.hbm.items.machine.IItemFluidIdentifier} - a trivial
 * interface (one method) whose only implementor, {@link ItemFluidIDMulti}, lives in this same file
 * per {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s recommendation ("port
 * alongside {@code ItemFluidIDMulti} - no reason to split").
 */
public interface IItemFluidIdentifier {

    FluidType getType(Level level, BlockPos pos, ItemStack stack);
}
