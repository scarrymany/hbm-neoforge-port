package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.Nodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Ported from CE's {@code TileEntityCableSwitch} (shared, unchanged, by both
 * {@link com.hbm.blocks.network.energy.CableSwitchBlock} and
 * {@link com.hbm.blocks.network.energy.CableDetectorBlock} - CE's own comment on the block class:
 * "same TE" for both). Whether the node exists at all is gated on the block's own {@code STATE}
 * boolean property (CE's meta 0/1 off/on) rather than an internal field, since {@code updateState()}
 * needs to read the current blockstate synchronously off a right-click/redstone-edge trigger, not
 * wait for the next tick.
 */
public class CableSwitchBlockEntity extends CableBaseBlockEntity {

    public CableSwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The boolean blockstate property both {@code CableSwitchBlock} and {@code CableDetectorBlock} key on. */
    private BooleanProperty stateProperty() {
        return com.hbm.blocks.network.energy.CableSwitchBlock.STATE;
    }

    private boolean isOn() {
        BlockState state = getBlockState();
        return state.hasProperty(stateProperty()) && state.getValue(stateProperty());
    }

    @Override
    public boolean shouldCreateNode() {
        return isOn();
    }

    /**
     * Called by the owning block right after it flips the {@code STATE} property (on right-click for
     * {@code CableSwitchBlock}, on a redstone edge for {@code CableDetectorBlock}). Ported from CE's
     * {@code TileEntityCableSwitch.updateState()}: if the switch is now off (meta 0, node present),
     * destroy and de-reference the node immediately rather than waiting for {@link #updateEntity()}
     * to notice - {@link #shouldCreateNode()} already prevents a new node from being created while
     * off, this only handles the "was already on" teardown case.
     */
    public void updateState() {
        if (!isOn() && this.node != null) {
            if (level != null && !level.isClientSide) {
                Nodespace.destroyNode(level, worldPosition);
            }
            this.node = null;
        }
    }
}
