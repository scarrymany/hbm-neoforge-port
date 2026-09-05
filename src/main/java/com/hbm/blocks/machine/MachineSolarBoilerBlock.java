package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blockentity.machine.SolarBoilerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * Ported from CE's {@code MachineSolarBoiler} (regname {@code machine_solar_boiler}). No GUI, no
 * inventory - a pure fluid producer fed externally by a {@link com.hbm.blocks.machine.SolarMirrorBlock}.
 * fillSpace extras Exact CE {@code :50-56}. printHook Exact CE {@code :66-85}.
 */
public class MachineSolarBoilerBlock extends BlockDummyable implements ILookOverlay {

    public MachineSolarBoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * Exact CE {@code MachineSolarBoiler.fillSpace} extras ({@code MachineSolarBoiler.java:50-56}).
     * After {@code super.fillSpace} (AABB only): one top extra at {@code core.y+2}. CE first adds
     * {@code dir * o} then {@code makeExtra(x, y+2, z)}. No ProxyCombo TE — extras are {@code makeExtra}
     * flags only (same as HeatBoiler / industrial boiler).
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        makeExtra(level, placedPos.relative(dir, placementOffset).above(2));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new SolarBoilerBlockEntity(PowerGenBlockEntities.SOLAR_BOILER.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.SOLAR_BOILER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineSolarBoiler.java:66-85 — heat + heatInput TU/t; tanks skip Fluids.NONE
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof SolarBoilerBlockEntity heater)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(String.format(Locale.US, "%,d", heater.heat) + " TU"));
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(Component.literal(String.format(Locale.US, "%,d", heater.heatInput) + " TU/t"))));
        FluidTankNTM water = heater.tanks[0];
        FluidTankNTM steam = heater.tanks[1];
        if (water.getTankType() != Fluids.NONE) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(water.getTankType().getLocalizedName())
                            .append(Component.literal(": " + water.getFill() + "/" + water.getMaxFill() + "mB"))));
        }
        if (steam.getTankType() != Fluids.NONE) {
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(steam.getTankType().getLocalizedName())
                            .append(Component.literal(": " + steam.getFill() + "/" + steam.getMaxFill() + "mB"))));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
