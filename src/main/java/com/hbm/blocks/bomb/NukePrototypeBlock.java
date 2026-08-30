package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blockentity.bomb.NukePrototypeBlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.items.bomb.NukeCasingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code NukePrototype} (236 lines, read in full) - antischrabidium test rig,
 * drives {@link EntityNukeExplosionMK3}'s "waste" path plus an {@link EntityCloudFleija} companion
 * cloud, and is jammer-checkable ({@link EntityNukeExplosionMK3#isJammed}). Uniquely among the 9
 * casings, a right-click while holding the {@code igniter} item detonates immediately without
 * opening the GUI (CE's {@code onBlockActivated}'s {@code ModItems.igniter} branch).
 */
public class NukePrototypeBlock extends NukeCasingBlockBase {

    public NukePrototypeBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukePrototypeBlockEntity(NukeCasingBlockEntities.NUKE_PROTOTYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    /** CE: {@code onBlockActivated}'s {@code ModItems.igniter} branch - always consumes the click, detonates only if ready. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() != NukeCasingItems.IGNITER.get() || player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NukePrototypeBlockEntity be && be.isReady()) {
            be.clearSlots();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            igniteTestBomb(level, player, pos, BombConfig.PROTOTYPE_RADIUS.get());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private void igniteTestBomb(Level level, @Nullable Entity detonator, BlockPos pos, int radius) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        EntityNukeExplosionMK3 entity = new EntityNukeExplosionMK3(NukeEntityTypes.NUKE_MK3.get(), level);
        entity.setPos(x, y, z);
        if (detonator != null) {
            entity.setDetonator(detonator);
        } else if (level.getBlockEntity(pos) instanceof NukePrototypeBlockEntity prototype) {
            entity.detonator = prototype.placerID;
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
        if (level.getBlockEntity(pos) instanceof NukePrototypeBlockEntity be && be.isReady()) {
            be.clearSlots();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            igniteTestBomb(level, detonator, pos, BombConfig.PROTOTYPE_RADIUS.get());
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }
}
