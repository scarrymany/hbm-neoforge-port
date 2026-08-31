package com.hbm.blocks.machine.accel;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.accel.AccelBlockEntities;
import com.hbm.blockentity.machine.accel.PaPartBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** CE Albion PA parts: beamline (no GUI), RFC / quadrupole / dipole / source (coil + battery). */
public class PaPartBlock extends BaseEntityBlock {

    public static final MapCodec<PaPartBlock> CODEC = simpleCodec(p -> new PaPartBlock(p, Kind.BEAMLINE));

    public enum Kind { BEAMLINE, RFC, QUADRUPOLE, DIPOLE, SOURCE }

    public final Kind kind;

    public PaPartBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaPartBlockEntity(AccelBlockEntities.typeFor(kind).get(), pos, state, kind);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ITickableBE.ticker();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind == Kind.BEAMLINE) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof PaPartBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }
}
