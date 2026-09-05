package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineSteamEngineBlockEntity;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ported from CE's {@code MachineSteamEngine} (regname {@code machine_steam_engine}): a
 * {@link BlockDummyable} multiblock, {@code {1,0,5,1,1,1}} dimensions, offset 1. No GUI, no
 * inventory - only the core block position (meta 12-15) carries a block entity; every dummy
 * position (meta 0-11) has none, matching CE's plain-dummy range (CE's "extra"-flagged dummies carry
 * a capability-forwarding proxy tile entity, {@code TileEntityProxyCombo}, which this port does not
 * have - see the research report's simplification note on this class's block-entity javadoc: the
 * core's own fixed connector-position math already reaches the right neighbor blocks directly,
 * without needing the dummy itself to forward anything). fillSpace extras Exact CE {@code :49-59}.
 * printHook Exact CE {@code :74-108}. Tooltip Exact CE {@code :64-71}.
 */
public class MachineSteamEngineBlock extends BlockDummyable implements ILookOverlay, ITooltipProvider {

    public MachineSteamEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 5, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * Exact CE {@code MachineSteamEngine.fillSpace} extras ({@code MachineSteamEngine.java:49-59}).
     * After {@code super.fillSpace}: add {@code dir * o} (core), {@code rot = dir} clockwise around Y,
     * then three extras at {@code y+1} along the rot face — {@code core+rot}, {@code core+rot+dir},
     * {@code core+rot-dir}. No ProxyCombo TE — extras are {@code makeExtra} flags only.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(rot).above());
        makeExtra(level, core.relative(rot).relative(dir).above());
        makeExtra(level, core.relative(rot).relative(dir.getOpposite()).above());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineSteamEngineBlockEntity(PowerGenBlockEntities.STEAM_ENGINE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.STEAM_ENGINE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineSteamEngine.java:74-108 — green steam / red spentsteam, %,d fill / max
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof MachineSteamEngineBlockEntity engine)) return;

        List<Component> text = new ArrayList<>();
        FluidTankNTM in = engine.tanks[0];
        FluidTankNTM out = engine.tanks[1];
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(in.getTankType().getLocalizedName())
                        .append(Component.literal(": "
                                + String.format(Locale.US, "%,d", in.getFill())
                                + " / "
                                + String.format(Locale.US, "%,d", in.getMaxFill())
                                + "mB"))));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(out.getTankType().getLocalizedName())
                        .append(Component.literal(": "
                                + String.format(Locale.US, "%,d", out.getFill())
                                + " / "
                                + String.format(Locale.US, "%,d", out.getMaxFill())
                                + "mB"))));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE MachineSteamEngine.java:64-71 — addStandardInfo via existing block.hbm.machine_steam_engine.desc
        addStandardInfo(tooltip);
    }
}
