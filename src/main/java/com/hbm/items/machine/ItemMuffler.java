package com.hbm.items.machine;

import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Sets a machine's {@link LoadedBaseBlockEntity#muffled} flag, ported from CE's
 * {@code com.hbm.items.machine.ItemMuffler} (read in full). Works on any
 * {@link LoadedBaseBlockEntity} subclass, matching
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s finding that this item
 * "has no other package dependency beyond the base class itself" - {@code muffled} is real, already
 * base-class content per {@code docs/phase2/blockentity_base.md}, confirmed at
 * {@code LoadedBaseBlockEntity} lines 66/133 ({@code public boolean muffled}/{@code setMuffled}).
 */
public class ItemMuffler extends Item {

    public ItemMuffler(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();

        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof BlockDummyable dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core != null) pos = core;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof LoadedBaseBlockEntity loaded) || loaded.muffled) return InteractionResult.PASS;

        if (!level.isClientSide) {
            loaded.setMuffled(true);
            level.playSound(player, player == null ? pos.getX() : player.getX(), player == null ? pos.getY() : player.getY(),
                    player == null ? pos.getZ() : player.getZ(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            loaded.setChanged();
            if (player != null) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
