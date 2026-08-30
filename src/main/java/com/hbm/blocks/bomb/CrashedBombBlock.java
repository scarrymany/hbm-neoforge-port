package com.hbm.blocks.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.bomb.CrashedBombBlockEntity;
import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IBomb;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.bomb.NukeCasingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code BlockCrashedBomb} (174 lines, read in full) - dud/salvage nuke wreckage.
 * CE backed all 4 {@link EnumDudType} variants with one {@code BlockEnumMeta}-flattened block; per
 * this port's metadata-flattening convention this becomes 4 distinct registry entries (one per dud
 * type, see {@code NukeCasingBlocks#registerAll()}), each instance carrying its own baked-in
 * {@link EnumDudType} rather than reading it off a {@code BlockState} property. Right-click with a
 * defuser bypasses the generic {@code IToolable}/{@code onScrew} path entirely with a direct item
 * identity check - the same bypass pattern {@code Landmine} uses in the sibling
 * conventional-explosives package (see {@code docs/phase3/bomb_blocks_and_detonators.md}'s headline
 * finding #2 and open questions). {@code ItemDefuser}/its two registry entries
 * ({@code hbm:defuser}/{@code hbm:defuser_desh}) are Section C of that report ("Detonator/defuser
 * items"), out of this task's Section-B scope and not yet landed in {@code ModItems} as of this
 * writing - matching {@code Landmine}'s own solution, this looks the two items up by registry id via
 * {@link BuiltInRegistries#ITEM} rather than a compile-time field reference, so it starts working the
 * moment that package registers them under whatever Java field name it chooses.
 * <p>
 * CE's {@code ball_tnt} salvage drop (the {@code CONVENTIONAL}/{@code NUKE}/{@code SALTED} branches)
 * is a documented gap here: {@code ModItems.ball_tnt} is confirmed absent from this port (owned by a
 * different, non-bomb-casing item family - see {@code NukeCasingItems}'s own javadoc for the same
 * gap in {@code TileEntityNukeCustom}'s crafting map) - those branches drop their other salvage
 * materials ({@code billet_plutonium}/{@code ingot_cobalt}, both confirmed present) and skip only the
 * {@code ball_tnt} stack.
 */
public class CrashedBombBlock extends BaseEntityBlock implements IBomb {

    private final EnumDudType dudType;

    public CrashedBombBlock(Properties properties, EnumDudType dudType) {
        super(properties);
        this.dudType = dudType;
    }

    public EnumDudType getDudType() {
        return dudType;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrashedBombBlockEntity(NukeCasingBlockEntities.CRASHED_BOMB.get(), pos, state, dudType);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == NukeCasingBlockEntities.CRASHED_BOMB.get() ? ITickableBE.ticker() : null;
    }

    /** CE: {@code onBlockActivated} - direct defuser identity check (registry-id lookup, see class javadoc), salvages materials by dud type. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!isDefuserItem(stack.getItem())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            if (stack.getMaxDamage() > 0) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }

            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            switch (dudType) {
                case BALEFIRE -> level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(NukeCasingItems.EGG_BALEFIRE_SHARD.get())));
                case CONVENTIONAL -> {
                    // TODO(ModItems.ball_tnt, confirmed absent - see class javadoc): CE drops 16x ball_tnt here.
                }
                case NUKE -> {
                    // TODO(ModItems.ball_tnt): CE also drops 8x ball_tnt here.
                    level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(BilletPowderItems.BILLET_PLUTONIUM.get(), 4)));
                }
                case SALTED -> {
                    // TODO(ModItems.ball_tnt): CE also drops 8x ball_tnt here.
                    level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(BilletPowderItems.BILLET_PLUTONIUM.get(), 2)));
                    level.addFreshEntity(new ItemEntity(level, x, y, z + 0.5, new ItemStack(IngotNuggetItems.INGOT_COBALT.get(), 12)));
                }
            }
            level.destroyBlock(pos, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** See class javadoc: registry-id lookup, not a compile-time {@code ModItems} field reference. */
    private static boolean isDefuserItem(Item item) {
        if (item == Items.AIR) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && "hbm".equals(id.getNamespace()) && ("defuser".equals(id.getPath()) || "defuser_desh".equals(id.getPath()));
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        switch (dudType) {
            case BALEFIRE -> {
                EntityBalefire bf = new EntityBalefire(NukeEntityTypes.BALEFIRE.get(), level);
                bf.setPos(pos.getX(), pos.getY(), pos.getZ());
                bf.destructionRange = (int) (BombConfig.FATMAN_RADIUS.get() * 1.25);
                level.addFreshEntity(bf);
                if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                    EntityNukeTorex.statFacBale(level, x, y + 5, z, bf.destructionRange);
                }
            }
            case CONVENTIONAL -> {
                ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 35F);
                xnt.setBlockAllocator(new BlockAllocatorStandard(24));
                xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                xnt.setEntityProcessor(new EntityProcessorCross(5D).withRangeMod(1.5F));
                xnt.setPlayerProcessor(new PlayerProcessorStandard());
                xnt.explode();
            }
            case NUKE -> {
                level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, 35, x, y, z));
                if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                    EntityNukeTorex.statFac(level, x, y + 5, z, 35);
                }
            }
            case SALTED -> {
                level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, 25, x, y, z).moreFallout(25));
                if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                    EntityNukeTorex.statFac(level, x, y + 5, z, 25);
                }
            }
        }
        return BombReturnCode.DETONATED;
    }

    public enum EnumDudType {
        BALEFIRE, CONVENTIONAL, NUKE, SALTED
    }
}
