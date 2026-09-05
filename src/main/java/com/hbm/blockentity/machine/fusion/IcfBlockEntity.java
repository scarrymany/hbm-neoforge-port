package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.fusion.IcfBlock;
import com.hbm.capability.NTMEnergyCapabilityWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Exact CE {@code BlockICF.TileEntityBlockICF} — IO-proxy to {@link IcfControllerBlockEntity}.
 * Energy forward only when {@code IO_ENABLED}. Tick: vanish if core gone / not assembled
 * ({@code :105-111}); restore happens in {@link IcfBlock#onRemove}.
 */
public class IcfBlockEntity extends LoadedBaseBlockEntity implements ITickableBE, IEnergyReceiverMK2 {

    @Nullable
    private BlockState originalBlockState;
    @Nullable
    private BlockPos corePos;
    @Nullable
    private IcfControllerBlockEntity cachedCore;

    public IcfBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setOriginal(BlockState originalBlockState, BlockPos corePos) {
        this.originalBlockState = originalBlockState;
        this.corePos = corePos;
        this.cachedCore = null;
        setChanged();
    }

    @Nullable
    public BlockState getOriginalBlockState() {
        return this.originalBlockState;
    }

    @Nullable
    public BlockPos getCorePos() {
        return this.corePos;
    }

    public boolean isIoEnabled() {
        return getBlockState().getValue(IcfBlock.IO_ENABLED);
    }

    @Nullable
    private IcfControllerBlockEntity getCore() {
        // CE BlockICF.java:115-129
        if (corePos == null || level == null) return null;
        if (cachedCore != null && !cachedCore.isRemoved() && cachedCore.getBlockPos().equals(corePos)) {
            return cachedCore;
        }
        if (level.isLoaded(corePos) && level.getBlockEntity(corePos) instanceof IcfControllerBlockEntity controller) {
            cachedCore = controller;
            return cachedCore;
        }
        cachedCore = null;
        return null;
    }

    @Override
    public void updateEntity() {
        // CE BlockICF.java:105-111
        if (level == null || level.isClientSide || corePos == null) return;
        if (level.getGameTime() % 20 != 0) return;
        IcfControllerBlockEntity controller = getCore();
        if (controller == null || !controller.assembled) {
            level.removeBlock(this.worldPosition, false);
        }
    }

    @Nullable
    public IEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        if (!isIoEnabled()) return null;
        return getCore() != null ? new NTMEnergyCapabilityWrapper(this, this.worldPosition) : null;
    }

    @Override
    public long getPower() {
        // CE BlockICF.java:182-187
        if (!isIoEnabled() || originalBlockState == null) return 0;
        IcfControllerBlockEntity controller = getCore();
        return controller != null ? controller.getPower() : 0;
    }

    @Override
    public void setPower(long power) {
        // CE BlockICF.java:191-196
        if (!isIoEnabled() || originalBlockState == null) return;
        IcfControllerBlockEntity controller = getCore();
        if (controller != null) controller.setPower(power);
    }

    @Override
    public long getMaxPower() {
        // CE BlockICF.java:199-204
        if (!isIoEnabled() || originalBlockState == null) return 0;
        IcfControllerBlockEntity controller = getCore();
        return controller != null ? controller.getMaxPower() : 0;
    }

    @Override
    public boolean canConnect(Direction dir) {
        // CE BlockICF.java:221-224
        if (!isIoEnabled()) return false;
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (originalBlockState != null) {
            tag.put("originalBlockState", NbtUtils.writeBlockState(originalBlockState));
        }
        if (corePos != null) {
            tag.putInt("coreX", corePos.getX());
            tag.putInt("coreY", corePos.getY());
            tag.putInt("coreZ", corePos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("originalBlockState")) {
            this.originalBlockState = NbtUtils.readBlockState(
                    registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("originalBlockState"));
        }
        if (tag.contains("coreX")) {
            this.corePos = new BlockPos(tag.getInt("coreX"), tag.getInt("coreY"), tag.getInt("coreZ"));
        }
        this.cachedCore = null;
    }
}
