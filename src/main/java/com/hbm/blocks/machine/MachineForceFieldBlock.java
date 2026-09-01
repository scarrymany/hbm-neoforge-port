package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineForceFieldBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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

/**
 * CE {@code MachineForceField} — 1×1 BlockContainer, not Dummyable.
 * Hardness 5 / resistance 100, missile tab.
 * TODO(CE: MachineForceField.java:83): ENTITYBLOCK_ANIMATED.
 * TODO(CE: RenderMachineForceField.java:20): TESR. Cube model stays.
 */
public class MachineForceFieldBlock extends BaseEntityBlock {

    public static final MapCodec<MachineForceFieldBlock> CODEC = simpleCodec(MachineForceFieldBlock::new);

    public MachineForceFieldBlock(Properties properties) {
        super(properties);
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
        return new MachineForceFieldBlockEntity(DummyableProcessBlockEntities.MACHINE_FORCEFIELD.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_FORCEFIELD.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineForceFieldBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!(level.getBlockEntity(pos) instanceof MachineForceFieldBlockEntity te)) return;
        if (te.isOn && te.cooldown == 0 && te.power > 0) {
            if (te.color == 0xFF0000) {
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.LAVA,
                            pos.getX() + rand.nextFloat(), pos.getY() + 2F, pos.getZ() + rand.nextFloat(),
                            0.0D, 0.0D, 0.0D);
                }
            }
        } else if (te.cooldown > 0) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.SMOKE,
                        pos.getX() + rand.nextFloat(), pos.getY() + 2F, pos.getZ() + rand.nextFloat(),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }
}
