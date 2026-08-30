package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.DetCord} (111 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. One class, five registered
 * instances ({@link BombBlocks#DET_CORD} through {@link BombBlocks#DET_BALE}) differentiated by
 * identity exactly like CE's own {@code this == ModBlocks.det_cord} chain. Every one of the 5
 * branches is fully wired - the two nuke-tier branches ({@code det_n2}/{@code det_nuke}) and the
 * balefire branch depend on {@link EntityNukeExplosionMK5}/{@link EntityBalefire}, both already
 * committed in this port's foundation wave (the research report's Section-B blocker no longer
 * applies to this specific package's needs).
 */
public class DetCord extends Block implements IBomb {

    public DetCord(Properties properties) {
        super(properties);
    }

    /** CE: {@code onExplosionDestroy} - caught in another explosion, detonates using that explosion's own source entity. */
    @Override
    public void wasExploded(Level level, BlockPos pos, @Nullable Explosion explosion) {
        explode(level, pos, explosion != null ? explosion.getIndirectSourceEntity() : null);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }

    /** CE: {@code getItemDropped} returns {@code Items.AIR} - never drops when mined normally either. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        if (this == BombBlocks.DET_CORD.get()) {
            level.explode(detonator, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.5F, true, Level.ExplosionInteraction.TNT);
        }
        if (this == BombBlocks.DET_CHARGE.get()) {
            ExplosionLarge.explode(level, detonator, pos.getX(), pos.getY(), pos.getZ(), 20, true, false, false);
        }
        if (this == BombBlocks.DET_N2.get()) {
            int radius = (BombConfig.N2_RADIUS.get() / 12) * 5;
            level.addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level, radius, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).setDetonator(detonator));
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFac(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius);
            }
        }
        if (this == BombBlocks.DET_NUKE.get()) {
            level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, BombConfig.MISSILE_RADIUS.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).setDetonator(detonator));
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFac(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BombConfig.MISSILE_RADIUS.get());
            }
        }
        if (this == BombBlocks.DET_BALE.get()) {
            EntityBalefire bf = new EntityBalefire(NukeEntityTypes.BALEFIRE.get(), level);
            bf.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            bf.destructionRange = 130;
            bf.setDetonator(detonator);
            level.addFreshEntity(bf);
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFacBale(level, pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5, 130F);
            }
        }

        return BombReturnCode.DETONATED;
    }
}
