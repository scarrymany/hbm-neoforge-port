package com.hbm.blocks.turret;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.blockentity.turret.TurretHIMARSBlockEntity;
import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code TurretHIMARS} — Dummyable {@code {1,0,2,1,2,1}} offset 1.
 * TODO(CE: TurretHIMARS.java:25): TileEntityProxyCombo(true,true,false) on extras.
 * TODO(CE: RenderTurretHIMARS.java:1): OBJ TESR {@code turret_himars.obj} Carriage/Launcher/Crane/tubes.
 */
public class TurretHIMARSBlock extends BlockDummyable {

    public TurretHIMARSBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 2, 1, 2, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new TurretHIMARSBlockEntity(TurretBlockEntities.HIMARS.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TurretBlockEntities.HIMARS.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }
}
