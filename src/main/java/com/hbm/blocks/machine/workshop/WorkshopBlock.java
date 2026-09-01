package com.hbm.blocks.machine.workshop;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.workshop.AmmoPressBlockEntity;
import com.hbm.blockentity.machine.workshop.ArcWelderBlockEntity;
import com.hbm.blockentity.machine.workshop.SolderingBlockEntity;
import com.hbm.blockentity.machine.workshop.WorkshopBlockEntities;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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

public class WorkshopBlock extends BaseEntityBlock {

    public enum Kind implements StringRepresentable {
        AMMO_PRESS("ammo_press"),
        ARC_WELDER("arc_welder"),
        SOLDERING("soldering");

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<WorkshopBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StringRepresentable.fromEnum(Kind::values).fieldOf("kind").forGetter(b -> b.kind)
    ).apply(i, WorkshopBlock::new));

    public final Kind kind;

    public WorkshopBlock(Properties properties, Kind kind) {
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
        return switch (kind) {
            case AMMO_PRESS -> new AmmoPressBlockEntity(WorkshopBlockEntities.MACHINE_AMMO_PRESS.get(), pos, state);
            case ARC_WELDER -> new ArcWelderBlockEntity(WorkshopBlockEntities.MACHINE_ARC_WELDER.get(), pos, state);
            case SOLDERING -> new SolderingBlockEntity(WorkshopBlockEntities.MACHINE_SOLDERING_STATION.get(), pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityType<?> expected = switch (kind) {
            case AMMO_PRESS -> WorkshopBlockEntities.MACHINE_AMMO_PRESS.get();
            case ARC_WELDER -> WorkshopBlockEntities.MACHINE_ARC_WELDER.get();
            case SOLDERING -> WorkshopBlockEntities.MACHINE_SOLDERING_STATION.get();
        };
        return type == expected ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MenuProvider be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }
}
