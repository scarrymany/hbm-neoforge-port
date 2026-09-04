package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.FluidBarrelBlock;
import com.hbm.inventory.container.machine.dummyable.FluidBarrelMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.uninos.UniNodespace;
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

import java.util.HashSet;
import java.util.List;

/**
 * CE {@code TileEntityBarrel} — transceiver + mode.
 * UniNodespace buffer: CE {@code TileEntityBarrel.java:247-286} / {@code createNode :296-307}.
 * Modes Exact CE: 0=in, 1=both/pipe, 2=out, 3=off.
 * {@code setType(0,1)} / {@code loadTank(2,3)} / {@code unloadTank(4,5)} Exact CE {@code :235-237}.
 * ROR: CE {@code TileEntityBarrel.java:473-504}.
 */
public class FluidBarrelBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IRORValueProvider, IRORInteractive {

    /** CE: 0 receive, 1 both (own pipe node), 2 send, 3 disabled. */
    public static final int MODE_IN = 0;
    public static final int MODE_BOTH = 1;
    public static final int MODE_OUT = 2;
    public static final int MODE_NONE = 3;

    // CE TileEntityBarrel.java:62-64
    private static final int[] SLOTS_TOP = new int[]{2};
    private static final int[] SLOTS_BOTTOM = new int[]{3, 5};
    private static final int[] SLOTS_SIDE = new int[]{4};

    public final FluidTankNTM tank;
    public int mode;
    private final FluidBarrelBlock.Kind kind;
    protected FluidNode node;
    protected FluidType lastType = Fluids.NONE;

    public FluidBarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, false);
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
        // CE TileEntityBarrel.java:446-453 (Library drain/fill helpers not ported — same as fluid tank)
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidIdentifier;
            default -> true;
        };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack) {
        // CE TileEntityBarrel.java:457-462
        return switch (slot) {
            case 1, 3, 5 -> false;
            default -> isItemValidForSlot(slot, itemStack);
        };
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        // CE TileEntityBarrel.java:465-470
        return switch (slot) {
            case 1, 3, 5 -> true;
            default -> !isItemValidForSlot(slot, itemStack);
        };
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        // CE TileEntityBarrel.java:440-443
        if (side == Direction.DOWN) return SLOTS_BOTTOM;
        if (side == Direction.UP) return SLOTS_TOP;
        return SLOTS_SIDE;
    }

    public void cycleMode() {
        mode = (mode + 1) % 4;
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityBarrel.java:235-237
        this.tank.setType(0, 1, inventory);
        this.tank.loadTank(2, 3, inventory);
        this.tank.unloadTank(4, 5, inventory);

        // CE TileEntityBarrel.java:247-286 — mode 1 = own pipe node; tilt skipped
        if (mode == 1) {
            if (this.node == null || this.node.expired || tank.getTankType() != lastType) {
                this.node = UniNodespace.getNode(level, worldPosition, tank.getTankType().getNetworkProvider());
                if (this.node == null || this.node.expired || tank.getTankType() != lastType) {
                    this.node = this.createNode(tank.getTankType());
                    UniNodespace.createNode(level, this.node);
                    lastType = tank.getTankType();
                }
            }
            if (node != null && node.hasValidNet()) {
                node.net.addProvider(this);
                node.net.addReceiver(this);
            }
        } else {
            if (this.node != null) {
                UniNodespace.destroyNode(level, worldPosition, tank.getTankType().getNetworkProvider());
                this.node = null;
            }
            for (DirPos pos : getConPos()) {
                FluidNode dirNode = UniNodespace.getNode(level, pos.getPos(), tank.getTankType().getNetworkProvider());
                if (mode == 2) {
                    tryProvide(tank, level, pos);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeProvider(this);
                }
                if (mode == 0) {
                    if (dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(this);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeReceiver(this);
                }
            }
        }

        if (tank.getFill() > 0) checkFluidInteraction();
        if (level.getBlockEntity(worldPosition) != this) return;

        dataChanged();
        networkPackMK2(50);
    }

    /** CE {@code TileEntityBarrel.java:296-307} / {@code getConPos :327-329}. */
    protected FluidNode createNode(FluidType type) {
        DirPos[] conPos = getConPos();
        HashSet<BlockPos> posSet = new HashSet<>();
        posSet.add(worldPosition);
        for (DirPos p : conPos) {
            Direction dir = p.getDir();
            posSet.add(p.getPos().relative(dir.getOpposite()));
        }
        return new FluidNode(type.getNetworkProvider(), posSet.toArray(new BlockPos[0])).setConnections(conPos);
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Direction.EAST),
                new DirPos(x - 1, y, z, Direction.WEST),
                new DirPos(x, y + 1, z, Direction.UP),
                new DirPos(x, y - 1, z, Direction.DOWN),
                new DirPos(x, y, z + 1, Direction.SOUTH),
                new DirPos(x, y, z - 1, Direction.NORTH),
        };
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && this.node != null) {
            UniNodespace.destroyNode(level, worldPosition, tank.getTankType().getNetworkProvider());
            this.node = null;
        }
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
        // CE :405
        return (mode == 0 || mode == 1) ? List.of(tank) : List.of();
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        // CE :400
        return (mode == 1 || mode == 2) ? List.of(tank) : List.of();
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

    @Override
    public String[] getFunctionInfo() {
        // CE :473-475
        return new String[]{
                PREFIX_VALUE + "type", PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :478-482
        if ((PREFIX_VALUE + "type").equals(name)) return tank.getTankType().getName();
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (tank.getFill() * 100 / tank.getMaxFill());
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :486-504
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int next = IRORInteractive.parseInt(params[0], 0, 3);
            if (next != this.mode) {
                this.mode = next;
                setChanged();
                return null;
            } else if (params.length > 1) {
                this.mode = IRORInteractive.parseInt(params[1], 0, 3);
                setChanged();
                return null;
            }
            return null;
        }
        return null;
    }
}
