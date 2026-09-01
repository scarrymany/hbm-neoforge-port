package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.FluidBarrelBlock;
import com.hbm.inventory.container.machine.dummyable.FluidBarrelMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityBarrel} — transceiver + mode. Canister load/unload / UniNodespace buffer skipped.
 */
public class FluidBarrelBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int MODE_BOTH = 0;
    public static final int MODE_IN = 1;
    public static final int MODE_OUT = 2;
    public static final int MODE_NONE = 3;

    public final FluidTankNTM tank;
    public int mode;
    private final FluidBarrelBlock.Kind kind;

    public FluidBarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, false);
        FluidBarrelBlock block = state.getBlock() instanceof FluidBarrelBlock b ? b : null;
        int cap = block != null ? block.capacity : 16_000;
        this.kind = block != null ? block.kind : FluidBarrelBlock.Kind.STEEL;
        this.tank = new FluidTankNTM(Fluids.NONE, cap).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.barrel");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    public void cycleMode() {
        mode = (mode + 1) % 4;
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(0);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            tank.setTankType(ident.getType(level, worldPosition, id));
        }

        if (tank.getFill() > 0) checkFluidInteraction();
        if (level.getBlockEntity(worldPosition) != this) return;

        if (level.getGameTime() % 20 == 0 && mode != MODE_NONE) {
            for (Direction d : Direction.values()) {
                DirPos p = new DirPos(worldPosition.relative(d), d);
                if (mode == MODE_BOTH || mode == MODE_IN) trySubscribe(tank.getTankType(), level, p);
                if ((mode == MODE_BOTH || mode == MODE_OUT) && tank.getFill() > 0) tryProvide(tank, level, p);
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void checkFluidInteraction() {
        if (level == null) return;
        if (kind != FluidBarrelBlock.Kind.ANTIMATTER && tank.getTankType().isAntimatter()) {
            explode();
            return;
        }
        if (kind == FluidBarrelBlock.Kind.PLASTIC
                && (tank.getTankType().isCorrosive() || tank.getTankType().isHot())) {
            level.destroyBlock(worldPosition, false);
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (kind == FluidBarrelBlock.Kind.CORRODED) {
            if (level.random.nextInt(3) == 0) tank.setFill(Math.max(0, tank.getFill() - 1));
            if (level.random.nextInt(3 * 60 * 20) == 0) level.destroyBlock(worldPosition, false);
        }
    }

    private void explode() {
        if (level == null) return;
        BlockPos p = worldPosition;
        level.destroyBlock(p, false);
        level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                5.0F, true, Level.ExplosionInteraction.TNT);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "t");
        tag.putInt("mode", mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "t");
        mode = tag.getInt("mode");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
        buf.writeInt(mode);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
        mode = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FluidBarrelMenu(id, inv, this);
    }
}
