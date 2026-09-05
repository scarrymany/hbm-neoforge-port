package com.hbm.blocks.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.bomb.BombBlockEntities;
import com.hbm.blockentity.bomb.LandmineBlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.config.ServerConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockAllocatorWater;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IBomb;
import com.hbm.saveddata.satellites.SatelliteDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.Landmine} (270 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Not {@code IToolable} - defusing is
 * a direct {@code ModItems.defuser}/{@code defuser_desh} identity check inside
 * {@link #useWithoutItem}/{@link #useItemOn}, bypassing the generic {@code IToolable} dispatch
 * entirely (a real, intentional CE quirk this port preserves exactly - see the research report's
 * headline finding #2 and Open Questions). {@link #explode} switches on which concrete variant
 * ({@link BombBlocks#MINE_AP} through {@link BombBlocks#MINE_NAVAL}) this instance is, by identity,
 * matching CE's own registry-name-string switch one-to-one in behavior.
 * <p>
 * {@code SatelliteDetector.reportEvent} on {@code mine_fat} is Exact CE {@code Landmine.java:241}.
 * CE's {@code MainRegistry.
 * polaroidID} easter egg (a 1-in-100-chance balefire-cloud swap, gated on an unrelated hidden
 * config id) is dropped - the underlying 1/100 random roll for the balefire-vs-normal mushroom
 * cloud is kept.
 */
public class Landmine extends BaseEntityBlock implements IBomb {

    public static final MapCodec<Landmine> CODEC = simpleCodec(p -> new Landmine(p, 0.0D, 0.0D));

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    private static final VoxelShape AP_SHAPE = Block.box(5, 0, 5, 11, 1, 11);
    private static final VoxelShape HE_SHAPE = Block.box(4, 0, 4, 12, 2, 12);
    private static final VoxelShape SHRAP_SHAPE = Block.box(5, 0, 5, 11, 1, 11);
    private static final VoxelShape FAT_SHAPE = Block.box(5, 0, 4, 11, 6, 12);

    public static boolean safeMode = false;

    public final double range;
    public final double height;

    public Landmine(Properties properties, double range, double height) {
        super(properties);
        this.range = range;
        this.height = height;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** CE: {@code getItemDropped} returns {@code Items.AIR} - normal mining never returns the mine (it explodes instead, see {@link #onRemove}). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this == BombBlocks.MINE_AP.get()) return AP_SHAPE;
        if (this == BombBlocks.MINE_HE.get()) return HE_SHAPE;
        if (this == BombBlocks.MINE_SHRAP.get()) return SHRAP_SHAPE;
        if (this == BombBlocks.MINE_FAT.get()) return FAT_SHAPE;
        return Shapes.block();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP) || belowState.getBlock() instanceof FenceBlock;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }

        if (!level.getBlockState(pos).canSurvive(level, pos)) {
            Block.popResource(level, pos, new ItemStack(this.asItem()));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !safeMode) {
            explode(level, pos, null);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * CE: {@code onBlockActivated} - direct defuser identity check, not routed through
     * {@code IToolable}. Looks the two defuser items up by registry id
     * ({@code hbm:defuser}/{@code hbm:defuser_desh}) rather than a compile-time
     * {@code ModItems} field reference: {@code com.hbm.items.tool.ItemDefuser}/its two registry
     * entries are Section C of {@code docs/phase3/bomb_blocks_and_detonators.md} ("Detonator /
     * defuser items"), explicitly out of this task's Section-A scope and not yet landed in
     * {@code ModItems} (a forbidden-to-edit shared file in this wave) as of this writing - a
     * registry-id lookup resolves correctly the moment that package registers those two items,
     * under any Java field name it chooses, with zero coupling to this class needing to change.
     */
    private InteractionResult tryDefuse(Level level, BlockPos pos, Player player) {
        Item heldMain = player.getMainHandItem().getItem();
        Item heldOff = player.getOffhandItem().getItem();
        boolean hasDefuser = isDefuserItem(heldMain) || isDefuserItem(heldOff);

        if (!hasDefuser) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            safeMode = true;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            safeMode = false;

            ItemStack stack = new ItemStack(this.asItem());
            RandomSource random = level.getRandom();
            float fx = random.nextFloat() * 0.6F + 0.2F;
            float fy = random.nextFloat() * 0.2F;
            float fz = random.nextFloat() * 0.6F + 0.2F;

            ItemEntity drop = new ItemEntity(level, pos.getX() + fx, pos.getY() + fy + 1, pos.getZ() + fz, stack);
            float velocity = 0.05F;
            drop.setDeltaMovement(random.nextGaussian() * velocity, random.nextGaussian() * velocity + 0.2F, random.nextGaussian() * velocity);
            level.addFreshEntity(drop);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return tryDefuse(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = tryDefuse(level, pos, player);
        return result == InteractionResult.SUCCESS
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LandmineBlockEntity(BombBlockEntities.LANDMINE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BombBlockEntities.LANDMINE.get() ? ITickableBE.ticker() : null;
    }

    private static boolean isDefuserItem(Item item) {
        if (item == Items.AIR) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && "hbm".equals(id.getNamespace()) && ("defuser".equals(id.getPath()) || "defuser_desh".equals(id.getPath()));
    }

    private boolean isWaterAbove(Level level, int x, int y, int z) {
        for (int xo = -1; xo <= 1; xo++) {
            for (int zo = -1; zo <= 1; zo++) {
                if (!level.getFluidState(new BlockPos(x + xo, y + 1, z + zo)).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        safeMode = true;
        level.destroyBlock(pos, false);
        safeMode = false;

        if (this == BombBlocks.MINE_AP.get()) {
            ExplosionVNT vnt = new ExplosionVNT(level, x + .5, y + .5, z + .5, 3F, detonator);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, ServerConfig.MINE_AP_DAMAGE.get().floatValue()).setupPiercing(5F, 0.2F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(5, 1F, .5F));
            vnt.explode();
        } else if (this == BombBlocks.MINE_HE.get()) {
            ExplosionVNT vnt = new ExplosionVNT(level, x + .5, y + .5, z + .5, 4F, detonator);
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            vnt.setBlockProcessor(new BlockProcessorStandard());
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, ServerConfig.MINE_HE_DAMAGE.get().floatValue()).setupPiercing(15F, 0.2F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
            vnt.explode();
        } else if (this == BombBlocks.MINE_SHRAP.get()) {
            ExplosionVNT vnt = new ExplosionVNT(level, x + 0.5, y + 0.5, z + 0.5, 3F, detonator);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, ServerConfig.MINE_SHRAP_DAMAGE.get().floatValue()));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(5, 1F, 0.5F));
            vnt.explode();
            ExplosionLarge.spawnShrapnelShower(level, x + 0.5, y + 0.5, z + 0.5, 0, 1D, 0, 45, .2D);
            ExplosionLarge.spawnShrapnels(level, x + 0.5, y + 0.5, z + 0.5, 5);
        } else if (this == BombBlocks.MINE_FAT.get()) {
            SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_LOW,
                    SatelliteDetector.BurstIntensity.LOW, x + 0.5, z + 0.5);
            level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, BombConfig.FATMAN_RADIUS.get(), x + 0.5, y + 0.5, z + 0.5).setDetonator(detonator));
            if (level.getRandom().nextInt(100) == 0) {
                EntityNukeTorex.statFacBale(level, x + 0.5, y + 0.5, z + 0.5, BombConfig.FATMAN_RADIUS.get());
            } else {
                EntityNukeTorex.statFac(level, x + 0.5, y + 0.5, z + 0.5, BombConfig.FATMAN_RADIUS.get());
            }
        } else if (this == BombBlocks.MINE_NAVAL.get()) {
            ExplosionVNT vnt = new ExplosionVNT(level, x + 5, y + 5, z + 5, 25F); // CE: offset by +5, not +0.5 - preserved exactly.
            vnt.setBlockAllocator(new BlockAllocatorWater(32));
            vnt.setBlockProcessor(new BlockProcessorStandard());
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, ServerConfig.MINE_NAVAL_DAMAGE.get().floatValue()).setupPiercing(5F, 0.2F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 1F, 0.5F));
            vnt.explode();

            ExplosionLarge.spawnParticlesRadial(level, x + 0.5, y + 2, z + 0.5, 30);
            ExplosionLarge.spawnRubble(level, x + 0.5, y + 0.5, z + 0.5, 5);

            if (isWaterAbove(level, x, y, z)) {
                ExplosionLarge.spawnFoam(level, x + 0.5, y + 0.5, z + 0.5, 60);
            }
        }

        return BombReturnCode.DETONATED;
    }
}
