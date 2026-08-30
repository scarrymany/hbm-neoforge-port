package com.hbm.blocks.generic;

import com.hbm.blocks.IOreType;
import com.hbm.hazard.HazardSystem;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code BlockNTMOre extends BlockOre}. Modern vanilla's ore-block analogue is
 * {@link DropExperienceBlock} (the successor to 1.12's {@code BlockOre}), which this extends for
 * the same fortune-aware self-drop default. When {@link #oreType} is non-null the block instead
 * drops through the {@link IOreType} drop-function/quantity-function pair, exactly like CE.
 * <p>
 * CE's chunk-radiation propagation ({@code ChunkRadiationManager}) and the entity-collision hazard
 * hooks are reduced to {@link HazardSystem#applyHazards} on {@link #entityInside}, the modern
 * unified replacement for CE's separate {@code onEntityWalk}/{@code onEntityCollision} overrides.
 */
public class BlockNTMOre extends DropExperienceBlock {

    @Nullable
    protected final IOreType oreType;

    public BlockNTMOre(Properties properties, @Nullable IOreType oreType) {
        this(properties, oreType, 0);
    }

    public BlockNTMOre(Properties properties, @Nullable IOreType oreType, int xp) {
        super(ConstantInt.of(xp), properties);
        this.oreType = oreType;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (oreType == null) {
            return super.getDrops(state, params);
        }

        RandomSource rand = params.getLevel().getRandom();
        int count = oreType.getQuantityFunction().apply(state, 0, rand);
        List<ItemStack> drops = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = oreType.getDropFunction().apply(state, rand);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
        return drops;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity instanceof LivingEntity living) {
            HazardSystem.applyHazards(this, living);
        }
    }
}
