package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AshpitMenu;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.tileentity.IConfigurableMachine;
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

import java.io.IOException;

/**
 * CE {@code TileEntityAshpit} — 5 output slots, door animation, ash→powder conversion.
 * All 5 ash types: wood/coal/misc/fly/soot (fullerene not used by CE ashpit).
 * {@link IConfigurableMachine} Exact CE {@code TileEntityAshpit.java:55-76} ({@code ashpit}).
 */
public class MachineAshpitBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public int playersUsing = 0;
    public float doorAngle = 0;
    public float prevDoorAngle = 0;
    public boolean isFull = false;

    public int ashLevelWood = 0;
    public int ashLevelCoal = 0;
    public int ashLevelMisc = 0;
    public int ashLevelFly = 0;
    public int ashLevelSoot = 0;

    // CE TileEntityAshpit.java:45-49
    public static int thresholdWood = 2000;
    public static int thresholdCoal = 2000;
    public static int thresholdMisc = 2000;
    public static int thresholdFly = 2000;
    public static int thresholdSoot = 8000;

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
            // CE TileEntityAshpit.java:89-93: process all 5 ash types
            if (processAsh(ashLevelWood, EnumAshType.WOOD, thresholdWood)) ashLevelWood -= thresholdWood;
            if (processAsh(ashLevelCoal, EnumAshType.COAL, thresholdCoal)) ashLevelCoal -= thresholdCoal;
            if (processAsh(ashLevelMisc, EnumAshType.MISC, thresholdMisc)) ashLevelMisc -= thresholdMisc;
            if (processAsh(ashLevelFly, EnumAshType.FLY, thresholdFly)) ashLevelFly -= thresholdFly;
            if (processAsh(ashLevelSoot, EnumAshType.SOOT, thresholdSoot)) ashLevelSoot -= thresholdSoot;

            // CE TileEntityAshpit.java:95-99: isFull flag
            isFull = false;
            for (int i = 0; i < 5; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    isFull = true;
                    break;
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
        tag.putInt("ashLevelCoal", ashLevelCoal);
        tag.putInt("ashLevelMisc", ashLevelMisc);
        tag.putInt("ashLevelFly", ashLevelFly);
        tag.putInt("ashLevelSoot", ashLevelSoot);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        playersUsing = tag.getInt("playersUsing");
        ashLevelWood = tag.getInt("ashLevelWood");
        ashLevelCoal = tag.getInt("ashLevelCoal");
        ashLevelMisc = tag.getInt("ashLevelMisc");
        ashLevelFly = tag.getInt("ashLevelFly");
        ashLevelSoot = tag.getInt("ashLevelSoot");
    }

    static void readAshpit(JsonObject obj) {
        // CE TileEntityAshpit.java:62-66
        thresholdWood = IConfigurableMachine.grab(obj, "I:thresholdWood", thresholdWood);
        thresholdCoal = IConfigurableMachine.grab(obj, "I:thresholdCoal", thresholdCoal);
        thresholdMisc = IConfigurableMachine.grab(obj, "I:thresholdMisc", thresholdMisc);
        thresholdFly = IConfigurableMachine.grab(obj, "I:thresholdFly", thresholdFly);
        thresholdSoot = IConfigurableMachine.grab(obj, "I:thresholdSoot", thresholdSoot);
    }

    static void writeAshpit(JsonWriter writer) throws IOException {
        // CE TileEntityAshpit.java:71-75
        writer.name("I:thresholdWood").value(thresholdWood);
        writer.name("I:thresholdCoal").value(thresholdCoal);
        writer.name("I:thresholdMisc").value(thresholdMisc);
        writer.name("I:thresholdFly").value(thresholdFly);
        writer.name("I:thresholdSoot").value(thresholdSoot);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "ashpit";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readAshpit(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeAshpit(writer);
        }
    }
}
