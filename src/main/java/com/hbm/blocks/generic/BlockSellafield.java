package com.hbm.blocks.generic;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Ported from CE's {@code BlockSellafield extends BlockMeta} (a 6-level, {@code needsRandomTick=true}
 * radioactive-decay block, {@code ore_sellafield}-family fallout decoration). CE's real per-tick
 * behavior (read in full): every random tick, radiate {@code 0.5F * (level + 1)} into the chunk
 * radiation field, then with a {@code 1-in-(level==0 ? 25 : 15)} chance either drop one level (if
 * {@code level > 0}) or turn into {@link WastelandVirusBlocks#SELLAFIELD_SLAKED} (at level 0) - the
 * "5-step meta decay chain terminating at sellafield_slaked" docs/phase4/
 * entities_vortex_gravity_wells.md's {@code decontaminate} row names. Walking across it applies a
 * {@code level}-scaled radiation potion effect and, at level &ge; 3, sets the entity on fire for
 * {@code level} seconds (both read verbatim from CE's {@code onEntityWalk}).
 * <p>
 * CE's custom baked-model random-variant rendering ({@code IDynamicModels}, {@code VARIANT} unlisted
 * property, the per-level RGB texture-tint tables) is a Phase 5 client-rendering concern, not ported
 * here - only the gameplay-relevant {@code LEVEL} state and its tick/walk behavior survive, exactly
 * the scope this port's own {@code BlockHazard}/{@code BlockHazardFalling} classes already established
 * for "CE drove extra client-only effects from this same class" cases.
 * Drops / pick-block keep {@code LEVEL} on {@code BLOCK_STATE} (CE ItemStack meta).
 */
public class BlockSellafield extends Block {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 5);

    /** CE's {@code private static final float rad = 0.5f}. */
    private static final float RAD_PER_LEVEL = 0.5F;

    public BlockSellafield(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    /** CE ItemStack meta. Missing {@code BLOCK_STATE} = meta 0. */
    public static int itemLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
        if (props == null) return 0;
        Integer v = props.get(LEVEL);
        return v != null ? v : 0;
    }

    /** CE {@code new ItemStack(sellafield, 1, meta)}. */
    public static ItemStack itemForLevel(int meta) {
        ItemStack stack = new ItemStack(WastelandVirusBlocks.SELLAFIELD.get());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LEVEL, Mth.clamp(meta, 0, 5)));
        return stack;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int meta = state.getValue(LEVEL);
        ChunkRadiationManager.proxy.incrementRad(level, pos, RAD_PER_LEVEL * (meta + 1));

        if (random.nextInt(meta == 0 ? 25 : 15) == 0) {
            if (meta > 0) {
                level.setBlock(pos, this.defaultBlockState().setValue(LEVEL, meta - 1), 2);
            } else {
                level.setBlock(pos, WastelandVirusBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        int meta = state.getValue(LEVEL);
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotionEffects.RADIATION, 30 * 20, meta < 5 ? meta : meta * 2));
        }
        if (meta >= 3) {
            entity.igniteForSeconds(meta);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(itemForLevel(state.getValue(LEVEL)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return itemForLevel(state.getValue(LEVEL));
    }
}
