package com.hbm.blocks.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blockentity.bomb.NukeCustomBlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityCloudSolinium;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.explosion.ExplosionLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code NukeCustom} (298 lines, read in full) - the modular casing. {@link #explodeCustom}
 * is CE's tiered priority chain (euphemium &gt; solinium &gt; schrabidium &gt; antimatter &gt;
 * hydrogen &gt; nuclear &gt; non-nuclear), each tier additively pulling in the tier below's yield
 * (see {@code NukeCustomBlockEntity#updateEntity} for how those additive values are computed) and
 * spawning a different entity. The euphemium tier is a documented forward reference - {@code
 * com.hbm.explosion.ExplosionChaos} (CE's {@code ExplosionChaos.zomg}) is confirmed absent from this
 * port and out of this bomb-casing package's scope to introduce; every other tier is fully wired.
 */
public class NukeCustomBlock extends NukeCasingBlockBase {

    public NukeCustomBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeCustomBlockEntity(NukeCasingBlockEntities.NUKE_CUSTOM.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == NukeCasingBlockEntities.NUKE_CUSTOM.get() ? ITickableBE.ticker() : null;
    }

    /**
     * CE: {@code NukeCustom.explodeCustom(...)} static helper - the exact call shape
     * {@code EntityFallingNuke}'s own documented TODO already names, so that class's air-dropped mode
     * is wired up here too (see this method's last call site below).
     */
    public static void explodeCustom(Level level, Entity detonator, double x, double y, double z,
                                      float tnt, float nuke, float hydro, float bale, float dirty, float schrab, float sol, float euph) {
        dirty = Math.min(dirty, BombConfig.MAX_CUSTOM_DIRTY_RADIUS.get());

        if (euph > 0) {
            // TODO(com.hbm.explosion.ExplosionChaos, confirmed absent from this port): CE calls
            // ExplosionChaos.zomg(world, x, y, z, (int)(100 * euph), detonator, null) here - the
            // "anti-mass" euphemium tier is a documented forward reference, out of this bomb-casing
            // package's scope (a whole separate explosion-helper class, not narrowly related to
            // casings/detonators).
        } else if (sol > 0) {
            sol += schrab / 2 + bale / 4 + hydro / 8 + nuke / 16 + tnt / 32;
            sol = Math.min(sol, BombConfig.MAX_CUSTOM_SOL_RADIUS.get());

            EntityNukeExplosionMK3 entity = new EntityNukeExplosionMK3(NukeEntityTypes.NUKE_MK3.get(), level);
            entity.setPos(x + 0.5, y + 0.5, z + 0.5);
            entity.destructionRange = (int) sol;
            entity.speed = BombConfig.BLAST_SPEED.get();
            entity.coefficient = 1.0F;
            entity.waste = false;
            entity.extType = 1;
            entity.setDetonator(detonator);
            level.addFreshEntity(entity);

            level.addFreshEntity(EntityCloudSolinium.create(level, x + 0.5, y + 0.5, z + 0.5, (int) sol));
        } else if (schrab > 0) {
            schrab += bale / 2 + hydro / 4 + nuke / 8 + tnt / 16;
            schrab = Math.min(schrab, BombConfig.MAX_CUSTOM_SCHRAB_RADIUS.get());

            EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(level, x + 0.5, y + 0.5, z + 0.5, (int) schrab);
            ex.setDetonator(detonator);
            if (!ex.isRemoved()) {
                level.addFreshEntity(ex);
                level.addFreshEntity(EntityCloudFleija.create(level, x + 0.5, y + 0.5, z + 0.5, (int) schrab));
            }
        } else if (bale > 0) {
            bale += hydro / 2 + nuke / 4 + tnt / 8;
            bale = Math.min(bale, BombConfig.MAX_CUSTOM_BALE_RADIUS.get());

            EntityBalefire bf = new EntityBalefire(NukeEntityTypes.BALEFIRE.get(), level);
            bf.setPos(x + 0.5, y + 0.5, z + 0.5);
            bf.destructionRange = (int) bale;
            bf.setDetonator(detonator);
            level.addFreshEntity(bf);
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFacBale(level, x + 0.5, y + 5, z + 0.5, bale);
            }
        } else if (hydro > 0) {
            hydro += nuke / 2 + tnt / 4;
            hydro = Math.min(hydro, BombConfig.MAX_CUSTOM_HYDRO_RADIUS.get());
            dirty *= 0.25F;

            level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, (int) hydro, x + 0.5, y + 0.5, z + 0.5).moreFallout((int) dirty).setDetonator(detonator));
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, hydro);
            }
        } else if (nuke > 0) {
            nuke += tnt / 2;
            nuke = Math.min(nuke, BombConfig.MAX_CUSTOM_NUKE_RADIUS.get());

            level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, (int) nuke, x + 0.5, y + 5, z + 0.5).moreFallout((int) dirty).setDetonator(detonator));
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, nuke);
            }
        } else if (tnt >= 75) {
            tnt = Math.min(tnt, BombConfig.MAX_CUSTOM_TNT_RADIUS.get());

            level.addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level, (int) tnt, x + 0.5, y + 0.5, z + 0.5).setDetonator(detonator));
            if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, tnt);
            }
        } else if (tnt > 0) {
            ExplosionLarge.explode(level, detonator, x + 0.5, y + 0.5, z + 0.5, tnt, true, true, true);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof NukeCustomBlockEntity be)) return BombReturnCode.UNDEFINED;

        Entity resolvedDetonator = detonator;
        if (resolvedDetonator == null && be.placerID != null && level.getServer() != null) {
            resolvedDetonator = level.getServer().getPlayerList().getPlayer(be.placerID);
        }

        if (!be.isFalling()) {
            be.clearSlots();
            level.destroyBlock(pos, false);
            explodeCustom(level, resolvedDetonator, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    be.tnt, be.nuke, be.hydro, be.bale, be.dirty, be.schrab, be.sol, be.euph);
            return BombReturnCode.TRIGGERED;
        }

        EntityFallingNuke bomb = new EntityFallingNuke(level, resolvedDetonator, be.tnt, be.nuke, be.hydro, be.bale, be.dirty, be.schrab, be.sol, be.euph);
        bomb.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        be.clearSlots();
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.addFreshEntity(bomb);
        return BombReturnCode.TRIGGERED;
    }
}
