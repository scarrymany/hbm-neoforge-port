package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.container.machine.dummyable.HeaterOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityHeaterOven.java}:26-75 — firebox with baseHeat 500, timeMult 0.125,
 * maxHeat 500_000, plus 50% pull from the {@code IHeatSource} below.
 */
public class HeaterOvenBlockEntity extends HeaterFireboxBlockEntity {

    public static final int OVEN_BASE_HEAT = 500;
    public static final int OVEN_MAX_HEAT = 500_000;
    public static final double TIME_MULT = 0.125D;
    public static final double HEAT_EFF = 0.5D;

    public HeaterOvenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterOven");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        tryPullHeat();
        tickBurn(OVEN_BASE_HEAT, TIME_MULT, OVEN_MAX_HEAT);
        dataChanged();
        networkPackMK2(50);
    }

    private void tryPullHeat() {
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source && below != this) {
            int room = OVEN_MAX_HEAT - heatEnergy;
            int toPull = Math.max(Math.min(source.getHeatStored(), room), 0);
            if (toPull > 0) {
                heatEnergy += (int) (toPull * HEAT_EFF);
                source.useUpHeat(toPull);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeaterOvenMenu(id, inv, this);
    }
}
