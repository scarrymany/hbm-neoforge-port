package com.hbm.blockentity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;

/**
 * Marker interface for {@link BlockEntity} subclasses that need per-tick server logic, ported from
 * CE's {@code com.hbm.tileentity.ITickable} (CE's own pre-1.13 replacement for vanilla's
 * {@code net.minecraft.util.ITickable}, which no longer exists in modern Minecraft either - ticking
 * is now supplied by the owning {@code Block} via
 * {@link net.minecraft.world.level.block.EntityBlock#getTicker}, not implemented directly on the
 * block entity). Renamed to {@code ITickableBE} (matching Neo Edition's separate {@code ITickable}
 * interface in spirit, kept distinct here to avoid any reader confusion with
 * {@link BlockEntityTicker}, the unrelated functional interface {@code getTicker} actually returns).
 *
 * <p>This lives on {@link LoadedBaseBlockEntity} subclasses as an opt-in marker rather than on the
 * base class itself, matching CE: {@code TileEntityLoadedBase} does not implement
 * {@code ITickable}, only the ~228 CE tile entities (a mix of both hierarchy tiers) that actually
 * need per-tick logic do, via {@code implements ITickable}. A block entity that never ticks (most of
 * the ~80 direct {@code TileEntityLoadedBase} subclasses) pays nothing for this - it simply doesn't
 * implement the interface, and {@link #ticker()} then returns {@code null} for it.
 *
 * <p><b>Usage</b> - every future machine {@code Block} (an {@code EntityBlock}, typically
 * {@code extends BaseEntityBlock}) implements its own {@code getTicker} as a one-liner against
 * this shared helper, instead of hand-rolling the {@code instanceof} check per block:
 * <pre>{@code
 * @Override
 * public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
 *     return type == MY_MACHINE_ENTITY_TYPE.get() ? ITickableBE.ticker() : null;
 * }
 * }</pre>
 * Matches the confirmed idiom read from this port's neighborhood (Neo Edition's
 * {@code MachinePressBlock}, cross-checked for API shape only): {@code updateEntity()} is called
 * unconditionally, on both logical sides, exactly like CE's own {@code ITickable.update()} call
 * site - implementations are themselves responsible for gating server-only logic (CE's own
 * {@code TileEntityMachinePress.update()} does {@code if (!world.isRemote) { ... }} internally, it
 * is not the ticker's job to filter that).
 */
public interface ITickableBE {

    void updateEntity();

    /**
     * Shared {@link BlockEntityTicker} factory. Returns a ticker that calls
     * {@link #updateEntity()} on any block entity that implements this interface. One static
     * instance would work just as well (the lambda captures nothing), but a fresh generic-typed
     * instance per {@code getTicker} call keeps every call site trivially type-correct without an
     * unchecked cast, matching how {@code BaseEntityBlock.createTickerHelper} itself is used
     * elsewhere in NeoForge.
     */
    static <T extends BlockEntity> BlockEntityTicker<T> ticker() {
        return (level, pos, state, be) -> {
            if (be instanceof ITickableBE tickable) tickable.updateEntity();
        };
    }
}
