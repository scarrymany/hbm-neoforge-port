package com.hbm.items.tool;

import com.hbm.blockentity.turret.TurretBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemTurretMobFilter} - shift-right-click on a turret's
 * {@link BlockDummyable} casing to open the mob-filter screen. Resolves the core block entity via
 * {@link BlockDummyable#findCore}, matching CE's own {@code onItemUseFirst}. Unlike CE's
 * {@code player.openGui(...)} round trip (an artifact of 1.12's GUI-id system, not a data-fetch
 * requirement), this port opens {@link com.hbm.inventory.gui.turret.TurretMobFilterScreen} directly
 * client-side - see that screen's own javadoc for why no server round trip is needed just to
 * display it (the block entity's state already reaches the client through its normal sync path).
 */
public class ItemTurretMobFilter extends ItemBase {

    public ItemTurretMobFilter(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof BlockDummyable dummyable)) return InteractionResult.PASS;

        BlockPos corePos = dummyable.findCore(level, pos);
        if (corePos == null) return InteractionResult.FAIL;

        if (!(level.getBlockEntity(corePos) instanceof TurretBaseBlockEntity)) return InteractionResult.PASS;

        if (level.isClientSide()) {
            com.hbm.client.ClientScreens.turretMobFilter(corePos);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Shift-click on turret to open mob filter list").withStyle(ChatFormatting.GRAY));
    }
}
