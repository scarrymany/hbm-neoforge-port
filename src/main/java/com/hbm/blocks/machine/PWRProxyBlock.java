package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.PWRBlockEntities;
import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.blockentity.machine.PWRProxyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

/**
 * Ported from CE's {@code BlockPWR} (regname {@code pwr_block}, read in full - CE's own file carries
 * the comment "Oh my fucking god fristie, this dogshit should be thrown the fuck out", preserved here
 * only as a citation, not an endorsement). The shared structural-replacement proxy every non-core
 * position of an assembled PWR becomes - see {@link MachinePWRControllerBlock#assemble} for the
 * placement side and {@link PWRProxyBlockEntity} for the forwarding side.
 *
 * <p>{@link #IO_ENABLED} maps CE's {@code PropertyBool} 1:1 onto a {@link BooleanProperty} - exactly
 * the confirmed, already-common blockstate idiom {@code docs/phase2/reactors_breeding_pwr.md} calls
 * out (see any existing {@link BooleanProperty} usage in {@code com.hbm.blocks.generic}).
 *
 * <p><b>Never obtainable</b>: CE's {@code getItemDropped} returns {@code Items.AIR} and its
 * creative tab is {@code null} - this port matches that intent structurally instead, by simply never
 * registering a {@code BlockItem} for it at all (see {@link PWRBlocks#registerAll()}) rather than
 * registering one and then suppressing every drop path; the block only ever appears as the result of
 * {@link MachinePWRControllerBlock#assemble}'s structure replacement, never placed or held directly.
 */
public class PWRProxyBlock extends BaseEntityBlock {

    public static final MapCodec<PWRProxyBlock> CODEC = simpleCodec(PWRProxyBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final BooleanProperty IO_ENABLED = BooleanProperty.create("io");

    public PWRProxyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(IO_ENABLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IO_ENABLED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PWRProxyBlockEntity(PWRBlockEntities.PWR_PROXY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PWRBlockEntities.PWR_PROXY.get() ? ITickableBE.ticker() : null;
    }

    /**
     * Ported from CE's {@code breakBlock}: if this position had a recorded core, restore the
     * original structural block and mark that core disassembled, instead of letting the position
     * default to air. Returning without calling {@code super.onRemove} once the restoration runs
     * (rather than letting it fall through, like CE's own {@code super.breakBlock(...)} call after
     * the state swap) avoids immediately tearing back down the just-restored block/BE pair CE's own
     * follow-up call would otherwise remove the tile entity of.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof PWRProxyBlockEntity proxy) {

            BlockPos corePos = proxy.getCorePos();
            if (corePos != null && level.getBlockEntity(corePos) instanceof PWRControllerBlockEntity controller) {
                controller.assembled = false;
                controller.setChanged();
            }

            BlockState original = proxy.getOriginalBlockState();
            if (original != null) {
                level.setBlock(pos, original, 3);
                return;
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
