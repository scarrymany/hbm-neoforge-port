package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import com.hbm.inventory.container.machine.dummyable.StirlingMenu;
import com.hbm.items.machine.ItemGear;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityStirling.java}:59-181 — heat pull diffusion 0.1, efficiency 0.5,
 * maxHeat 300/1500, overspeed 300. EntityCog spawn skipped.
 */
public class MachineStirlingBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, ITickableBE, MenuProvider {

    public static final double DIFFUSION = 0.1D;
    public static final double EFFICIENCY = 0.5D;
    public static final int MAX_HEAT_NORMAL = 300;
    public static final int MAX_HEAT_STEEL = 1500;
    public static final int OVERSPEED_LIMIT = 300;

    public long powerBuffer;
    public int heat;
    public boolean hasCog = true;
    public int overspeed;

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
        return getBlockState().is(DummyableProcessBlocks.MACHINE_STIRLING.get()) ? MAX_HEAT_NORMAL : MAX_HEAT_STEEL;
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
            powerBuffer = (long) (heat * (isCreative() ? 1D : EFFICIENCY));
            if (!isCreative()) {
                if (heat > maxHeat()) {
                    overspeed++;
                    if (overspeed > OVERSPEED_LIMIT) {
                        hasCog = false;
                        powerBuffer = 0;
                        level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                                5.0F, false, Level.ExplosionInteraction.NONE);
                    }
                } else {
                    overspeed = 0;
                }
            }
        } else {
            powerBuffer = 0;
            heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
        }

        if (powerBuffer > 0) {
            for (DirPos pos : getConPos()) tryProvide(level, pos.getPos(), pos.getDir());
        }
        heat = 0;

        dataChanged();
        networkPackMK2(25);
    }

    private void tryPullHeat() {
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source && below != this) {
            int heatSrc = (int) (source.getHeatStored() * DIFFUSION);
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
        return Math.max(1L, (long) (maxHeat() * (isCreative() ? 1D : EFFICIENCY)));
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
        buf.writeInt(heat);
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
