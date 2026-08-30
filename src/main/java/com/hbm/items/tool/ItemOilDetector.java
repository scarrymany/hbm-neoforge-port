package com.hbm.items.tool;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Samples straight down and a scatter of nearby columns for oil ore. Ported from CE's
 * {@code com.hbm.items.tool.ItemOilDetector}.
 *
 * <p><b>Stubbed pending {@code ModBlocks.ore_oil}/{@code ore_bedrock_oil}.</b> Unlike
 * {@link ItemOreDensityScanner} (whose dependency, the {@code BedrockOre*} noise-scan cluster in
 * {@code com.hbm.items.special}, is already ported), this item's real CE dependency is two
 * <em>world-gen ore blocks</em> that do not exist anywhere in this port yet -
 * {@code com.hbm.blocks.ModBlocks} is still the Phase 0 registry skeleton with no oil ore block
 * fields at all. Per the port plan's "stub with a documented TODO rather than blocking" rule, the
 * item is registered (tooltip included) and its right-click keeps CE's genuine tactile feedback (the
 * detector "click" sound and arm swing) but reports no result, rather than fabricating a detection
 * outcome against ore that cannot exist in the world yet.
 */
public class ItemOilDetector extends Item {

    public ItemOilDetector(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Scans the local area for oil deposits."));
        tooltip.add(Component.literal("Right-click to scan."));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            // TODO(cross-area follow-up): once ModBlocks.ore_oil/ore_bedrock_oil exist, port CE's
            // direct-column + 50-sample Gaussian-scatter search here and report bullseye/detected/
            // noOil via a chat message, as CE's onItemRightClick does.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }
}
