package com.hbm.blockentity.machine.chem;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.chem.GasCentrifugeMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.chem.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.chem.GasCentrifugeRecipes.PseudoFluidType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineGasCent} - the real isotope-separation machine
 * ({@code docs/phase2/machines_chemical_isotope.md}'s headline finding). The enrichment math
 * ({@link #enrich()}/{@link #canEnrich()}, and every number in {@link GasCentrifugeRecipes}) is
 * preserved exactly; see that recipe class's own header for the one documented item substitution.
 * <p>
 * Slot 5 fluid-ID is Exact CE {@code TileEntityMachineGasCent.java:193}/{@code :346-362}:
 * {@code setTankType(5)} retypes {@code tank} + pseudo in/out from {@link IItemFluidIdentifier}
 * when {@link GasCentrifugeRecipes#FLUID_CONVERSIONS} has the type (UF6/PUF6/WATZ).
 * Slot 5 @ 91,15 / upgrade slot 6 Exact CE {@code ContainerMachineGasCent.java:48-51}.
 * {@code gui_centrifuge_gas.png} is not in this tree — do not invent it.
 * <p>
 * <b>Scope trims from CE</b>: no looped centrifuge audio (same precedent as
 * {@code MachineRefineryBlockEntity}).
 */
public class GasCentrifugeBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    public static final long MAX_POWER = 100_000L;
    public static final int PROCESSING_SPEED = 150;
    private static final int[] SLOTS_IO = new int[]{0, 1, 2, 3};

    public static final int BATTERY_SLOT = 4;
    public static final int SLOT_ID = 5;
    public static final int UPGRADE_SLOT = 6;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.UF6, 2000).withOwner(this);
    public final PseudoFluidTank inputTank = new PseudoFluidTank(GasCentrifugeRecipes.PseudoFluidType.NUF6, 8000);
    public final PseudoFluidTank outputTank = new PseudoFluidTank(GasCentrifugeRecipes.PseudoFluidType.LEUF6, 8000);

    public int progress;
    public long power;
    public boolean isProgressing;

    private static Item gcSpeedUpgrade;

    public GasCentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 7, true, true);
    }

    @Override
    protected ItemStackHandler getNewInventory(int scount, int slotlimit) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                super.setStackInSlot(slot, stack);
                // CE TileEntityMachineGasCent.java:81
                if (!stack.isEmpty() && slot == UPGRADE_SLOT
                        && stack.getItem() instanceof ItemMachineUpgrade && level != null && !level.isClientSide) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotlimit;
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gasCentrifuge");
    }

    /** Lazily resolves {@code hbm:upgrade_gc_speed} by registry name - see class javadoc for why no direct field exists to reference. */
    private static Item speedUpgradeItem() {
        if (gcSpeedUpgrade == null) {
            gcSpeedUpgrade = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "upgrade_gc_speed"));
        }
        return gcSpeedUpgrade;
    }

    private boolean hasSpeedUpgrade() {
        Item upgrade = speedUpgradeItem();
        ItemStack stack = inventory.getStackInSlot(UPGRADE_SLOT);
        return upgrade != null && !stack.isEmpty() && stack.getItem() == upgrade;
    }

    public int getProcessingSpeed() {
        return hasSpeedUpgrade() ? PROCESSING_SPEED - 70 : PROCESSING_SPEED;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == BATTERY_SLOT) return Library.isBattery(stack);
        if (i == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        if (i == UPGRADE_SLOT) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot < 4;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return SLOTS_IO;
    }

    public int getCentrifugeProgressScaled(int i) {
        return (progress * i) / getProcessingSpeed();
    }

    public long getPowerRemainingScaled(int i) {
        return (power * i) / MAX_POWER;
    }

    private boolean canEnrich() {
        if (power <= 0) return false;
        PseudoFluidType stage = inputTank.getTankType();
        if (inputTank.getFill() < stage.getFluidConsumed()) return false;
        if (outputTank.getFill() + stage.getFluidProduced() > outputTank.getMaxFill()) return false;

        if (stage.getIfHighSpeed() && !hasSpeedUpgrade()) return false;

        ItemStack[] list = stage.getOutput();
        if (list == null || list.length < 1) return false;

        return doesOutputHaveSpace(list);
    }

    /** Ported from CE's {@code InventoryUtil.doesArrayHaveSpace} at this call site: can every output item fit into slots 0-3? */
    private boolean doesOutputHaveSpace(ItemStack[] outputs) {
        for (ItemStack out : outputs) {
            ItemStack remainder = simulateInsert(out);
            if (!remainder.isEmpty()) return false;
        }
        return true;
    }

    private ItemStack simulateInsert(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < 4 && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, true);
        }
        return remaining;
    }

    private void tryAddOutputs(ItemStack[] outputs) {
        for (ItemStack out : outputs) {
            ItemStack remaining = out.copy();
            for (int slot = 0; slot < 4 && !remaining.isEmpty(); slot++) {
                remaining = inventory.insertItem(slot, remaining, false);
            }
        }
    }

    private void enrich() {
        PseudoFluidType stage = inputTank.getTankType();
        ItemStack[] output = stage.getOutput();

        this.progress = 0;
        inputTank.setFill(inputTank.getFill() - stage.getFluidConsumed());
        outputTank.setFill(outputTank.getFill() + stage.getFluidProduced());

        for (ItemStack out : output) tryAddOutputs(new ItemStack[]{out});
    }

    private void attemptConversion() {
        if (inputTank.getFill() < inputTank.getMaxFill() && tank.getFill() > 0) {
            int fill = Math.min(inputTank.getMaxFill() - inputTank.getFill(), tank.getFill());
            tank.setFill(tank.getFill() - fill);
            inputTank.setFill(inputTank.getFill() + fill);
        }
    }

    /** CE {@code TileEntityMachineGasCent.setTankType} :346-362. */
    public void setTankType(int in) {
        ItemStack stack = inventory.getStackInSlot(in);
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidIdentifier id)) return;
        FluidType newType = id.getType(level, worldPosition, stack);
        if (tank.getTankType() == newType) return;
        PseudoFluidType pseudo = GasCentrifugeRecipes.FLUID_CONVERSIONS.get(newType);
        if (pseudo == null) return;
        inputTank.setTankType(pseudo);
        outputTank.setTankType(pseudo.getOutputType());
        tank.setTankType(newType);
    }

    /** Adopts the real tank's feed fluid as this stage's pseudo-fluid the moment a matching fluid arrives. */
    private void syncPseudoTypeFromTank() {
        PseudoFluidType pseudo = GasCentrifugeRecipes.FLUID_CONVERSIONS.get(tank.getTankType());
        if (pseudo != null && inputTank.getTankType() != pseudo) {
            inputTank.setTankType(pseudo);
            outputTank.setTankType(pseudo.getOutputType());
        }
    }

    private boolean attemptTransfer(GasCentrifugeBlockEntity downstream) {
        if (downstream.tank.getFill() == 0 && downstream.tank.getTankType() == tank.getTankType()) {
            if (downstream.inputTank.getTankType() != outputTank.getTankType() && outputTank.getTankType() != GasCentrifugeRecipes.PseudoFluidType.NONE) {
                downstream.inputTank.setTankType(outputTank.getTankType());
                downstream.outputTank.setTankType(outputTank.getTankType().getOutputType());
            }

            if (downstream.inputTank.getFill() < downstream.inputTank.getMaxFill() && outputTank.getFill() > 0) {
                int fill = Math.min(downstream.inputTank.getMaxFill() - downstream.inputTank.getFill(), outputTank.getFill());
                outputTank.setFill(outputTank.getFill() - fill);
                downstream.inputTank.setFill(downstream.inputTank.getFill() + fill);
            }
            return true;
        }
        return false;
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX(), p.getY() - 1, p.getZ(), Direction.DOWN),
                new DirPos(p.getX() + 1, p.getY(), p.getZ(), Direction.EAST),
                new DirPos(p.getX() - 1, p.getY(), p.getZ(), Direction.WEST),
                new DirPos(p.getX(), p.getY(), p.getZ() + 1, Direction.SOUTH),
                new DirPos(p.getX(), p.getY(), p.getZ() - 1, Direction.NORTH)
        };
    }

    /** The core-facing direction this centrifuge was placed in, or {@code null} if not a resolved core. */
    private Direction facingDirection() {
        BlockState state = getBlockState();
        if (!state.hasProperty(BlockDummyable.META)) return null;
        int meta = state.getValue(BlockDummyable.META);
        if (meta < 12) return null;
        return Direction.from3DDataValue(meta - BlockDummyable.offset);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            trySubscribe(tank.getTankType(), level, dp);
        }

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);
        setTankType(SLOT_ID);

        syncPseudoTypeFromTank();

        if (GasCentrifugeRecipes.FLUID_CONVERSIONS.containsValue(inputTank.getTankType())) {
            attemptConversion();
        }

        if (canEnrich()) {
            isProgressing = true;
            progress++;

            power -= hasSpeedUpgrade() ? 300 : 200;
            if (power < 0) {
                power = 0;
                progress = 0;
            }

            if (progress >= getProcessingSpeed()) enrich();
        } else {
            isProgressing = false;
            progress = 0;
        }

        // CE: TileEntityMachineGasCent.getLoopedSound() - continuous AudioWrapper loop
        // (HBMSoundHandler.centrifugeOperate, 20-tick keepAlive) while enriching. No looped-block-audio
        // bridge ported yet (see CentrifugeBlockEntity's identical note); substituted with a periodic
        // broadcast every 20 ticks while progressing.
        if (isProgressing && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.centrifugeOperate.get(), SoundSource.BLOCKS, 1F, 1.0F);
        }

        if (level.getGameTime() % 10 == 0) {
            Direction dir = facingDirection();
            boolean transferred = false;

            if (dir != null) {
                BlockPos upstreamPos = worldPosition.relative(dir.getOpposite());
                if (level.getBlockEntity(upstreamPos) instanceof GasCentrifugeBlockEntity upstream) {
                    transferred = attemptTransfer(upstream);
                }
            }

            if (!transferred && inputTank.getTankType() == GasCentrifugeRecipes.PseudoFluidType.LEUF6) {
                if (outputTank.getFill() >= 600 && doesOutputHaveSpace(fuelEscapeHatchOutput())) {
                    outputTank.setFill(outputTank.getFill() - 600);
                    tryAddOutputs(fuelEscapeHatchOutput());
                }
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    private ItemStack[] fuelEscapeHatchOutput() {
        return new ItemStack[]{
                new ItemStack(com.hbm.items.IngotNuggetItems.NUGGET_URANIUM_FUEL.get(), 6),
                new ItemStack(com.hbm.items.PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1)
        };
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
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tank.writeToNBT(tag, "tank");
        inputTank.writeToNBT(tag, "inputTank");
        outputTank.writeToNBT(tag, "outputTank");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        tank.readFromNBT(tag, "tank");
        inputTank.readFromNBT(tag, "inputTank");
        outputTank.readFromNBT(tag, "outputTank");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeBoolean(isProgressing);
        buf.writeInt(inputTank.getFill());
        buf.writeInt(outputTank.getFill());
        buf.writeUtf(inputTank.getTankType().name);
        buf.writeUtf(outputTank.getTankType().name);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        isProgressing = buf.readBoolean();
        inputTank.setFill(buf.readInt());
        outputTank.setFill(buf.readInt());
        inputTank.setTankType(PseudoFluidType.TYPES.get(buf.readUtf()));
        outputTank.setTankType(PseudoFluidType.TYPES.get(buf.readUtf()));
        tank.deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        if (tank.getFill() <= 0 && inputTank.getFill() <= 0 && outputTank.getFill() <= 0) return;
        tank.writeToNBT(nbt, "tank");
        inputTank.writeToNBT(nbt, "inputTank");
        outputTank.writeToNBT(nbt, "outputTank");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tank.readFromNBT(nbt, "tank");
        inputTank.readFromNBT(nbt, "inputTank");
        outputTank.readFromNBT(nbt, "outputTank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasCentrifugeMenu(containerId, playerInventory, this);
    }

    /**
     * Ported from CE's {@code TileEntityMachineGasCent.PseudoFluidTank} - a lightweight fluid-amount
     * tracker for the pseudo-fluid enrichment chain, deliberately not {@link FluidTankNTM} (the
     * pseudo-fluids it tracks are not real registered {@link FluidType}s, see
     * {@link GasCentrifugeRecipes}'s header).
     */
    public static final class PseudoFluidTank {
        private PseudoFluidType type;
        private int fill;
        private int maxFill;

        public PseudoFluidTank(PseudoFluidType type, int maxFill) {
            this.type = type;
            this.maxFill = maxFill;
        }

        public void setFill(int i) {
            fill = Math.max(0, Math.min(i, maxFill));
        }

        public void setTankType(PseudoFluidType type) {
            PseudoFluidType next = type == null ? GasCentrifugeRecipes.PseudoFluidType.NONE : type;
            if (this.type == next) return;
            this.type = next;
            setFill(0);
        }

        public PseudoFluidType getTankType() {
            return type;
        }

        public int getFill() {
            return fill;
        }

        public int getMaxFill() {
            return maxFill;
        }

        public void writeToNBT(CompoundTag nbt, String key) {
            nbt.putInt(key, fill);
            nbt.putInt(key + "_max", maxFill);
            nbt.putString(key + "_type", type.name);
        }

        public void readFromNBT(CompoundTag nbt, String key) {
            fill = nbt.getInt(key);
            int max = nbt.getInt(key + "_max");
            if (max > 0) maxFill = max;
            PseudoFluidType read = PseudoFluidType.TYPES.get(nbt.getString(key + "_type"));
            type = read == null ? GasCentrifugeRecipes.PseudoFluidType.NONE : read;
        }
    }
}
