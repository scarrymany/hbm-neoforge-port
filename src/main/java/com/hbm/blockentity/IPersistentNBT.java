package com.hbm.blockentity;

import com.hbm.api.tile.IWorldRenameable;
import com.hbm.blocks.BlockDummyable;
import com.hbm.util.TagsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent block data helper, ported from CE's {@code com.hbm.tileentity.IPersistentNBT}.
 *
 * <p>Lifecycle expectations (unchanged from CE, retyped for 1.21):</p>
 * <ul>
 *   <li>A concrete {@link Block} should call {@link #breakBlock} from its own
 *   {@code onRemove(BlockState, Level, BlockPos, BlockState, boolean)} override, before calling
 *   {@code super.onRemove(...)}, whenever {@code state.getBlock() != newState.getBlock()} — the 1.21
 *   equivalent of CE's {@code Block#breakBlock}, which no longer exists as a separate method (see
 *   {@link BlockDummyable#onRemove} for the pattern, though {@code BlockDummyable} itself never
 *   carries block-entity data of its own and so never calls this).</li>
 *   <li>Call {@link #restoreData} from {@code setPlacedBy}; {@link BlockDummyable#setPlacedBy}
 *   already does this for every multiblock core.</li>
 *   <li>Call {@link #onBlockHarvested} from {@code playerWillDestroy}/{@code playerDestroy}.</li>
 *   <li>{@link #writeNBT(CompoundTag)} receives a new tag; put your contents under
 *   {@link #NBT_PERSISTENT_KEY} if you want to keep the item's persistent data clean.</li>
 *   <li>{@link #readNBT(CompoundTag)} gets the full persistent-data tag; check both the root and
 *   {@link #NBT_PERSISTENT_KEY} so old items still load.</li>
 *   <li>Call {@link #setDestroyedByCreativePlayer()} (via {@link #onBlockHarvested}) to skip drops
 *   for creative breaks.</li>
 * </ul>
 *
 * <p><strong>Warning:</strong> This differs from CE because vanilla/NeoForge's break order still
 * destroys the block before the block entity is removed, same as CE's own warning noted.</p>
 *
 * <p>Package/naming decision (see {@code docs/phase2/multiblock_framework.md}): this interface lives
 * in {@code com.hbm.blockentity} (not CE's {@code com.hbm.tileentity}) matching Neo Edition's own
 * choice, and the static restore-on-place method is named {@link #restoreData} (Neo Edition's name)
 * rather than CE's {@code onBlockPlacedBy}, which would otherwise collide/read confusingly next to
 * {@code Block#onBlockPlacedBy}. Every other name and the full CE contract (creative-no-drop,
 * custom-name round-trip via {@link IWorldRenameable}, {@link #carriesContents}) is kept — Neo
 * Edition's own version drops all of that, which is a behavior loss this port does not adopt.</p>
 */
public interface IPersistentNBT {

    String NBT_PERSISTENT_KEY = "persistent";

    /**
     * Call before {@code super.onRemove(...)} when the block is actually being removed (i.e. CE's
     * {@code Block#breakBlock} call site — see class javadoc for the 1.21 mapping). Server side only.
     *
     * @return true if an item was dropped
     */
    static boolean breakBlock(Level level, BlockPos pos, BlockState state) {
        // intentionally does not resolve dummy->core (unlike onBlockHarvested below), so that only
        // the block that actually holds the core block entity drops items, not every dummy above it
        BlockEntity tile = level.getBlockEntity(pos);
        final boolean flag;
        if (tile instanceof IPersistentNBT persistentTE && persistentTE.shouldDrop()) {
            ItemStack itemstack = new ItemStack(state.getBlock());
            CompoundTag data = new CompoundTag();
            persistentTE.writeNBT(data);
            if (!data.isEmpty()) TagsUtil.putCustomData(itemstack, data);
            if (tile instanceof IWorldRenameable nameable && nameable.hasCustomName()) {
                itemstack.set(DataComponents.CUSTOM_NAME, nameable.getName());
                nameable.setCustomName(null);
            }
            Block.popResource(level, pos, itemstack);
            flag = true;
        } else {
            flag = false;
        }
        if (state.hasAnalogOutputSignal()) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        return flag;
    }

    /**
     * Call from {@code setPlacedBy}. Server side reads the persisted data tag back into the block
     * entity; the custom-name round-trip (item display name -> block entity name) runs regardless of
     * side, matching CE.
     */
    static void restoreData(Level level, BlockPos pos, ItemStack stack) {
        if (!level.isClientSide && TagsUtil.hasCustomData(stack) && level.getBlockEntity(pos) instanceof IPersistentNBT persistentTE) {
            persistentTE.readNBT(TagsUtil.getCustomData(stack));
        }
        if (stack.has(DataComponents.CUSTOM_NAME) && level.getBlockEntity(pos) instanceof IWorldRenameable renameable) {
            renameable.setCustomName(stack.getHoverName());
        }
    }

    /**
     * Marks creative destruction so {@link #shouldDrop()} can skip spawning items. Resolves
     * {@code pos} to its multiblock core first (matching CE's {@code CompatExternal.getCoreFromPos}
     * for {@link BlockDummyable} dummies) so breaking any dummy of a creatively-destroyed multiblock
     * correctly suppresses the core's drop too.
     */
    static void onBlockHarvested(Level level, BlockPos pos, Player player) {
        if (player.isCreative() && findPersistentTarget(level, pos) instanceof IPersistentNBT persistentTE) {
            persistentTE.setDestroyedByCreativePlayer();
        }
    }

    /**
     * True when the stack carries block contents in its persistent data. Such stacks are unique and
     * must never merge with one another, otherwise placing the merged stack hands out one crate's
     * worth of contents once per item in it.
     */
    static boolean carriesContents(ItemStack stack) {
        if (stack.isEmpty() || !TagsUtil.hasCustomData(stack)) return false;
        CompoundTag tag = TagsUtil.getCustomData(stack);
        return tag.contains(NBT_PERSISTENT_KEY) && !tag.getCompound(NBT_PERSISTENT_KEY).isEmpty();
    }

    /**
     * CE resolved this through {@code com.hbm.util.CompatExternal.getCoreFromPos}, which does not
     * exist in this port yet (it also handles a legacy mk1 {@code TileEntityDummy} system that has
     * not been ported and is out of this package's scope). Only the {@link BlockDummyable} branch —
     * the only one relevant to any multiblock this port's {@code BlockDummyable} contract covers —
     * is inlined here to avoid depending on a class this Phase 2 package was not asked to create.
     */
    private static BlockEntity findPersistentTarget(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof BlockDummyable dummy) {
            BlockPos corePos = dummy.findCore(level, pos);
            if (corePos != null) return level.getBlockEntity(corePos);
        }
        return level.getBlockEntity(pos);
    }

    default boolean shouldDrop() {
        return !isDestroyedByCreativePlayer();
    }

    void setDestroyedByCreativePlayer();

    boolean isDestroyedByCreativePlayer();

    void writeNBT(CompoundTag nbt);

    void readNBT(CompoundTag nbt);
}
