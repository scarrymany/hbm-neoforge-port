package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.NukeEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code NukeFleija} (248 lines, read in full). Same yaw-derived facing quirk as
 * {@code NukeManBlock} - see that class's javadoc for the {@code getCounterClockWise()} derivation.
 */
public class NukeFleijaBlock extends NukeCasingBlockBase {

    public NukeFleijaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getCounterClockWise());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeFleijaBlockEntity(NukeCasingBlockEntities.NUKE_FLEIJA.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    private void igniteTestBomb(Level level, @Nullable Entity detonator, BlockPos pos) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        int radius = BombConfig.FLEIJA_RADIUS.get();
        EntityNukeExplosionMK3 entity = new EntityNukeExplosionMK3(NukeEntityTypes.NUKE_MK3.get(), level);
        entity.setPos(x, y, z);
        if (detonator != null) {
            entity.setDetonator(detonator);
        } else if (level.getBlockEntity(pos) instanceof NukeFleijaBlockEntity fleija) {
            entity.detonator = fleija.placerID;
        }
        if (!EntityNukeExplosionMK3.isJammed(level, entity)) {
            entity.destructionRange = radius;
            entity.speed = BombConfig.BLAST_SPEED.get();
            entity.coefficient = 1.0F;
            entity.waste = false;

            level.addFreshEntity(entity);
            level.addFreshEntity(EntityCloudFleija.create(level, x, y, z, radius));
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof NukeFleijaBlockEntity be && be.isReady()) {
            be.clearSlots();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            igniteTestBomb(level, detonator, pos);
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }
}
