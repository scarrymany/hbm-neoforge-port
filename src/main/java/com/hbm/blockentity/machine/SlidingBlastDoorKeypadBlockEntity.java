package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntitySlidingBlastDoorKeypad}. Live TE so keypad extras exist;
 * keypad mesh / password leftover
 * TODO(CE: TileEntitySlidingBlastDoorKeypad.java:26-87): KeypadClient + keypadActivated/passwordSet.
 */
public class SlidingBlastDoorKeypadBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    public SlidingBlastDoorKeypadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        // CE client finds core and builds KeypadClient matrix. No Keypad in this port.
    }

    /** CE {@code keypadActivated} — toggle core. Call site is KeypadClient leftover. */
    public void keypadActivated() {
        if (level == null || !(getBlockState().getBlock() instanceof BlockDummyable dummyable)) return;
        var core = dummyable.findCoreBlockEntity(level, worldPosition);
        if (core instanceof SlidingBlastDoorBlockEntity door) door.toggle();
    }
}
