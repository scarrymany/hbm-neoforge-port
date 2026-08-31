package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.machine.CrateBlockEntity;
import com.hbm.blockentity.machine.CrateBlockEntity.CrateType;
import com.hbm.blockentity.machine.StorageBlockEntities;
import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.inventory.container.CrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Mass storage crate block, ported from CE's {@code com.hbm.blocks.generic.BlockStorageCrate}
 * (read in full). One instance per {@link CrateType} grade, matching this port's own table-driven
 * block-family precedent (see {@code com.hbm.blocks.generic.GenericCrateBlocks}) rather than CE's
 * {@code this == ModBlocks.crate_x} identity dispatch.
 *
 * <p>The tungsten grade additionally implements {@link IRadResistantBlock}, matching CE's
 * {@code BlockStorageCrateRadResistant} (a distinct subclass in CE whose only difference is a
 * radiation-shielding tooltip line and a {@code RadiationSystemNT.markSectionForRebuild} hook on
 * place/break - folded into this single class as a per-{@link CrateType} flag rather than a second
 * subclass). The {@code markSectionForRebuild} hook itself is wired (Phase 4) in {@link #setPlacedBy}/
 * {@link #onRemove}, called unconditionally for every grade (not just tungsten) since a non-resistant
 * crate replacing a resistant one, or vice versa, still changes the section's resistant-block mask.
 * {@link CrateType#TUNGSTEN}'s laser-heating mechanic ({@code ILaserable}, CE's
 * {@code TileEntityCrateTungsten}) - previously a documented follow-up here (see
 * {@code docs/phase2/machines_storage.md}'s "Open questions") - is now wired on
 * {@link CrateBlockEntity} itself (implements {@code ILaserable}; see that class's javadoc/
 * {@code addEnergy}, ported {@code c7-mrec-05-purex-misc}). No laser <em>source</em> (CE's
 * {@code TileEntityCoreEmitter}) exists anywhere in this port yet to ever call it - see that javadoc
 * for why that remains a separate, unbuilt DFC-core machine system.
 *
 * <p>Also implements {@link BlockStorageCrate} (added in the Phase 6 parity-audit pass) - the marker
 * interface {@link com.hbm.hazard.transformer.HazardTransformerRadiationContainer}'s
 * {@code instanceof BlockStorageCrate} check needs to recognize crate_iron/steel/tungsten/desh/safe
 * as CE's real {@code BlockStorageCrate} lineage; see that interface's javadoc for why this class,
 * not a new abstract class, is where the interface lands.
 */
public class CrateBlock extends BaseEntityBlock implements IRadResistantBlock, BlockStorageCrate {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final CrateType type;

    public CrateBlock(Properties properties, CrateType type) {
        super(properties);
        this.type = type;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public CrateType getCrateType() {
        return this.type;
    }

    /**
     * Only the {@link CrateType#TUNGSTEN} grade is rad-resistant in CE (a distinct
     * {@code BlockStorageCrateRadResistant} subclass there) - overridden rather than left at
     * {@link IRadResistantBlock}'s unconditional-{@code true} default, since every {@link CrateType}
     * grade shares this one class (see class javadoc) and the default would otherwise incorrectly
     * mark every non-tungsten grade rad-resistant too.
     */
    @Override
    public boolean isRadResistant(Level level, BlockPos blockPos) {
        return this.type == CrateType.TUNGSTEN;
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(StorageBlockEntities.CRATE_TYPE.get(), pos, state, type);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        IPersistentNBT.restoreData(level, pos, stack);
        if (!level.isClientSide) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            IPersistentNBT.breakBlock(level, pos, state);
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        IPersistentNBT.onBlockHarvested(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Opens {@link CrateMenu} - confirmed-real NeoForge 1.21.1 shape
     * ({@code player.openMenu(MenuProvider, BlockPos)} + {@link SimpleMenuProvider}), cross-checked
     * against Neo Edition's own real, compiling {@code CrateBlock#useWithoutItem}.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            player.openMenu(new SimpleMenuProvider((id, inv, ply) -> new CrateMenu(id, inv, crate), crate.getDisplayName()), pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Comparator override, matching CE's {@code hasComparatorInputOverride}/
     * {@code getComparatorInputOverride} -> {@code ItemHandlerHelper.calcRedstoneFromInventory}. That
     * exact NeoForge utility method's presence could not be independently confirmed against a real
     * decompiled class in this sandbox, so the well-known chest-comparator algorithm it implements
     * (identical across every Forge/NeoForge version since 1.7) is reimplemented directly here rather
     * than risk an unconfirmed-API compile failure.
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) return 0;
        return calcRedstoneFromInventory(crate.getItemHandlerCapability(null));
    }

    private static int calcRedstoneFromInventory(@Nullable IItemHandler inv) {
        if (inv == null || inv.getSlots() == 0) return 0;
        int itemsFound = 0;
        float proportion = 0F;
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                proportion += (float) stack.getCount() / Math.min(inv.getSlotLimit(slot), stack.getMaxStackSize());
                itemsFound++;
            }
        }
        proportion = proportion / inv.getSlots();
        return Mth.floor(proportion * 14.0F) + (itemsFound > 0 ? 1 : 0);
    }
}
