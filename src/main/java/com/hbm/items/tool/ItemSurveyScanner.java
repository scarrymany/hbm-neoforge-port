package com.hbm.items.tool;

import com.hbm.blocks.generic.BlockBedrockOreTE;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Exact CE {@code com.hbm.items.tool.ItemSurveyScanner} {@code :30-87}: 11×11 column grid
 * ({@code a,b ∈ [-5,5]}, {@code y+15} down by 2) for {@code ore_oil}/{@code ore_coltan}/
 * {@code stone_depth}/{@code stone_depth_nether}/{@code stone_gneiss}/{@code ore_australium},
 * plus {@code ore_bedrock_block} TE {@code resource} at {@code y=0}. Easter egg
 * {@code block_beryllium}+{@code entanglement_kit} stays skipped ({@code block_beryllium}
 * is not registered).
 */
public class ItemSurveyScanner extends Item {

    public ItemSurveyScanner(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // CE ItemSurveyScanner.java:34-80
            BlockPos playerPos = player.blockPosition();
            int x = playerPos.getX();
            int y = playerPos.getY();
            int z = playerPos.getZ();

            boolean hasOil = false;
            boolean hasColtan = false;
            boolean hasDepth = false;
            boolean hasSchist = false;
            boolean hasAussie = false;
            BlockBedrockOreTE.BedrockOreBlockEntity tile = null;

            Block oreOil = hbmBlock("ore_oil");
            Block oreColtan = hbmBlock("ore_coltan");
            Block stoneDepth = hbmBlock("stone_depth");
            Block stoneDepthNether = hbmBlock("stone_depth_nether");
            Block stoneGneiss = hbmBlock("stone_gneiss");
            Block oreAustralium = hbmBlock("ore_australium");
            Block oreBedrock = hbmBlock("ore_bedrock_block");

            for (int a = -5; a <= 5; a++) {
                for (int b = -5; b <= 5; b++) {
                    for (int i = y + 15; i > 1; i -= 2) {
                        Block block = level.getBlockState(new BlockPos(x + a * 5, i, z + b * 5)).getBlock();

                        if (block == oreOil) hasOil = true;
                        else if (block == oreColtan) hasColtan = true;
                        else if (block == stoneDepth) hasDepth = true;
                        else if (block == stoneDepthNether) hasDepth = true;
                        else if (block == stoneGneiss) hasSchist = true;
                        else if (block == oreAustralium) hasAussie = true;
                    }

                    Block bedrockBlock = level.getBlockState(new BlockPos(x + a * 2, 0, z + b * 2)).getBlock();
                    if (bedrockBlock == oreBedrock) {
                        BlockEntity te = level.getBlockEntity(new BlockPos(x + a * 2, 0, z + b * 2));
                        if (te instanceof BlockBedrockOreTE.BedrockOreBlockEntity ore) {
                            tile = ore;
                        }
                    }
                }
            }

            if (hasOil) {
                player.sendSystemMessage(Component.translatable("chat.surveyscanner.oil").withStyle(ChatFormatting.BLACK));
            }
            if (hasColtan) {
                player.sendSystemMessage(Component.translatable("chat.surveyscanner.coltan").withStyle(ChatFormatting.GOLD));
            }
            if (hasDepth) {
                player.sendSystemMessage(Component.translatable("chat.surveyscanner.depth").withStyle(ChatFormatting.GRAY));
            }
            if (hasSchist) {
                player.sendSystemMessage(Component.translatable("chat.surveyscanner.schist").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (hasAussie) {
                player.sendSystemMessage(Component.translatable("chat.surveyscanner.australium").withStyle(ChatFormatting.YELLOW));
            }
            if (tile != null && !tile.resource.isEmpty()) {
                player.sendSystemMessage(Component.translatable("chat.surveyscanner.bedrock", tile.resource.getHoverName())
                        .withStyle(ChatFormatting.RED));
            }
        }

        // CE :83-85 — sound + swing on both sides
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // CE ItemSurveyScanner.java:91-98 — block_beryllium is not registered
        return InteractionResult.PASS;
    }

    private static Block hbmBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
