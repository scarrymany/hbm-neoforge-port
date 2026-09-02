package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Port of CE {@code com.hbm.tileentity.machine.TileEntityCargoElevator} - hydraulic lift platform logic.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityCargoElevator.java
 * <p>
 * TODO(CE): Full lift behavior - height storage (CE :26), extension animation (CE :28-30, :63-70),
 * entity lifting (CE :83-97), toggleElevator (CE :100-108), merging with lower elevator (CE :43-60),
 * client sync (CE :111-125), ROR integration. Current port: placeholder tick for registration.
 */
public class CargoElevatorBlockEntity extends BlockEntity {

    public int height = 0; // CE :26 - number of additional blocks above base
    public int targetExtension = 0; // CE :28 - target platform height
    public double extension = 0; // CE :29 - current platform height (interpolated)
    public double prevExtension = 0; // CE :30 - for rendering interpolation

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CARGO_ELEVATOR_ENTITY.get(), pos, state);
    }

    public void serverTick() {
        // TODO(CE): Implement CE :39-98 update() logic (extension animation, entity lifting, lower elevator merging)
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.height = tag.getInt("height");
        this.targetExtension = tag.getInt("targetExtension");
        this.extension = tag.getDouble("extension");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("height", height);
        tag.putInt("targetExtension", targetExtension);
        tag.putDouble("extension", extension);
    }
}
