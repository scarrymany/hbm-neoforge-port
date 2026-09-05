package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.dummyable.CompressorMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.inventory.recipes.CompressorRecipes.CompressorRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityMachineCompressorBase}: 4 slots, 2×16k tanks, 100k HE, generic +1 PU fallback.
 * {@code tanks[0].setType(0)} Exact CE {@code :91}. Slots 2-3 upgrades Exact CE {@code :94-112}
 * / {@code ContainerCompressor.java:36-37}. IUpgradeInfoProvider stay skipped.
 */
public class MachineCompressorBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IControlReceiver {

    public static final long MAX_POWER = 100_000;
    public static final int PROCESS_TIME_BASE = 100;
    public static final int POWER_REQ_BASE = 2_500;
    public static final int SLOT_UPGRADE_A = 2;
    public static final int SLOT_UPGRADE_B = 3;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 9);
    }

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public long power;
    public int progress;
    public int processTime = PROCESS_TIME_BASE;
    public int powerRequirement = POWER_REQ_BASE;
    public boolean isOn;
    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);

    public MachineCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, true, true);
        this.input = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
        this.output = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this).withPressure(1);
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
                // CE TileEntityMachineCompressorBase.java:66-67
                if (!stack.isEmpty() && slot >= SLOT_UPGRADE_A && slot <= SLOT_UPGRADE_B
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
        return Component.translatable("container.machineCompressor");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 1) return Library.isBattery(stack);
        if (slot == SLOT_UPGRADE_A || slot == SLOT_UPGRADE_B) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 1;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                trySubscribe(input.getTankType(), level, pos);
            }
        }

        power = Library.chargeTEFromItems(inventory, 1, power, MAX_POWER);
        // CE TileEntityMachineCompressorBase.java:91
        this.input.setType(0, inventory);
        setupTanks();

        CompressorRecipe rec = CompressorRecipes.getRecipe(input.getTankType(), input.getPressure());

        // CE TileEntityMachineCompressorBase.java:94-112
        upgradeManager.checkSlots(inventory, 1, 3);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        int timeBase = rec != null ? rec.duration : PROCESS_TIME_BASE;
        if (rec == null) {
            this.processTime = speedLevel == 3 ? 10 : speedLevel == 2 ? 20 : speedLevel == 1 ? 60 : timeBase;
        } else {
            this.processTime = timeBase / (speedLevel + 1);
        }
        this.powerRequirement = POWER_REQ_BASE / (powerLevel + 1);
        this.processTime = this.processTime / (overLevel + 1);
        this.powerRequirement = this.powerRequirement * ((overLevel * 2) + 1);
        if (processTime <= 0) processTime = 1;

        if (canProcess()) {
            progress++;
            isOn = true;
            power -= powerRequirement;
            if (progress >= processTime) {
                progress = 0;
                process();
                setChanged();
            }
        } else {
            progress = 0;
            isOn = false;
        }

        for (DirPos pos : getConPos()) {
            if (output.getFill() > 0) tryProvide(output, level, pos);
        }

        dataChanged();
        networkPackMK2(100);
    }

    public boolean canProcess() {
        if (power <= powerRequirement) return false;
        CompressorRecipe recipe = CompressorRecipes.getRecipe(input.getTankType(), input.getPressure());
        if (recipe == null) {
            return input.getFill() >= 1_000 && output.getFill() + 1_000 <= output.getMaxFill();
        }
        return input.getFill() >= recipe.inputAmount && output.getFill() + recipe.output.fill <= output.getMaxFill();
    }

    public void process() {
        CompressorRecipe recipe = CompressorRecipes.getRecipe(input.getTankType(), input.getPressure());
        if (recipe == null) {
            input.setFill(input.getFill() - 1_000);
            output.setFill(output.getFill() + 1_000);
        } else {
            input.setFill(input.getFill() - recipe.inputAmount);
            output.setFill(output.getFill() + recipe.output.fill);
        }
    }

    public void setupTanks() {
        CompressorRecipe recipe = CompressorRecipes.getRecipe(input.getTankType(), input.getPressure());
        if (recipe == null) {
            output.withPressure(input.getPressure() + 1).setTankType(input.getTankType());
        } else {
            output.withPressure(recipe.output.pressure).setTankType(recipe.output.type);
        }
    }

    public void setCompression(int compression) {
        compression = Math.max(0, Math.min(5, compression));
        if (compression == input.getPressure()) return;
        input.withPressure(compression);
        setupTanks();
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    /** Exact CE {@code TileEntityMachineCompressorBase.receiveControl} :245-260. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("compression")) return;
        int compression = data.getInt("compression");
        if (compression != input.getPressure()) {
            input.withPressure(compression);
            setupTanks();
            setChanged();
            dataChanged();
        }
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir.getOpposite()), dir.getOpposite()),
                new DirPos(worldPosition.relative(rot), rot),
                new DirPos(worldPosition.relative(rot.getOpposite()), rot.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    public int getProgressScaled(int i) {
        return (progress * i) / Math.max(1, processTime);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(output);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, output);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        input.writeToNBT(tag, "0");
        output.writeToNBT(tag, "1");
        tag.putLong("power", power);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.readFromNBT(tag, "0");
        output.readFromNBT(tag, "1");
        power = tag.getLong("power");
        progress = tag.getInt("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        buf.writeInt(powerRequirement);
        buf.writeLong(power);
        input.serialize(buf);
        output.serialize(buf);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readInt();
        processTime = buf.readInt();
        powerRequirement = buf.readInt();
        power = buf.readLong();
        input.deserialize(buf);
        output.deserialize(buf);
        isOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CompressorMenu(id, inv, this);
    }
}
