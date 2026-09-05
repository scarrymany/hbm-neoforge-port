package com.hbm.items.tool;

import com.hbm.api.block.ILockable;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.machine.foundry.FoundryCastingBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKSlottedBlockEntity;
import com.hbm.blockentity.network.PipeBaseBlockEntity;
import com.hbm.blockentity.network.energy.PylonBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Generic block/tile-entity debug dump, ported from CE's {@code com.hbm.items.tool.ItemAnalyzer}
 * (read in full). Reports the block's registry name, the block entity's class name, and - if
 * present - a few well-known interfaces' state: {@link IEnergyReceiverMK2} power,
 * {@link PipeBaseBlockEntity} duct fluid type, {@link PylonBaseBlockEntity} connection list, and
 * {@link ILockable} lock state, and slot count (CE {@code IInventory.getSizeInventory()}
 * {@code :74-78}). 1.21 has no {@code IInventory} on BEs — slot count is
 * {@code MachineBaseBlockEntity}/{@code RBMKSlottedBlockEntity}/{@code FoundryCastingBaseBlockEntity}
 * {@code inventory.getSlots()} or vanilla {@link Container#getContainerSize()}.
 * <p>
 * <b>Not ported</b>: CE's {@code TileEntityDummy} branch (port uses {@code BlockDummyable.findCore},
 * no dummy BE) and 1.12 {@code Meta:} dump.
 */
public class ItemAnalyzer extends Item {

    public ItemAnalyzer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Block block = level.getBlockState(pos).getBlock();

        if (level.isClientSide) {
            if (player != null) {
                player.displayClientMessage(Component.literal("Block: " + BuiltInRegistries.BLOCK.getKey(block)), false);
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (player == null) return InteractionResult.SUCCESS;

        if (te == null) {
            player.displayClientMessage(Component.literal("Tile Entity: none"), false);
        } else {
            player.displayClientMessage(Component.literal("Tile Entity: " + te.getClass().getSimpleName()), false);

            // CE ItemAnalyzer.java:74-78
            int slots = analyzerSlotCount(te);
            if (slots >= 0) {
                player.displayClientMessage(Component.literal("Slots: " + slots), false);
            }

            if (te instanceof IEnergyReceiverMK2 receiver) {
                player.displayClientMessage(Component.literal("Electricity: " + receiver.getPower() + " HE"), false);
            }

            if (te instanceof PipeBaseBlockEntity pipe) {
                player.displayClientMessage(Component.literal("Duct Type: ").append(pipe.getFluidType().getLocalizedName()), false);
            }

            if (te instanceof PylonBaseBlockEntity pylon) {
                player.displayClientMessage(Component.literal("Connections:"), false);
                for (BlockPos c : pylon.connected) {
                    player.displayClientMessage(Component.literal(" *" + c.getX() + " / " + c.getY() + " / " + c.getZ()), false);
                }
            }

            if (te instanceof ILockable lockable) {
                player.displayClientMessage(Component.literal("Locked: " + lockable.isLocked()), false);
                if (lockable.isLocked()) {
                    player.displayClientMessage(Component.literal("Pick Chance: " + (lockable.getMod() * 100D) + "%"), false);
                }
            }
        }

        player.displayClientMessage(Component.literal("----------------------------"), false);
        return InteractionResult.SUCCESS;
    }

    /** 1.21 stand-in for CE {@code IInventory.getSizeInventory()}. */
    private static int analyzerSlotCount(BlockEntity te) {
        if (te instanceof MachineBaseBlockEntity machine && machine.inventory != null) {
            return machine.inventory.getSlots();
        }
        if (te instanceof RBMKSlottedBlockEntity rbmk) {
            return rbmk.inventory.getSlots();
        }
        if (te instanceof FoundryCastingBaseBlockEntity foundry && foundry.inventory != null) {
            return foundry.inventory.getSlots();
        }
        if (te instanceof Container container) {
            return container.getContainerSize();
        }
        return -1;
    }
}
