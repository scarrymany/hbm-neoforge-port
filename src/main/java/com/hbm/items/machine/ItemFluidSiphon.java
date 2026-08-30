package com.hbm.items.machine;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.tool.ItemPipette;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Drains a machine's fluid tank into a held pipette, ported (in reduced scope) from CE's
 * {@code com.hbm.items.machine.ItemFluidSiphon} (read in full). Dispatches through
 * {@link IFluidStandardReceiverMK2#getReceivingTanks} - already real, already-implemented
 * infrastructure in this port.
 * <p>
 * <b>Scope reduction from CE, documented rather than silent</b>: CE's real item drains into two
 * separate consumers - (1) any held {@link ItemPipette}, and (2) CE's {@code FluidContainerRegistry}
 * (bucket-style "fill this container from this fluid" recipe registry, matching e.g. empty bottle +
 * water -> water bottle). {@code FluidContainerRegistry} does not exist anywhere in this port
 * (confirmed by grep) - it is a separate, not-yet-ported registry, not part of this machine-coupling
 * item pass's scope. This port's version drains only into a held {@link ItemPipette} (CE's own
 * fallback for "no container recipe matched"), which is a fully real, functional siphon for the
 * common case; bucket/canister-style container filling can be added once that registry lands with no
 * further changes to this class's core tank-draining logic.
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

        BlockEntity te = level.getBlockEntity(context.getClickedPos());
        if (!(te instanceof IFluidStandardReceiverMK2 receiver)) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        for (FluidTankNTM tank : receiver.getReceivingTanks()) {
            if (tank == null) continue;
            int fill = tank.getFill();
            if (fill <= 0) continue;

            FluidType type = tank.getTankType();
            if (type == Fluids.NONE) continue;

            ItemStack heldPipette = findPipette(player, type);
            if (heldPipette.isEmpty()) continue;

            ItemPipette pipette = (ItemPipette) heldPipette.getItem();
            int newFill = pipette.tryFill(type, fill, heldPipette);
            if (newFill == fill) continue;

            tank.setFill(newFill);
            te.setChanged();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static ItemStack findPipette(Player player, FluidType type) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ItemPipette pipette && pipette.acceptsFluid(type, stack) && !pipette.willFizzle(type)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
