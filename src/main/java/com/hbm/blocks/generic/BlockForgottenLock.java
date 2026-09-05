package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Dungeon-ruin decorative lock prop, ported from CE's {@code BlockForgottenLock}. Same flattening
 * treatment as {@link BlockForgottenBrick}: CE's four metadata variants become four registry
 * entries of this one class, none wired into a creative tab (CE: {@code setCreativeTab(null)}).
 * <p>
 * The key-unlock interaction (right-click with {@code hbm:key_red}/{@code hbm:key_red_cracked} to
 * carve out a small vault room) is resolved through a {@link BuiltInRegistries#ITEM} lookup.
 * Unlock sound Exact CE {@code BlockForgottenLock.java:88} ({@code lockOpen} 1.0F/1.0F BLOCKS at player).
 */
public class BlockForgottenLock extends BlockBase {

    private static final ResourceLocation KEY_RED_ID = ResourceLocation.fromNamespaceAndPath("hbm", "key_red");
    private static final ResourceLocation KEY_RED_CRACKED_ID = ResourceLocation.fromNamespaceAndPath("hbm", "key_red_cracked");
    private static final int ROOM_HALF_WIDTH = 2;
    private static final int ROOM_HALF_HEIGHT = 2;
    private static final int ROOM_LENGTH = 15;

    public BlockForgottenLock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
        Item keyRed = BuiltInRegistries.ITEM.getOptional(KEY_RED_ID).orElse(null);
        Item keyRedCracked = BuiltInRegistries.ITEM.getOptional(KEY_RED_CRACKED_ID).orElse(null);

        boolean cracked = keyRedCracked != null && heldItem.is(keyRedCracked);
        boolean plain = keyRed != null && heldItem.is(keyRed);
        Direction facing = hit.getDirection();

        if ((!cracked && !plain) || facing.getAxis() == Direction.Axis.Y) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (cracked) {
            heldItem.shrink(1);
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        generate(level, pos, facing);
        // Exact CE BlockForgottenLock.java:88
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                HBMSoundHandler.lockOpen.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }

    public static void generate(Level level, BlockPos pos, Direction dir) {
        Direction rot = dir.getClockWise();
        BlockState brick = ModBlocks.BRICK_FORGOTTEN.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int w = -ROOM_HALF_WIDTH; w <= ROOM_HALF_WIDTH; w++) {
            for (int h = -ROOM_HALF_HEIGHT; h <= ROOM_HALF_HEIGHT; h++) {
                for (int d = 0; d < ROOM_LENGTH; d++) {
                    boolean shell = w == -ROOM_HALF_WIDTH || w == ROOM_HALF_WIDTH
                            || h == -ROOM_HALF_HEIGHT || h == ROOM_HALF_HEIGHT
                            || d == ROOM_LENGTH - 1;
                    BlockPos target = pos.offset(
                            -dir.getStepX() * d + rot.getStepX() * w,
                            h,
                            -dir.getStepZ() * d + rot.getStepZ() * w);
                    level.setBlockAndUpdate(target, shell ? brick : air);
                }
            }
        }
    }
}
