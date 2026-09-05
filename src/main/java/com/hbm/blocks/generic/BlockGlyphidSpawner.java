package com.hbm.blocks.generic;

import com.hbm.blockentity.ITickableBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.Locale;

/**
 * CE {@code BlockGlyphidSpawner} ({@code BlockGlyphidSpawner.java}:34-206). One block, {@code TYPE}
 * BASE/INFESTED/RAD. Drops {@code egg_glyphid} 1+rand(3)+fortune. TE swarms glyphids that already
 * exist in this port ({@code GlyphidEntityTypes}).
 */
public class BlockGlyphidSpawner extends BaseEntityBlock {

    public static final MapCodec<BlockGlyphidSpawner> CODEC = simpleCodec(BlockGlyphidSpawner::new);
    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

    public BlockGlyphidSpawner(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, Type.BASE));
    }

    @Override
    protected MapCodec<? extends BlockGlyphidSpawner> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlyphidSpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PlantBlocks.GLYPHID_SPAWNER_ENTITY_TYPE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int n = 1 + params.getLevel().random.nextInt(3);
        return List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("hbm:egg_glyphid")), n));
    }

    public enum Type implements StringRepresentable {
        BASE, INFESTED, RAD;

        public static final Type[] VALUES = values();

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
