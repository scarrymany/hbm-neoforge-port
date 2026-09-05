package com.hbm.blockentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.network.BlockCraneRouter;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.menu.CraneRouterMenu;
import com.hbm.module.ModulePatternMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge port of CE's {@code TileEntityCraneRouter} - routes items to different output directions.
 * With ModulePatternMatcher filter inventory:
 * - 30 filter slots = 6 sides × 5 filters
 * - 6 modes: NONE, WHITELIST, BLACKLIST, WILDCARD (per side)
 * - Filtered routing: check filters for all 6 sides, output to matching direction(s)
 * CE behavior: whitelist/blacklist filtering + wildcard fallback + random selection from valid dirs.
 */
public class CraneRouterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;

    public ModulePatternMatcher[] patterns = new ModulePatternMatcher[6];
    public int[] modes = new int[6];
    public ItemStackHandler inventory = new ItemStackHandler(30); // 6 sides × 5 filters

    public CraneRouterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < patterns.length; i++) {
            patterns[i] = new ModulePatternMatcher(5);
        }
    }

    public void tick() {
        // Passive routing: no active tick logic needed
    }

    /**
     * Routes an item to a valid output direction based on filters.
     * CE logic: check filters for all 6 sides, pick matching direction(s), choose random from valid.
     * Filtering: WHITELIST requires match, BLACKLIST requires no match, WILDCARD accepts all, NONE skips.
     */
    public void routeItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return;
        }

        Direction chosenDir = getOutputDir(stack);

        if (chosenDir == null) {
            // No valid direction found — drop item
            level.addFreshEntity(new ItemEntity(level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    stack.copy()));
            return;
        }

        // Spawn EntityMovingItem on chosen conveyor
        sendOnRoute(stack, chosenDir);
        setChanged();
    }

    /**
     * CE logic: determine output direction for a stack.
     * 1. Check all 6 sides for whitelist/blacklist filters
     * 2. If no match, use wildcard sides
     * 3. Random selection from valid directions
     */
    private Direction getOutputDir(ItemStack stack) {
        List<Direction> validDirs = new ArrayList<>();

        // Check filters for all 6 sides
        for (int side = 0; side < 6; side++) {
            int mode = modes[side];

            // Skip disabled or wildcard sides in first pass
            if (mode == MODE_NONE || mode == MODE_WILDCARD) {
                continue;
            }

            boolean matchesFilter = false;
            ModulePatternMatcher matcher = patterns[side];

            for (int slot = 0; slot < 5; slot++) {
                ItemStack filter = inventory.getStackInSlot(side * 5 + slot);
                if (filter.isEmpty()) {
                    continue;
                }

                // Filter kicks in if any entry matches
                if (matcher.isValidForFilter(filter, slot, stack)) {
                    matchesFilter = true;
                    break;
                }
            }

            // Add dir if matches with whitelist on or doesn't match with blacklist on
            if ((mode == MODE_WHITELIST && matchesFilter) || (mode == MODE_BLACKLIST && !matchesFilter)) {
                validDirs.add(Direction.from3DDataValue(side));
            }
        }

        // If no valid dirs found, use wildcard
        if (validDirs.isEmpty()) {
            for (int side = 0; side < 6; side++) {
                if (modes[side] == MODE_WILDCARD) {
                    validDirs.add(Direction.from3DDataValue(side));
                }
            }
        }

        // No valid directions at all
        if (validDirs.isEmpty()) {
            return null;
        }

        // Random selection from valid directions (CE behavior)
        int i = level.random.nextInt(validDirs.size());
        return validDirs.get(i);
    }

    /**
     * Spawn EntityMovingItem on target conveyor belt, or drop as EntityItem if no belt.
     */
    private void sendOnRoute(ItemStack stack, Direction dir) {
        BlockPos targetPos = worldPosition.relative(dir);
        IConveyorBelt belt = null;

        if (level.getBlockState(targetPos).getBlock() instanceof IConveyorBelt) {
            belt = (IConveyorBelt) level.getBlockState(targetPos).getBlock();
        }

        if (belt != null) {
            EntityMovingItem moving = new EntityMovingItem(ConveyorEntityTypes.MOVING_ITEM.get(), level);
            Vec3 pos = new Vec3(
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.55,
                    worldPosition.getY() + 0.5 + dir.getStepY() * 0.55,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.55
            );
            Vec3 snap = belt.getClosestSnappingPosition(level, targetPos, pos);
            moving.setPos(snap.x, snap.y, snap.z);
            moving.setItemStack(stack.copy());
            level.addFreshEntity(moving);
        } else {
            level.addFreshEntity(new ItemEntity(level,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.55,
                    worldPosition.getY() + 0.5 + dir.getStepY() * 0.55,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.55,
                    stack.copy()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putIntArray("Modes", modes);
        for (int i = 0; i < patterns.length; i++) {
            CompoundTag patternTag = new CompoundTag();
            patterns[i].writeToNBT(patternTag);
            tag.put("Pattern" + i, patternTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Modes")) {
            modes = tag.getIntArray("Modes");
        }
        for (int i = 0; i < patterns.length; i++) {
            if (tag.contains("Pattern" + i)) {
                patterns[i].readFromNBT(tag.getCompound("Pattern" + i));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.crane_router");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraneRouterMenu(containerId, playerInventory, this);
    }
}
