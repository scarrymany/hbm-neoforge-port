package com.hbm.items.tool;

import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Exact CE {@code com.hbm.items.tool.ItemOilDetector} {@code :40-101}: direct column
 * {@code y+15→1} for {@code ore_oil}, bedrock {@code y=0} for {@code ore_bedrock_oil},
 * then 50 Gaussian samples (range 25). Chat {@code .bullseyeBedrock}/{@code .bullseye}/
 * {@code .detectedBedrock}/{@code .detected}/{@code .noOil}.
 */
public class ItemOilDetector extends Item {

    public ItemOilDetector(Properties properties) {
        super(properties);
    }

    public static boolean isOil(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == hbmBlock("ore_oil");
    }

    public static boolean isBedrockOil(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == hbmBlock("ore_bedrock_oil");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // CE ItemOilDetector.java:35-37
        tooltip.add(Component.literal(I18nUtil.resolveKey("item.oil_detector.desc1")));
        tooltip.add(Component.literal(I18nUtil.resolveKey("item.oil_detector.desc2")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // CE ItemOilDetector.java:49-101
        boolean bedrockoil = false;
        boolean oil = false;
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        boolean directoil = false;
        for (int ly = y + 15; ly > 0; ly--) {
            directoil |= isOil(level, mPos.set(x, ly, z));
            if (directoil) break;
        }
        boolean directBedrock = isBedrockOil(level, new BlockPos(x, 0, z));

        int range = 25;
        int samples = 50;

        for (int i = 0; i < samples; i++) {
            if (oil || bedrockoil) break;
            int lx = (int) Mth.clamp(level.getRandom().nextGaussian() * range / 2F, -range, range);
            int lz = (int) Mth.clamp(level.getRandom().nextGaussian() * range / 2F, -range, range);
            for (int ly = y + 15; ly > 0; ly--) {
                oil |= isOil(level, mPos.set(x + lx, ly, z + lz));
                if (oil) break;
            }
            bedrockoil |= isBedrockOil(level, mPos.set(x + lx, 0, z + lz));
        }

        if (!level.isClientSide()) {
            if (directBedrock) {
                player.sendSystemMessage(Component.translatable("item.oil_detector.bullseyeBedrock").withStyle(ChatFormatting.DARK_GREEN));
            } else if (directoil) {
                player.sendSystemMessage(Component.translatable("item.oil_detector.bullseye").withStyle(ChatFormatting.GREEN));
            } else if (bedrockoil) {
                player.sendSystemMessage(Component.translatable("item.oil_detector.detectedBedrock").withStyle(ChatFormatting.GOLD));
            } else if (oil) {
                player.sendSystemMessage(Component.translatable("item.oil_detector.detected").withStyle(ChatFormatting.YELLOW));
            } else {
                player.sendSystemMessage(Component.translatable("item.oil_detector.noOil").withStyle(ChatFormatting.RED));
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    private static Block hbmBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
