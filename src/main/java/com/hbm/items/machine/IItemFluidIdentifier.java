package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE {@code IItemFluidIdentifier} - marker interface for items that identify a {@link FluidType}
 * contextually (e.g., {@link ItemFluidIDMulti} returns the player-selected primary type stored in NBT).
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/items/machine/IItemFluidIdentifier.java (10 lines)
 */
public interface IItemFluidIdentifier {
    /**
     * Returns the {@link FluidType} this item-stack identifies at the given world position.
     * CE signature: {@code getType(World world, int x, int y, int z, ItemStack stack)}.
     */
    FluidType getType(Level level, BlockPos pos, ItemStack stack);
}
