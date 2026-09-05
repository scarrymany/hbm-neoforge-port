package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import com.hbm.inventory.container.machine.dummyable.StirlingMenu;
import com.hbm.items.machine.ItemGear;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;

/**
 * CE {@code TileEntityStirling.java}:59-181 — heat pull diffusion 0.1, efficiency 0.5,
 * maxHeat 300/1500, overspeed 300. EntityCog spawn skipped.
 * Overspeed warning Exact CE {@code :65-75}: {@code warnOverspeed} 2.0F/1.0F after
 * {@code overspeed > 60}, cooldown 100t. {@link IConfigurableMachine} Exact CE
 * {@code TileEntityStirling.java:233-253} ({@code stirling}).
 */
public class MachineStirlingBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, ITickableBE, MenuProvider, IConfigurableMachine {

    public static double diffusion = 0.1D;
    public static double efficiency = 0.5D;
    public static int maxHeatNormal = 300;
    public static int maxHeatSteel = 1500;
    public static int overspeedLimit = 300;

    public long powerBuffer;
    public int heat;
    public boolean hasCog = true;
    public int overspeed;
    private int warnCooldown;
    private int syncHeat;

    public MachineStirlingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineStirling");
    }

    public boolean isCreative() {
        return getBlockState().is(DummyableProcessBlocks.MACHINE_STIRLING_CREATIVE.get());
    }

    public boolean isSteel() {
        return getBlockState().is(DummyableProcessBlocks.MACHINE_STIRLING_STEEL.get());
    }

    public int maxHeat() {
        return getBlockState().is(DummyableProcessBlocks.MACHINE_STIRLING.get()) ? maxHeatNormal : maxHeatSteel;
    }

    public ItemGear.GearType requiredGear() {
        return isSteel() ? ItemGear.GearType.STEEL : ItemGear.GearType.BRONZE;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (hasCog) {
            powerBuffer = 0;
            tryPullHeat();
            powerBuffer = (long) (heat * (isCreative() ? 1D : efficiency));
            // CE TileEntityStirling.java:65-75
            if (warnCooldown > 0) warnCooldown--;
            if (heat > maxHeat() && !isCreative()) {
                overspeed++;
                if (overspeed > 60 && warnCooldown == 0) {
                    warnCooldown = 100;
                    level.playSound(null, worldPosition, HBMSoundHandler.warnOverspeed.get(),
                            SoundSource.BLOCKS, 2.0F, 1.0F);
                }
                if (overspeed > overspeedLimit) {
                    hasCog = false;
                    powerBuffer = 0;
                    level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                            5.0F, false, Level.ExplosionInteraction.NONE);
                }
            } else {
                overspeed = 0;
            }
        } else {
            // CE TileEntityStirling.java:97-99
            overspeed = 0;
            warnCooldown = 0;
            powerBuffer = 0;
            heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
        }

        if (powerBuffer > 0) {
            for (DirPos pos : getConPos()) tryProvide(level, pos.getPos(), pos.getDir());
        }
        // CE TileEntityStirling.java:101-114 — snapshot then pack then zero
        syncHeat = heat;
        dataChanged();
        networkPackMK2(25);
        heat = 0;
    }

    private void tryPullHeat() {
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source && below != this) {
            int heatSrc = (int) (source.getHeatStored() * diffusion);
            if (heatSrc > 0) {
                source.useUpHeat(heatSrc);
                heat += heatSrc;
            }
        } else {
            heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
        }
    }

    public boolean tryInstallCog(ItemStack stack) {
        if (hasCog || isCreative()) return false;
        if (!(stack.getItem() instanceof ItemGear gear)) return false;
        if (gear.getType() != requiredGear()) return false;
        hasCog = true;
        overspeed = 0;
        setChanged();
        return true;
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.east(2), Direction.EAST),
                new DirPos(worldPosition.west(2), Direction.WEST),
                new DirPos(worldPosition.south(2), Direction.SOUTH),
                new DirPos(worldPosition.north(2), Direction.NORTH),
        };
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long power) {
        this.powerBuffer = power;
    }

    @Override
    public long getMaxPower() {
        return Math.max(1L, (long) (maxHeat() * (isCreative() ? 1D : efficiency)));
    }

    @Override
    public String getConfigName() {
        return "stirling";
    }

    @Override
    public void readIfPresent(JsonObject obj) {
        readConfig(obj);
    }

    @Override
    public void writeConfig(JsonWriter writer) throws IOException {
        writeConfigStatic(writer);
    }

    static void readConfig(JsonObject obj) {
        // CE TileEntityStirling.java:239-243
        diffusion = IConfigurableMachine.grab(obj, "D:diffusion", diffusion);
        efficiency = IConfigurableMachine.grab(obj, "D:efficiency", efficiency);
        maxHeatNormal = IConfigurableMachine.grab(obj, "I:maxHeatNormal", maxHeatNormal);
        maxHeatSteel = IConfigurableMachine.grab(obj, "I:maxHeatSteel", maxHeatSteel);
        overspeedLimit = IConfigurableMachine.grab(obj, "I:overspeedLimit", overspeedLimit);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityStirling.java:248-252
        writer.name("D:diffusion").value(diffusion);
        writer.name("D:efficiency").value(efficiency);
        writer.name("I:maxHeatNormal").value(maxHeatNormal);
        writer.name("I:maxHeatSteel").value(maxHeatSteel);
        writer.name("I:overspeedLimit").value(overspeedLimit);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "stirling";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readConfig(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeConfigStatic(writer);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", powerBuffer);
        tag.putInt("heat", heat);
        tag.putBoolean("cog", hasCog);
        tag.putInt("over", overspeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        powerBuffer = tag.getLong("power");
        heat = tag.getInt("heat");
        hasCog = !tag.contains("cog") || tag.getBoolean("cog");
        overspeed = tag.getInt("over");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(powerBuffer);
        buf.writeInt(syncHeat);
        buf.writeBoolean(hasCog);
        buf.writeInt(overspeed);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        powerBuffer = buf.readLong();
        heat = buf.readInt();
        hasCog = buf.readBoolean();
        overspeed = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StirlingMenu(id, inv, this);
    }
}
