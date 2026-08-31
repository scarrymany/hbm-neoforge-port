package com.hbm.blocks.generic;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Canned-food loot crate, ported from CE's {@code BlockCanCrate}. CE's random-item pool
 * ({@code canned_conserve} x N food types, plus a dozen specific can items) draws entirely from
 * food items not yet registered in this port pass; the pool is left empty and extensible via
 * {@link #addContent}, matching {@link BlockCrate}'s documented gap. The crowbar-break interaction
 * itself is fully ported.
 */
public class BlockCanCrate extends Block {

    private static final List<Supplier<ItemStack>> CONTENTS = new ArrayList<>();

    public BlockCanCrate(Properties properties) {
        super(properties);
    }

    /** Registers one more possible can/food drop for this crate's contents roll. */
    public static void addContent(Supplier<ItemStack> stack) {
        CONTENTS.add(stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(BlockCrate.CROWBAR_TAG)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            dropContents(level, pos);
            level.removeBlock(pos, false);
            level.playSound(null, pos, HBMSoundHandler.crateBreak.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private void dropContents(Level level, BlockPos pos) {
        if (CONTENTS.isEmpty()) {
            return;
        }

        RandomSource random = level.getRandom();
        int count = 5 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            ItemStack stack = CONTENTS.get(random.nextInt(CONTENTS.size())).get();
            Block.popResource(level, pos, stack);
        }
    }
}
