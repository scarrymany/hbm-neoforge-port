package com.hbm.blocks.machine.dummyable;

import com.hbm.api.block.IToolable;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.HeaterOilburnerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.items.tool.ItemTooling;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CE {@code HeaterOilburner} — Dummyable {1,0,1,1,1,1} offset 1 + 5 extras.
 * Screwdriver/hand-drill burn-rate Exact CE {@code HeaterOilburner.java:120-142}.
 * Tool-skip GUI Exact CE {@code :55-61}. Overlay Exact CE {@code :95-117}.
 * Tooltip Exact CE {@code :88-92}.
 */
public class HeaterOilburnerBlock extends BlockDummyable implements IToolable, ILookOverlay, ITooltipProvider {

    public HeaterOilburnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new HeaterOilburnerBlockEntity(DummyableProcessBlockEntities.HEATER_OILBURNER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.HEATER_OILBURNER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Exact CE HeaterOilburner.java:55-61 — screwdriver/hand-drill must not steal GUI
        if (stack.getItem() instanceof ItemTooling tool
                && (tool.getType() == ToolType.SCREWDRIVER || tool.getType() == ToolType.HAND_DRILL)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = standardOpenBehavior(level, pos, player);
        return result == InteractionResult.FAIL ? ItemInteractionResult.FAIL : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        makeExtra(level, core.east());
        makeExtra(level, core.west());
        makeExtra(level, core.north());
        makeExtra(level, core.south());
        makeExtra(level, core.above());
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER && tool != ToolType.HAND_DRILL)
            return false;
        if (world.isClientSide) return true;

        BlockPos core = findCore(world, new BlockPos(x, y, z));
        if (core == null) return false;
        if (!(world.getBlockEntity(core) instanceof HeaterOilburnerBlockEntity tile)) return false;

        // Exact CE HeaterOilburner.java:135-139
        if (tool == ToolType.SCREWDRIVER)
            tile.toggleSettingUp();
        else
            tile.toggleSettingDown();
        tile.setChanged();
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE HeaterOilburner.java:95-117
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof HeaterOilburnerBlockEntity heater)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(heater.setting + " mB/t").withStyle(ChatFormatting.RESET)));
        FluidType type = heater.tank.getTankType();
        if (type.hasTrait(FT_Flammable.class)) {
            int heat = (int) (type.getTrait(FT_Flammable.class).getHeatEnergy() * heater.setting / 1000);
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(String.format(Locale.US, "%,d", heat) + " TU/t").withStyle(ChatFormatting.RESET)));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE HeaterOilburner.java:88-92
        addStandardInfo(tooltip);
    }
}
