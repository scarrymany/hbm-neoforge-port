package com.hbm.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * CE looked up an {@code IItemHandler} through a held {@code ICapabilityProvider} reference; NeoForge
 * dropped that instance-based capability model in favour of looking a capability up by block position
 * (see {@code Capabilities.ItemHandler.BLOCK}), so the {@code ICapabilityProvider} overloads become
 * {@code Level}/{@code BlockPos} overloads here instead.
 * <p>
 * Neo Edition never touches {@code net.neoforged.neoforge.capabilities}, so this was verified directly
 * against the {@code neoforge-21.1.228-sources.jar} instead: {@code Capabilities.ItemHandler.BLOCK} is a
 * real {@code BlockCapability<IItemHandler, @Nullable Direction>}
 * ({@code net/neoforged/neoforge/capabilities/Capabilities.java}), and {@code Level} inherits
 * {@code getCapability(BlockCapability<T, C>, BlockPos, C)} from
 * {@code net.neoforged.neoforge.common.extensions.ILevelExtension}, matching the
 * {@code world.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)} calls below.
 */
public class InventoryHelper {

    public static final Random RANDOM = new Random();

    public static void dropInventoryItems(Level world, BlockPos pos, @Nullable Direction side) {
        IItemHandler inventory = world.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (inventory == null) return;
        dropInventoryItems(world, pos, inventory);
    }

    public static void dropInventoryItems(Level world, BlockPos pos, IItemHandler inventory) {
        IntStream.range(0, inventory.getSlots()).mapToObj(inventory::getStackInSlot).filter(itemstack ->
                !itemstack.isEmpty()).forEach(itemstack -> spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), itemstack));
    }

    public static void dropInventoryItems(Level world, BlockPos pos, @Nullable Direction side, int beginSlot, int endSlot) {
        IItemHandler inventory = world.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (inventory == null) return;
        for (int i = beginSlot; i <= endSlot; ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);

            if (!itemstack.isEmpty()) {
                spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), itemstack);
            }
        }
    }

    /**
     * DO NOT ADD 0.5 to x, y, z if you are using this with a BlockPos!
     */
    public static void spawnItemStack(Level worldIn, double x, double y, double z, ItemStack stack) {
        float xOffset = RANDOM.nextFloat() * 0.8F + 0.1F;
        float yOffset = RANDOM.nextFloat() * 0.8F + 0.1F;
        float zOffset = RANDOM.nextFloat() * 0.8F + 0.1F;

        while (!stack.isEmpty()) {
            ItemEntity entityitem = new ItemEntity(worldIn, x + xOffset, y + yOffset, z + zOffset, stack.split(RANDOM.nextInt(21) + 10));
            entityitem.setDeltaMovement(RANDOM.nextGaussian() * 0.05, RANDOM.nextGaussian() * 0.05 + 0.2, RANDOM.nextGaussian() * 0.05);
            worldIn.addFreshEntity(entityitem);
        }
    }
}
