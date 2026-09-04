package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MachineFluidTankMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.machine.IItemFluidIdentifier;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineFluidTank}: 6 slots, 256000, mode 0=in / 1=both / 2=out / 3=off.
 * CE :263-370 — post-explode leak/fire/pollute ✓ (FT_Polluting, updateLeak).
 * TODO(CE: TileEntityMachineFluidTank.java:198-235): UniNodespace pipe-mode node.
 * TODO(CE: TileEntityMachineFluidTank.java:70): OC / IControllable / IClimbable / IRepairable.
 * TODO(CE: TileEntityMachineFluidTank.java:253-256): ExplosionVNT.makeAmat.
 * TODO(CE: TileEntityMachineFluidTank.java:343): ExplosionVNT.makeAmat().setBlockAllocator(null).setBlockProcessor(null).
 * TODO(CE: TileEntityMachineFluidTank.java:348): ParticleUtil.spawnGasFlame particle.
 * TODO(CE: TileEntityMachineFluidTank.java:356-365): AuxParticlePacketNT Tower particle.
 * ROR: CE {@code TileEntityMachineFluidTank.java:652-682}.
 */
public class MachineFluidTankBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IRORValueProvider, IRORInteractive {

    public static final int CAPACITY = 256_000;
    public static final short MODES = 4;

    public final FluidTankNTM tank;
    public short mode;
    public boolean hasExploded;
    public boolean onFire;

    public MachineFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, false);
        this.tank = new FluidTankNTM(Fluids.NONE, CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fluidtank");
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return null;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidIdentifier;
            default -> true;
        };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        return switch (slot) {
            case 1, 3, 5 -> false;
            default -> isItemValidForSlot(slot, itemStack);
        };
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return switch (slot) {
            case 1, 3, 5 -> true;
            default -> !isItemValidForSlot(slot, itemStack);
        };
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (!hasExploded) {
            tank.loadTank(2, 3, inventory);
            tank.setType(0, 1, inventory);
        }

        if (tank.getFill() > 0) {
            if (tank.getTankType().isAntimatter()) {
                // TODO(CE: TileEntityMachineFluidTank.java:253-256): ExplosionVNT.makeAmat
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                        5.0F, true, Level.ExplosionInteraction.TNT);
                explode();
                tank.setFill(0);
            }
            FT_Corrosive corrosive = tank.getTankType().getTrait(FT_Corrosive.class);
            if (corrosive != null && corrosive.isHighlyCorrosive()) {
                explode();
            }
        }

        if (hasExploded) {
            int leaking;
            if (tank.getTankType().isAntimatter()) {
                leaking = tank.getFill();
            } else if (tank.getTankType().hasTrait(FluidTraitSimple.FT_Gaseous.class)
                    || tank.getTankType().hasTrait(FluidTraitSimple.FT_Gaseous_ART.class)) {
                leaking = Math.min(tank.getFill(), tank.getMaxFill() / 100);
            } else {
                leaking = Math.min(tank.getFill(), tank.getMaxFill() / 10000);
            }
            updateLeak(leaking);
        }

        if (!hasExploded) {
            for (DirPos pos : getConPos()) {
                if (mode == 0 || mode == 1) trySubscribe(tank.getTankType(), level, pos);
                if ((mode == 1 || mode == 2) && tank.getFill() > 0) tryProvide(tank, level, pos);
            }
        }

        tank.unloadTank(4, 5, inventory);
        dataChanged();
        networkPackMK2(150);
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Direction.EAST),
                new DirPos(x + 2, y, z + 1, Direction.EAST),
                new DirPos(x - 2, y, z - 1, Direction.WEST),
                new DirPos(x - 2, y, z + 1, Direction.WEST),
                new DirPos(x - 1, y, z + 2, Direction.SOUTH),
                new DirPos(x + 1, y, z + 2, Direction.SOUTH),
                new DirPos(x - 1, y, z - 2, Direction.NORTH),
                new DirPos(x + 1, y, z - 2, Direction.NORTH),
        };
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        if (hasExploded) return List.of();
        return (mode == 0 || mode == 1) ? List.of(tank) : List.of();
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        if (hasExploded) return List.of();
        return (mode == 1 || mode == 2) ? List.of(tank) : List.of();
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putShort("mode", mode);
        tag.putBoolean("hasExploded", hasExploded);
        tag.putBoolean("onFire", onFire);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "tank");
        mode = tag.getShort("mode");
        hasExploded = tag.getBoolean("hasExploded");
        onFire = tag.getBoolean("onFire");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        buf.writeBoolean(hasExploded);
        buf.writeBoolean(onFire);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        hasExploded = buf.readBoolean();
        onFire = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineFluidTankMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :652-654
        return new String[]{
                PREFIX_VALUE + "type", PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :657-661
        if ((PREFIX_VALUE + "type").equals(name)) return tank.getTankType().getName();
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (tank.getFill() * 100 / tank.getMaxFill());
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :665-682
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int next = IRORInteractive.parseInt(params[0], 0, 3);
            if (next != this.mode) {
                this.mode = (short) next;
                setChanged();
                return null;
            } else if (params.length > 1) {
                this.mode = (short) IRORInteractive.parseInt(params[1], 0, 3);
                setChanged();
                return null;
            }
            return null;
        }
        return null;
    }

    /**
     * CE {@code TileEntityMachineFluidTank.explode} :315-328 — called when tank breaks, sets {@code hasExploded} and {@code onFire}.
     */
    public void explode() {
        this.hasExploded = true;
        this.onFire = tank.getTankType().hasTrait(FT_Flammable.class);
        setChanged();
    }

    /**
     * CE {@code TileEntityMachineFluidTank.updateLeak} :330-370 — called every tick post-explosion, leaks fluid and spawns particles.
     */
    public void updateLeak(int amount) {
        if (!hasExploded) return;
        if (amount <= 0) return;

        tank.getTankType().onFluidRelease(this, tank, amount);
        tank.setFill(Math.max(0, tank.getFill() - amount));

        var type = tank.getTankType();

        if (type.hasTrait(FluidTraitSimple.FT_Amat.class)) {
            // TODO(CE: TileEntityMachineFluidTank.java:343): ExplosionVNT.makeAmat().setBlockAllocator(null).setBlockProcessor(null)
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                    5.0F, Level.ExplosionInteraction.TNT);

        } else if (type.hasTrait(FT_Flammable.class) && onFire) {
            var box = new AABB(
                    worldPosition.getX() - 1.5, worldPosition.getY(), worldPosition.getZ() - 1.5,
                    worldPosition.getX() + 2.5, worldPosition.getY() + 5, worldPosition.getZ() + 2.5
            );
            level.getEntities(null, box).forEach(e -> e.igniteForSeconds(5));

            // TODO(CE: TileEntityMachineFluidTank.java:348): ParticleUtil.spawnGasFlame

            if (level.getGameTime() % 5 == 0) {
                FT_Polluting.pollute(level, worldPosition, tank.getTankType(), FluidTrait.FluidReleaseType.BURN, amount * 5);
            }

        } else if (type.hasTrait(FluidTraitSimple.FT_Gaseous.class) || type.hasTrait(FluidTraitSimple.FT_Gaseous_ART.class)) {

            // TODO(CE: TileEntityMachineFluidTank.java:356-365): AuxParticlePacketNT Tower particle

            if (level.getGameTime() % 5 == 0) {
                FT_Polluting.pollute(level, worldPosition, tank.getTankType(), FluidTrait.FluidReleaseType.SPILL, amount * 5);
            }
        }
    }
}
