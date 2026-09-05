package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.PWRProxyBlock;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.CapabilityContextProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code BlockPWR.TileEntityBlockPWR} (read in full alongside its outer
 * {@code BlockPWR} class): the shared structural-proxy block entity every non-controller position of
 * an assembled PWR gets replaced with. Stores the position's original {@link BlockState} (for
 * restoration on disassembly/break) and a cached back-pointer to the core controller, and forwards
 * every fluid query to that core when {@link PWRProxyBlock#IO_ENABLED} is set - exactly CE's
 * {@code getCapability}/{@code transferFluid}/{@code getDemand}/{@code canConnect}/{@code getAllTanks}
 * one-line forwards, translated to this port's {@code fluidmk2} API
 * ({@link com.hbm.api.fluidmk2.IFluidReceiverMK2} in place of CE's identical-shaped interface of the
 * same name) and to {@link com.hbm.blockentity.MachineBaseBlockEntity}'s accessor-method capability
 * contract in place of CE's per-instance {@code getCapability} override.
 *
 * <h2>{@link CapabilityContextProvider} - why the forward pushes its own position first</h2>
 * {@link com.hbm.blockentity.MachineBaseBlockEntity#getFluidHandlerCapability} caches its wrapper by
 * "accessor position" (see that class's own javadoc) so a multiblock's several ports can each get a
 * distinct, independently-addressable wrapper off one shared core. Forwarding through the core's own
 * {@code getFluidHandlerCapability} call site without pushing this proxy's position first would make
 * every port collapse onto the core's own position as the accessor key - harmless today (the
 * controller's own {@code getAccessibleSlotsFromSide} does not vary by accessor), but exactly the
 * mechanism a future per-port behavior difference would need, and the documented, intended use of
 * {@link CapabilityContextProvider#pushPos}/{@link CapabilityContextProvider#popPos} (see that
 * class's own javadoc: "a future proxy block entity pushes its own position before delegating").
 *
 * <p>ROR forward: CE {@code BlockPWR.TileEntityBlockPWR}:321-341 (IO_ENABLED gate + core delegate).
 * Not ported: {@code ILookOverlay}/{@code printHook}.
 */
public class PWRProxyBlockEntity extends LoadedBaseBlockEntity
        implements ITickableBE, IFluidReceiverMK2, IRORValueProvider, IRORInteractive {

    @Nullable
    private BlockState originalBlockState;
    @Nullable
    private BlockPos corePos;
    @Nullable
    private PWRControllerBlockEntity cachedCore;

    public PWRProxyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Called once by {@link com.hbm.blocks.machine.MachinePWRControllerBlock#assemble} right after this proxy replaces a structural block. */
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

    private boolean isIoEnabled() {
        return getBlockState().getValue(PWRProxyBlock.IO_ENABLED);
    }

    /** Finds and caches the core {@link PWRControllerBlockEntity}, matching CE's {@code getCore()} caching contract. */
    @Nullable
    private PWRControllerBlockEntity getCore() {
        if (corePos == null || level == null) return null;
        if (cachedCore != null && !cachedCore.isRemoved() && cachedCore.getBlockPos().equals(corePos)) {
            return cachedCore;
        }
        if (level.isLoaded(corePos) && level.getBlockEntity(corePos) instanceof PWRControllerBlockEntity controller) {
            cachedCore = controller;
            return cachedCore;
        }
        cachedCore = null;
        return null;
    }

    /** CE: every 20 ticks, self-destruct if the core is gone or the reactor is no longer assembled. */
    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide || corePos == null) return;
        if (level.getGameTime() % 20 != 0) return;

        PWRControllerBlockEntity controller = getCore();
        if (controller == null || !controller.assembled) {
            level.removeBlock(this.worldPosition, false);
        }
    }

    @Nullable
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        if (!isIoEnabled()) return null;
        PWRControllerBlockEntity core = getCore();
        if (core == null) return null;

        BlockPos prev = CapabilityContextProvider.pushPos(this.worldPosition);
        try {
            return core.getFluidHandlerCapability(side);
        } finally {
            CapabilityContextProvider.popPos(prev);
        }
    }

    public boolean hasFluidHandlerCapability() {
        return isIoEnabled() && getCore() != null;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long fluid) {
        if (!isIoEnabled()) return fluid;
        PWRControllerBlockEntity core = getCore();
        return core != null ? core.transferFluid(type, pressure, fluid) : fluid;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        if (!isIoEnabled()) return 0;
        PWRControllerBlockEntity core = getCore();
        return core != null ? core.getDemand(type, pressure) : 0;
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        if (!isIoEnabled()) return false;
        PWRControllerBlockEntity core = getCore();
        return core == null || core.canConnect(type, dir);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        if (!isIoEnabled()) return List.of();
        PWRControllerBlockEntity core = getCore();
        return core != null ? core.getAllTanks() : List.of();
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
            // `registries` (not `this.level`, which is not guaranteed set yet at load time - the
            // BlockEntity is deserialized before setLevel runs) is exactly the HolderLookup.Provider
            // NbtUtils.readBlockState needs; see MachineBaseBlockEntity's own loadAdditional javadoc
            // for the same "registries argument, not a level field" rationale.
            this.originalBlockState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("originalBlockState"));
        }
        if (tag.contains("coreX")) {
            this.corePos = new BlockPos(tag.getInt("coreX"), tag.getInt("coreY"), tag.getInt("coreZ"));
        }
        this.cachedCore = null;
    }

    @Override
    public String[] getFunctionInfo() {
        // CE BlockPWR.java:321-325
        if (!isIoEnabled()) return new String[0];
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.getFunctionInfo();
        return new String[0];
    }

    @Override
    public String provideRORValue(String name) {
        // CE :329-333
        if (!isIoEnabled()) return "";
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.provideRORValue(name);
        return "";
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :337-341
        if (!isIoEnabled()) return "";
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.runRORFunction(name, params);
        return "";
    }
}
