package com.hbm.items.tool;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Wide-area resource survey: scans a grid of columns for several named ores plus any bedrock-ore
 * tile entity nearby. Ported from CE's {@code com.hbm.items.tool.ItemSurveyScanner}.
 *
 * <p><b>Stubbed pending several missing world-gen blocks.</b> CE's {@code onItemRightClick} checks a
 * grid of columns against {@code ModBlocks.ore_oil}/{@code ore_coltan}/{@code stone_depth}/
 * {@code stone_depth_nether}/{@code stone_gneiss}/{@code ore_australium} and a
 * {@code BlockBedrockOreTE.TileEntityBedrockOre} lookup against {@code ModBlocks.ore_bedrock_block}.
 * None of these exist in this port yet: {@code com.hbm.blocks.ModBlocks} is still the Phase 0
 * registry skeleton (no such fields at all), and this port's own bedrock-ore system
 * ({@code com.hbm.blocks.generic.BlockBedrockOre} + {@code com.hbm.items.special.BedrockOre*}) is a
 * different, simpler design than CE's TE-backed feature and has no equivalent
 * "resource"-bearing tile entity to query. (Note: {@link ItemOreDensityScanner} in this same package
 * is genuinely portable today because its dependency, the noise-scan side of that cluster, does not
 * need a placed tile entity - see that class's javadoc.) CE's Nether-portal easter egg
 * ({@code block_beryllium} + {@code entanglement_kit}) is dropped for the same reason - neither item
 * nor block exists yet. Per the port plan's "stub with a documented TODO rather than blocking" rule,
 * the item is registered and keeps CE's genuine tactile feedback (detector sound + arm swing) on
 * right-click; the survey and easter-egg block-use are both left explicit TODOs.
 */
public class ItemSurveyScanner extends Item {

    public ItemSurveyScanner(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            // TODO(cross-area follow-up): once the relevant ore/stone blocks exist in ModBlocks,
            // port CE's 11x11 column grid survey (oil/coltan/depth-stone/schist/australium) plus the
            // bedrock-ore tile-entity lookup here, reporting each via chat as CE's
            // onItemRightClick does.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): CE's Nether-portal easter egg (right-clicking
        // ModBlocks.block_beryllium while holding ModItems.entanglement_kit teleports the player to
        // the Nether) depends on both an item and a block that do not exist in this port yet.
        return InteractionResult.PASS;
    }
}
