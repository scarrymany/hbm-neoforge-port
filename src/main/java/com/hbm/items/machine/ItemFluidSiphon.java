package com.hbm.items.machine;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.tool.ItemPipette;
import com.hbm.items.tool.ToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Exact CE {@code ItemFluidSiphon}: dummyable {@code findCore}, {@code FT_Unsiphonable} skip,
 * {@link FluidContainerRegistry} fill, then pipette remainder {@code fill < 1000}.
 */
public class ItemFluidSiphon extends Item {

    public ItemFluidSiphon(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockEntity te = resolveCore(level, context.getClickedPos());
        if (level.isClientSide) {
            return te instanceof IFluidStandardReceiverMK2 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(te instanceof IFluidStandardReceiverMK2 receiver)) return InteractionResult.PASS;

        List<FluidTankNTM> tanks = receiver.getReceivingTanks();
        if (tanks == null) return InteractionResult.PASS;

        // Exact CE ItemFluidSiphon.java:38-102
        for (FluidTankNTM tank : tanks) {
            if (tank == null) continue;
            int fill = tank.getFill();
            if (fill <= 0) continue;
            FluidType tankType = tank.getTankType();
            if (tankType == Fluids.NONE) continue;
            if (tankType.hasTrait(FluidTraitSimple.FT_Unsiphonable.class)) continue;

            boolean drainedThisTank = false;
            ItemStack availablePipette = ItemStack.EMPTY;
            List<ItemStack> inv = player.getInventory().items;
            for (int slot = 0; slot < inv.size(); slot++) {
                ItemStack stack = inv.get(slot);
                if (stack.isEmpty()) continue;
                if (availablePipette.isEmpty() && stack.getItem() instanceof ItemPipette pipette) {
                    if (stack.getItem() != ToolItems.PIPETTE_LABORATORY.get()
                            && !pipette.willFizzle(tankType) && pipette.acceptsFluid(tankType, stack)) {
                        availablePipette = stack;
                    }
                }
                FluidContainerRegistry.FluidContainer recipe = FluidContainerRegistry.getFillRecipe(stack, tankType);
                if (recipe == null) continue;
                int perContainer = recipe.content();
                if (perContainer <= 0) continue;
                int maxByFluid = fill / perContainer;
                if (maxByFluid == 0) continue;
                int toFillTotal = Math.min(stack.getCount(), maxByFluid);
                if (toFillTotal <= 0) continue;
                stack.shrink(toFillTotal);
                if (stack.getCount() <= 0) {
                    inv.set(slot, ItemStack.EMPTY);
                }

                ItemStack outTemplate = recipe.fullContainer().copy();
                int maxOutStack = Math.max(1, outTemplate.getMaxStackSize());
                int remaining = toFillTotal;
                while (remaining > 0) {
                    int batch = Math.min(remaining, maxOutStack);
                    ItemStack out = outTemplate.copy();
                    out.setCount(batch);
                    if (!player.getInventory().add(out)) {
                        player.drop(out, false);
                    }
                    remaining -= batch;
                }

                fill -= toFillTotal * perContainer;
                drainedThisTank = true;
                if (fill <= 0) break;
            }

            if (!availablePipette.isEmpty() && fill > 0 && fill < 1000) {
                ItemPipette pipette = (ItemPipette) availablePipette.getItem();
                int newFill = pipette.tryFill(tankType, fill, availablePipette);
                if (newFill != fill) {
                    fill = newFill;
                    drainedThisTank = true;
                }
            }

            if (drainedThisTank) {
                tank.setFill(fill);
                te.setChanged();
                player.getInventory().setChanged();
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private static BlockEntity resolveCore(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof BlockDummyable dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core != null) pos = core;
        }
        return level.getBlockEntity(pos);
    }
}
