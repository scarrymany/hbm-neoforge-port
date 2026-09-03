package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AshpitMenu;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.ItemEnums.EnumAshType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityAshpit} — 5 output slots, door animation, ash→powder conversion.
 * Minimal port: wood ash only for now (other ash types deferred).
 */
public class MachineAshpitBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public int playersUsing = 0;
    public float doorAngle = 0;
    public float prevDoorAngle = 0;
    public int ashLevelWood = 0;
    public static final int THRESHOLD_WOOD = 2000;

    public MachineAshpitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.ashpit");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4};
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            // CE TileEntityAshpit.java:118-132: ash→powder conversion (wood ash only)
            if (ashLevelWood >= THRESHOLD_WOOD) {
                if (processAsh(ashLevelWood, EnumAshType.WOOD, THRESHOLD_WOOD)) {
                    ashLevelWood -= THRESHOLD_WOOD;
                }
            }

            dataChanged();
            networkPackMK2(50);
        } else {
            // CE TileEntityAshpit.java:102-114: door animation
            prevDoorAngle = doorAngle;
            float swingSpeed = (doorAngle / 10F) + 3;

            if (playersUsing > 0) {
                doorAngle += swingSpeed;
            } else {
                doorAngle -= swingSpeed;
            }

            doorAngle = Mth.clamp(doorAngle, 0F, 135F);
        }
    }

    protected boolean processAsh(int level, EnumAshType type, int threshold) {
        if (level >= threshold) {
            for (int i = 0; i < 5; i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (slot.isEmpty()) {
                    inventory.setStackInSlot(i, new ItemStack(BilletPowderItems.powderAsh(type).get(), 1));
                    return true;
                } else if (slot.is(BilletPowderItems.powderAsh(type).get()) && slot.getCount() < slot.getMaxStackSize()) {
                    slot.grow(1);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AshpitMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("playersUsing", playersUsing);
        tag.putInt("ashLevelWood", ashLevelWood);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        playersUsing = tag.getInt("playersUsing");
        ashLevelWood = tag.getInt("ashLevelWood");
    }
}
