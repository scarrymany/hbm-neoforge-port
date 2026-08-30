package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * Dungeon-ruin decorative brick, ported from CE's {@code BlockForgottenBrick}. CE folded seven
 * cosmetic variants (including a "loot hole" / "spent hole" pair) into one {@code PropertyInteger}
 * metadata block; per the port's flattening rule each variant becomes its own registry entry, all
 * built from this one class. Only the hole variant keeps CE's interaction (empty-handed right
 * click hands the player {@code hbm:coal_eternal} and swaps the block to the emptied variant) -
 * resolved through a lazy {@code emptiedBlock} supplier and a {@link BuiltInRegistries#ITEM}
 * lookup rather than a hard {@code ModItems} field reference, since {@code coal_eternal} belongs
 * to a different Phase 1 area and has not landed yet; the interaction silently no-ops until it
 * does. CE never registers this family to a creative tab ({@code setCreativeTab(null)}), carried
 * over unchanged here by simply never wiring these into {@code CreativeTabContents}.
 */
public class BlockForgottenBrick extends BlockBase {

    private static final ResourceLocation COAL_ETERNAL_ID = ResourceLocation.fromNamespaceAndPath("hbm", "coal_eternal");

    private final Supplier<? extends net.minecraft.world.level.block.Block> emptiedBlock;

    public BlockForgottenBrick(Properties properties) {
        this(properties, null);
    }

    public BlockForgottenBrick(Properties properties, Supplier<? extends net.minecraft.world.level.block.Block> emptiedBlock) {
        super(properties);
        this.emptiedBlock = emptiedBlock;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (emptiedBlock == null || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Item coalEternal = BuiltInRegistries.ITEM.getOptional(COAL_ETERNAL_ID).orElse(null);
        if (coalEternal == null) {
            return InteractionResult.PASS;
        }

        player.getInventory().add(new ItemStack(coalEternal));
        level.setBlockAndUpdate(pos, emptiedBlock.get().defaultBlockState());
        return InteractionResult.SUCCESS;
    }
}
