package com.hbm.blocks.generic;

import com.hbm.config.StructureConfig;
import com.hbm.itempool.ItemPool;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

/**
 * Port of CE {@code com.hbm.blocks.generic.BlockWandLoot} (434 lines) — the structure-authoring
 * loot marker. At paste time CE's {@code TileEntityWandLoot.transformTE} sets {@code triggerReplace}
 * unless {@code StructureConfig.debugStructures}; next tick swaps this block for a chest (or the
 * baked replace-block) and rolls {@link ItemPool}. CE write keys: {@code pool}/{@code min}/{@code max}
 * / {@code block} ({@code writeToNBT} lines 369-382).
 */
public class BlockWandLoot extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<BlockWandLoot> CODEC = simpleCodec(BlockWandLoot::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BlockWandLoot(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWandLoot> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandLootBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof WandLootBlockEntity loot) loot.serverTick();
        };
    }

    public static class WandLootBlockEntity extends BlockEntity {

        private String poolName = "POOL_GENERIC";
        private int minItems;
        private int maxItems = 1;
        private String replaceBlockId = "minecraft:chest";
        private boolean triggerReplace;

        public WandLootBlockEntity(BlockPos pos, BlockState state) {
            super(Phase8Blocks.WAND_LOOT_ENTITY_TYPE.get(), pos, state);
        }

        public void loadFromStructureNbt(CompoundTag tag) {
            if (tag.contains("pool")) poolName = tag.getString("pool");
            else if (tag.contains("poolName")) poolName = tag.getString("poolName");
            if (tag.contains("min")) minItems = tag.getInt("min");
            else if (tag.contains("minItems")) minItems = tag.getInt("minItems");
            if (tag.contains("max")) maxItems = tag.getInt("max");
            else if (tag.contains("maxItems")) maxItems = tag.getInt("maxItems");
            if (tag.contains("block")) replaceBlockId = tag.getString("block");
            triggerReplace = tag.contains("trigger") ? tag.getBoolean("trigger") : !StructureConfig.DEBUG_STRUCTURES.get();
        }

        public void markForReplace() {
            this.triggerReplace = !StructureConfig.DEBUG_STRUCTURES.get();
        }

        void serverTick() {
            if (!triggerReplace) return;
            triggerReplace = false;
            replace();
        }

        private void replace() {
            if (!(level.getBlockState(worldPosition).getBlock() instanceof BlockWandLoot)) return;
            Direction facing = level.getBlockState(worldPosition).getValue(FACING);
            Block replace = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(replaceBlockId)).orElse(Blocks.CHEST);
            if (replace == Blocks.AIR) replace = Blocks.CHEST;
            BlockState placed = replace.defaultBlockState();
            if (placed.hasProperty(ChestBlock.FACING)) {
                placed = placed.setValue(ChestBlock.FACING, facing);
            }
            level.setBlock(worldPosition, placed, 3);
            BlockEntity te = level.getBlockEntity(worldPosition);
            if (!(te instanceof RandomizableContainerBlockEntity chest)) return;
            ItemPool pool = ItemPool.getPool(poolName);
            int count = minItems;
            if (maxItems > minItems) count += level.random.nextInt(maxItems - minItems + 1);
            if (count <= 0) count = 1;
            int slots = chest.getContainerSize();
            RandomSource random = level.random;
            for (int i = 0; i < count; i++) {
                ItemStack stack = ItemPool.getStack(pool, random);
                if (stack.isEmpty() || slots <= 0) continue;
                chest.setItem(random.nextInt(slots), stack);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putString("pool", poolName);
            tag.putInt("min", minItems);
            tag.putInt("max", maxItems);
            tag.putString("block", replaceBlockId);
            tag.putBoolean("trigger", triggerReplace);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            loadFromStructureNbt(tag);
        }
    }
}
