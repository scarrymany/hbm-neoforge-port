package com.hbm.blockentity.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.bomb.Landmine;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.tileentity.bomb.TileEntityLandmine} (111 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Two-phase proximity trigger:
 * {@link #waitingForPlayer} mode uses a wide (25-block) box that only clears on player presence (a
 * "safety" un-prime, used when the mine is placed with someone already standing on it - CE itself
 * never fully wires the trigger for entering this mode either, preserved as-is per the research
 * report); otherwise a {@code range}/{@code height}-sized box (from the paired {@link Landmine}
 * instance's own fields, doubled while not yet primed) scans for any {@link LivingEntity} (bats
 * excluded) - a first pass without {@link #isPrimed} just plays a click and arms; a later pass with
 * {@link #isPrimed} detonates, using the triggering entity itself as the detonator.
 */
public class LandmineBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    private boolean isPrimed = false;
    public boolean waitingForPlayer = false;

    public LandmineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide()) return;

        if (!(level.getBlockState(worldPosition).getBlock() instanceof Landmine landmine)) return;

        double range = landmine.range;
        double height = landmine.height;

        if (waitingForPlayer) {
            range = 25;
            height = 25;
        } else if (!isPrimed) {
            range *= 2;
            height *= 2;
        }

        AABB box = new AABB(
                worldPosition.getX() - range, worldPosition.getY() - height, worldPosition.getZ() - range,
                worldPosition.getX() + range + 1, worldPosition.getY() + height, worldPosition.getZ() + range + 1);

        List<Entity> list = level.getEntitiesOfClass(Entity.class, box);

        for (Entity entity : list) {
            if (entity instanceof Bat) continue;

            if (waitingForPlayer) {
                if (entity instanceof Player) {
                    waitingForPlayer = false;
                    return;
                }
            } else if (entity instanceof LivingEntity) {
                if (isPrimed) {
                    landmine.explode(level, worldPosition, entity);
                }
                return;
            }
        }

        if (!isPrimed && !waitingForPlayer) {
            level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    HBMSoundHandler.fstbmbStart.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            isPrimed = true;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isPrimed = tag.getBoolean("primed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("primed", isPrimed);
    }
}
