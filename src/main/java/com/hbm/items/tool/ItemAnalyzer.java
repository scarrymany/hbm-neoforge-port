package com.hbm.items.tool;

import com.hbm.api.block.ILockable;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.network.PipeBaseBlockEntity;
import com.hbm.blockentity.network.energy.PylonBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
 * {@link ILockable} lock state (CE: {@code TileEntityPylonBase}/{@code TileEntityPipeBaseNT}/
 * {@code TileEntityLockableBase} branches, all confirmed real targets in this port - see this
 * class's sibling {@link ItemAnalysisTool} for the {@code BlockDummyable}/{@code IAnalyzable}-based
 * per-machine debug info this item does not attempt to duplicate).
 * <p>
 * <b>Not ported</b>: CE's {@code TileEntityDummy} branch (a bespoke lightweight dummy-block marker
 * distinct from this port's {@code BlockDummyable}, which resolves dummy positions at the
 * {@code Block} level via {@code findCore} instead of a dedicated dummy {@code BlockEntity} class) -
 * not applicable to this port's multiblock design, so nothing is lost by omitting it.
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
}
