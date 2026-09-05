package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import com.hbm.blockentity.machine.FloodlightBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Floodlight extends BaseEntityBlock implements IToolable {

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 11);
    public static final MapCodec<Floodlight> CODEC = simpleCodec(p -> new Floodlight());

    public Floodlight() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F)
        );
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected @NotNull MapCodec<? extends Floodlight> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(META, context.getClickedFace().ordinal());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FloodlightBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FloodlightBlockEntity floodlight) {
                floodlight.tick();
            }
        };
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        if (placer != null) {
            setAngle(level, pos, placer, true);
        }
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        setAngle(world, new BlockPos(x, y, z), player, false);
        return true;
    }

    public void setAngle(Level world, BlockPos pos, LivingEntity player, boolean updateMeta) {
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        float rotation = player.getXRot();

        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof FloodlightBlockEntity floodlight) {
            BlockState state = world.getBlockState(pos);
            int meta = state.getValue(META) % 6;

            if (meta == 0 || meta == 1) {
                if (i == 0 || i == 2) {
                    if (updateMeta) {
                        world.setBlock(pos, state.setValue(META, meta + 6), 3);
                    }
                }
                if (meta == 1) {
                    if (i == 0 || i == 1) rotation = 180F - rotation;
                }
                if (meta == 0) {
                    if (i == 0 || i == 3) rotation = 180F - rotation;
                }
            }

            floodlight.rotation = -Math.round(rotation / 5F) * 5F;
            if (floodlight.isOn) floodlight.destroyLights();
            floodlight.setChanged();
        }
    }
}
