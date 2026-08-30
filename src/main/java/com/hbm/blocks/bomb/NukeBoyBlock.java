package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeBoyBlockEntity;
import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Ported from CE's {@code NukeBoy} (215 lines, read in full). */
public class NukeBoyBlock extends NukeCasingBlockBase {

    public NukeBoyBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeBoyBlockEntity(NukeCasingBlockEntities.NUKE_BOY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    /** CE: {@code igniteTestBomb} - plays the boom sound, spawns the mk5 shot (falls back to {@code placerID} when {@code detonator == null}), and spawns the mushroom cloud VFX entity. */
    private void igniteTestBomb(Level level, @Nullable Entity detonator, BlockPos pos) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        int radius = BombConfig.BOY_RADIUS.get();
        EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.statFac(level, radius, x, y, z);
        if (detonator != null) {
            mk5.setDetonator(detonator);
        } else if (level.getBlockEntity(pos) instanceof NukeBoyBlockEntity boy) {
            mk5.detonator = boy.placerID;
        }
        level.addFreshEntity(mk5);

        if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
            EntityNukeTorex.statFac(level, x, y, z, radius);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof NukeBoyBlockEntity be && be.isReady()) {
            be.clearSlots();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            igniteTestBomb(level, detonator, pos);
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }
}
