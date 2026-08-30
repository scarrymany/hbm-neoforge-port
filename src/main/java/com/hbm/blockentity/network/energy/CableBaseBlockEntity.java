package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.energy.TileEntityCableBaseNT} (the conductor
 * root every cable/wire/box tile entity in that package either is or extends, read in full). Package
 * root is {@code com.hbm.blockentity.network.energy} (CE's {@code tileentity.network.energy} with the
 * port's established {@code tileentity}-&gt;{@code blockentity} rename, matching this port's own
 * de-facto choice - see {@code BlockDummyable}'s already-landed {@code com.hbm.blockentity.IPersistentNBT}
 * import and {@code LoadedBaseBlockEntity}'s own javadoc).
 *
 * <p>Per the research report's own "Key design/API decisions": node registration/teardown is a
 * mechanical transcription onto Phase 0's already-verified {@code Nodespace}/{@code UniNodespace}
 * API, not new graph logic - {@link #updateEntity()} is CE's {@code update()} body unchanged bar the
 * {@code world}-&gt;{@code level} rename and the {@code ITickable}-&gt;{@link ITickableBE} interface
 * swap; {@link #setRemoved()} replaces CE's {@code invalidate()} override (1.21's confirmed removal
 * hook, see {@link net.minecraft.world.level.block.entity.BlockEntity#setRemoved()}).
 *
 * <p><b>Deliberately not ported</b>: CE's Forge/NeoForge-Energy (FE) bridge half of this class
 * ({@code refreshFENeighbors}/{@code handleFETransfers}, gated by
 * {@code GeneralConfig.autoCableConversion}) - the research report's own "Deferred scope" flags this
 * as capability-<i>registration</i> work with no established {@code RegisterCapabilitiesEvent}
 * convention anywhere in this port yet, explicitly "not a blocker: the HE-only path works with zero
 * capability registration." The connection-mask caching half of CE's class
 * ({@code getCachedConnectionMask}/{@code invalidateConnectionCache}, backing 1.12's
 * {@code IExtendedBlockState}-based render mask) is also dropped: per the report's own "dynamic
 * connection-mask block state" decision, that entire mechanism is replaced by ordinary listed
 * {@code BooleanProperty} state on the owning {@link com.hbm.blocks.network.energy.BlockCable}/
 * {@link com.hbm.blocks.network.energy.PowerCableBoxBlock}, recomputed directly in
 * {@code updateShape} - the block's blockstate <i>is</i> the cache, so this tile entity needs none of
 * its own.
 */
public class CableBaseBlockEntity extends LoadedBaseBlockEntity implements IEnergyConductorMK2, ITickableBE {

    protected Nodespace.PowerNode node;

    public CableBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Gate on whether this conductor should hold a live node at all - overridden by
     * {@link CableSwitchBlockEntity} (off state = no node, matching CE's
     * {@code TileEntityCableSwitch}). Always {@code true} for a plain cable.
     */
    public boolean shouldCreateNode() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (this.node == null || this.node.expired) {
            if (this.shouldCreateNode()) {
                this.node = Nodespace.getNode(level, worldPosition);

                if (this.node == null || this.node.expired) {
                    this.node = this.createNode();
                    Nodespace.createNode(level, this.node);
                }
            }
        }
    }

    /**
     * 1.21's confirmed removal hook (see this class's javadoc) - {@code super.setRemoved()} first,
     * matching every other {@code setRemoved} override in this port, then CE's own
     * {@code invalidate()} node-teardown body.
     */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && this.node != null) {
            Nodespace.destroyNode(level, worldPosition);
        }
    }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != null;
    }
}
