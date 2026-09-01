package com.hbm.blockentity.machine.accel;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.generic.BlockBedrockOreTE;
import com.hbm.inventory.container.machine.accel.ExcavatorMenu;
import com.hbm.items.ItemEnums;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * CE {@code TileEntityMachineExcavator.java}: maxPower 10_000_000, baseConsumption 10_000.
 * Mines a downward column; silk/vein/fortune come from the drillbit (CE {@code ItemDrillbit} flags).
 */
public class ExcavatorBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    private static final int SLOT_DRILL = 0;
    private static final int SLOT_BATTERY = 1;
    private static final int SLOT_OUT_START = 2;
    private static final int SLOT_OUT_END = 10;
    public static final long MAX_POWER = 10_000_000L;
    public static final long BASE_CONSUMPTION = 10_000L;

    public long power;
    public boolean drilling;
    public int depth;

    public ExcavatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineExcavator");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        return slot == SLOT_DRILL && stack.getItem() instanceof ItemDrillbit;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= SLOT_OUT_START && slot <= SLOT_OUT_END;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, MAX_POWER);
        trySubscribe(level, worldPosition.above(), Direction.UP);

        ItemStack bit = inventory.getStackInSlot(SLOT_DRILL);
        drilling = bit.getItem() instanceof ItemDrillbit && power >= BASE_CONSUMPTION;
        if (drilling && level instanceof ServerLevel server) {
            mineTick(server, (ItemDrillbit) bit.getItem());
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void mineTick(ServerLevel server, ItemDrillbit drill) {
        ItemEnums.EnumDrillType type = drill.getType();
        int radius = type.vein ? 2 : 1;
        int speed = Math.max(1, (int) type.speed);

        for (int n = 0; n < speed; n++) {
            if (power < BASE_CONSUMPTION) break;
            BlockPos target = nextTarget(radius);
            if (target == null) break;
            BlockState state = server.getBlockState(target);
            if (state.getBlock() instanceof BlockBedrockOreTE) {
                if (!collectBedrock(server, target, type)) {
                    depth++;
                    continue;
                }
                power -= BASE_CONSUMPTION;
                return;
            }
            if (state.isAir() || state.getDestroySpeed(server, target) < 0) {
                depth++;
                continue;
            }
            ItemStack tool = new ItemStack(drill);
            if (type.silk) {
                var silk = server.registryAccess().holder(Enchantments.SILK_TOUCH);
                silk.ifPresent(h -> tool.enchant(h, 1));
            } else if (type.fortune > 0) {
                var fortune = server.registryAccess().holder(Enchantments.FORTUNE);
                fortune.ifPresent(h -> tool.enchant(h, type.fortune));
            }
            List<ItemStack> drops = Block.getDrops(state, server, target, server.getBlockEntity(target), null, tool);
            if (!canFit(drops)) break;
            server.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            for (ItemStack drop : drops) insertOutput(drop);
            power -= BASE_CONSUMPTION;
            depth++;
        }
    }

    /**
     * CE {@code TileEntityMachineExcavator.collectBedrock}: copy TE resource, fortune via
     * {@link ItemBedrockOreBase#setOreAmount}. Does not destroy the deposit. Acid-gated ores
     * are skipped — this excavator has no tank (CE does).
     */
    private boolean collectBedrock(ServerLevel server, BlockPos pos, ItemEnums.EnumDrillType drill) {
        if (!(server.getBlockEntity(pos) instanceof BlockBedrockOreTE.BedrockOreBlockEntity ore)) return false;
        if (ore.resource.isEmpty()) return false;
        if (ore.tier > drill.tier) return false;
        if (ore.acidRequirement != null) return false;

        ItemStack stack = ore.resource.copy();
        if (stack.is(BedrockOreItems.BEDROCK_ORE_BASE.get())) {
            ItemBedrockOreBase.setOreAmount(stack, pos.getX(), pos.getZ(), 1D + drill.fortune * 0.25D);
        }
        if (!canFit(List.of(stack))) return false;
        insertOutput(stack);
        return true;
    }

    private BlockPos nextTarget(int radius) {
        int y = worldPosition.getY() - 1 - (depth / ((radius * 2 + 1) * (radius * 2 + 1)));
        if (y < level.getMinBuildHeight()) return null;
        int idx = depth % ((radius * 2 + 1) * (radius * 2 + 1));
        int dx = (idx % (radius * 2 + 1)) - radius;
        int dz = (idx / (radius * 2 + 1)) - radius;
        return worldPosition.offset(dx, y - worldPosition.getY(), dz);
    }

    private boolean canFit(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            int left = drop.getCount();
            for (int i = SLOT_OUT_START; i <= SLOT_OUT_END && left > 0; i++) {
                ItemStack have = inventory.getStackInSlot(i);
                if (have.isEmpty()) {
                    left = 0;
                } else if (ItemStack.isSameItemSameComponents(have, drop)) {
                    left -= Math.min(left, have.getMaxStackSize() - have.getCount());
                }
            }
            if (left > 0) return false;
        }
        return true;
    }

    private void insertOutput(ItemStack drop) {
        ItemStack remaining = drop.copy();
        for (int i = SLOT_OUT_START; i <= SLOT_OUT_END && !remaining.isEmpty(); i++) {
            ItemStack have = inventory.getStackInSlot(i);
            if (have.isEmpty()) {
                inventory.setStackInSlot(i, remaining);
                remaining = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(have, remaining)) {
                int add = Math.min(remaining.getCount(), have.getMaxStackSize() - have.getCount());
                have.grow(add);
                remaining.shrink(add);
            }
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("depth", depth);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        depth = tag.getInt("depth");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(drilling);
        buf.writeInt(depth);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        drilling = buf.readBoolean();
        depth = buf.readInt();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExcavatorMenu(containerId, playerInventory, this);
    }
}
