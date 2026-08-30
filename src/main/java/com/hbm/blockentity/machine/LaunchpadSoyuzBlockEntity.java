package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.LaunchpadSoyuzMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ISatChip;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz} (435 lines, read in
 * full) - the Soyuz complex's 8-axis crane/strut/carriage animation state machine
 * ({@link #updateStates()}'s 5-state {@link SoyuzStatus}). Per
 * {@code docs/phase3/missile_launch_infra.md}'s explicit instruction, {@link SoyuzStatus#LAUNCHING}
 * is preserved exactly as CE itself left it - <b>CE's own code only retracts the carriage/rotor
 * there; it has never wired an actual missile-spawn call for this complex</b> (CE's own comment:
 * {@code // TBI: countdown, retracting the struts, launch}). Do not "finish" what CE itself never
 * finished.
 * <p>
 * <b>Blocked, documented</b>: {@code ModItems.missile_soyuz}/{@code missile_soyuz_lander} are not
 * registered anywhere in this port yet (confirmed absent from {@code MissileItems}/{@code ModItems}
 * by grep) - {@code missile_soyuz} is resolved lazily by registry name via
 * {@link #soyuzMissileItem()} rather than a compile-time reference, so this class compiles and runs
 * correctly today ({@link #hasRocketLoaded()} simply never returns {@code true} until that item
 * exists) and starts working with zero further changes once a future missile-item pass adds it.
 * {@code missile_soyuz_lander} (slot 3, the lander module) is left permanently rejecting items
 * ({@link #isItemValidForSlot}) for the same reason - CE's own {@code cargoMode} gate around it is
 * preserved in shape but has nothing valid to accept yet. {@code loadedType} (CE: the loaded
 * stack's metadata, selecting which Soyuz model variant to render) is left at a placeholder
 * {@code 0} for the same reason - no real per-stack variant data exists to read yet.
 */
public class LaunchpadSoyuzBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IEnergyReceiverMK2, IFluidStandardReceiverMK2, MenuProvider {

    public long power;
    public static final long MAX_POWER = 1_000_000;
    public static final long CONSUMPTION = 10_000;
    public final FluidTankNTM[] tanks;

    public static final int INDEX_STRUT1 = 0; // struts 1-5 on the support tower
    public static final int INDEX_STRUT2 = 1;
    public static final int INDEX_STRUT3 = 2;
    public static final int INDEX_STRUT4 = 3;
    public static final int INDEX_STRUT5 = 4;
    public static final int INDEX_CARRIAGE = 5; // delivery carriage
    public static final int INDEX_ROTOR = 6;    // carriage deploy progress
    public static final int INDEX_TILT = 7;     // carriage tilt after ramming the buffer stops

    public final float[] positions = new float[8];
    public final float[] prevPositions = new float[8];
    public final float[] speed = new float[8];
    public final float[] target = new float[8];
    public final float[] syncPositions = new float[8];

    protected int turnProgress;

    public SoyuzStatus soyuzStatus = SoyuzStatus.ABSENT;
    public ComponentStatus strutStatus = ComponentStatus.RETRACT;
    public ComponentStatus carriageStatus = ComponentStatus.RETRACT;
    public ComponentStatus rotorStatus = ComponentStatus.RETRACT;

    public boolean cargoMode = false;
    public int loadedType = -1;
    public int fuelCountdown = 0;
    public static final int FUEL_DURATION = 15 * 20;

    public LaunchpadSoyuzBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 27, true, true);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.KEROSENE_REFORM, 128_000).withOwner(this),
                new FluidTankNTM(Fluids.OXYGEN, 128_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchpadSoyuz");
    }

    public float getInterpPos(int index, float interp) {
        return prevPositions[index] + (positions[index] - prevPositions[index]) * interp;
    }

    private static Item soyuzMissileItem() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "missile_soyuz"));
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.power = Library.chargeTEFromItems(inventory, 8, power, MAX_POWER);

        if (!hasRocketLoaded()) {
            boolean isMoving = false;
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.finishedMoving(i)) {
                    isMoving = true;
                    break;
                }
            }

            // if all parts are currently idle, set the status back to absent and prepare new loading procedure
            if (!isMoving) this.soyuzStatus = SoyuzStatus.ABSENT;

            this.loadedType = -1;
        } else {
            this.loadedType = 0;
        }

        if (this.power >= CONSUMPTION) {
            this.updateStates();
            this.move();
            this.power -= CONSUMPTION;
        }

        networkPackNT(300);
    }

    public void updateStates() {
        if (this.soyuzStatus == SoyuzStatus.ABSENT) {
            /* RETURN BEHAVIOR */

            // retract all struts
            if (this.strutStatus == ComponentStatus.DEPLOY) {
                this.strutStatus = ComponentStatus.RETRACT;
                for (int i = 0; i <= INDEX_STRUT5; i++) {
                    setTarget(i, false, 60 + (level != null ? level.getRandom().nextInt(21) : 0));
                }
            }

            // first send away the carriage
            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                setTarget(INDEX_CARRIAGE, false, 100);
            }

            // once carriage has stopped, tilt, then retract rotor
            if (this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);

                if (this.target[INDEX_TILT] == 0 && this.rotorStatus == ComponentStatus.DEPLOY) {
                    this.rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100);
                }
            }

            // return tilt
            if (this.target[INDEX_TILT] > 0 && this.finishedMoving(INDEX_TILT)) {
                setTarget(INDEX_TILT, false, 3);
            }

            /* DEPLOY BEHAVIOR */

            if (this.hasRocketLoaded()
                    && this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_ROTOR)) {

                this.carriageStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_CARRIAGE, true, 200);

                this.soyuzStatus = SoyuzStatus.LOADING;
                return; // always return on status change
            }
        }

        if (this.soyuzStatus == SoyuzStatus.LOADING) {
            if (this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                this.rotorStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_ROTOR, true, 200);
            }

            if (this.carriageStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_ROTOR)) {

                if (this.strutStatus == ComponentStatus.RETRACT) {
                    this.strutStatus = ComponentStatus.DEPLOY;
                    for (int i = 0; i <= INDEX_STRUT5; i++) {
                        setTarget(i, true, 60 + (level != null ? level.getRandom().nextInt(21) : 0));
                    }
                } else {
                    boolean strutsDeployed = true;
                    for (int i = 0; i <= INDEX_STRUT5; i++) {
                        if (!this.finishedMoving(i)) strutsDeployed = false;
                    }

                    if (strutsDeployed) {
                        this.fuelCountdown = FUEL_DURATION;
                        this.soyuzStatus = SoyuzStatus.FUELING;
                        return;
                    }
                }
            }
        }

        if (this.soyuzStatus == SoyuzStatus.FUELING) {
            if (this.hasFuel()) {
                if (this.fuelCountdown > 0) {
                    this.fuelCountdown--;
                } else {
                    this.soyuzStatus = SoyuzStatus.IDLE;
                    return;
                }
            }
        }

        if (this.soyuzStatus == SoyuzStatus.IDLE) {
            if (!this.hasFuel()) {
                this.fuelCountdown = FUEL_DURATION;
                this.soyuzStatus = SoyuzStatus.FUELING;
                return;
            }
        }

        if (this.soyuzStatus == SoyuzStatus.LAUNCHING) {
            // return carriage
            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                this.setTarget(INDEX_CARRIAGE, false, 100);
            } else if (this.rotorStatus == ComponentStatus.DEPLOY) {
                this.rotorStatus = ComponentStatus.RETRACT;
                this.setTarget(INDEX_ROTOR, false, 100);
            }

            // TBI (CE's own unfinished state, preserved exactly - see class javadoc): countdown,
            // retracting the struts, launch.
        }
    }

    public boolean hasRocketLoaded() {
        ItemStack stack = inventory.getStackInSlot(0);
        return !stack.isEmpty() && stack.getItem() == soyuzMissileItem();
    }

    public boolean finishedMoving(int index) {
        return this.positions[index] == this.target[index];
    }

    public boolean wasMoving(int index) {
        return this.positions[index] != this.prevPositions[index];
    }

    public boolean hasFuel() {
        return this.tanks[0].getFill() >= 100_000 && this.tanks[1].getFill() >= 100_000;
    }

    public void setTarget(int index, boolean deploy, int duration) {
        this.target[index] = deploy ? 1F : 0F;
        this.speed[index] = 1F / duration;
    }

    public void move() {
        for (int i = 0; i < this.positions.length; i++) {
            this.prevPositions[i] = this.positions[i];

            if (Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
                this.positions[i] = this.target[i];
            } else if (this.positions[i] < this.target[i]) {
                this.positions[i] += this.speed[i];
            } else {
                this.positions[i] -= this.speed[i];
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() == soyuzMissileItem();
        if (slot == 1) return stack.getItem() instanceof IDesignatorItem;
        if (slot == 2) return stack.getItem() instanceof ISatChip && !cargoMode;
        if (slot == 3) return false; // lander module item not yet registered, see class javadoc
        if (slot > 8) {
            if (!cargoMode) return false;
            for (int i = 0; i <= 3; i++) if (isItemValidForSlot(i, stack)) return false;
            return true;
        }
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{
                0, 2, 3,
                9, 10, 11,
                12, 13, 14,
                15, 16, 17,
                18, 19, 20,
                21, 22, 23,
                24, 25, 26
        };
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeLong(power);
        buf.writeInt(loadedType);

        for (float pos : this.positions) buf.writeFloat(pos);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        this.power = buf.readLong();
        this.loadedType = buf.readInt();

        for (int i = 0; i < this.positions.length; i++) {
            float newSync = buf.readFloat();
            if (this.syncPositions[i] != newSync) {
                this.syncPositions[i] = newSync;
                this.turnProgress = 2;
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = tag.getLong("power");
        this.cargoMode = tag.getBoolean("cargoMode");
        this.fuelCountdown = tag.getInt("fuelCountdown");
        this.tanks[0].readFromNBT(tag, "t0");
        this.tanks[1].readFromNBT(tag, "t1");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putBoolean("cargoMode", cargoMode);
        tag.putInt("fuelCountdown", fuelCountdown);
        this.tanks[0].writeToNBT(tag, "t0");
        this.tanks[1].writeToNBT(tag, "t1");
    }

    @Override
    public long getPower() {
        return this.power;
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
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LaunchpadSoyuzMenu(containerId, playerInventory, this);
    }

    public enum SoyuzStatus {
        ABSENT,   // no rocket is present, return all components to null position
        LOADING,  // rocket is moved to launch pad
        FUELING,  // rocket is on the launch pad, cooldown is active
        IDLE,     // rocket is ready to launch
        LAUNCHING // countdown is active
    }

    public enum ComponentStatus {
        DEPLOY, RETRACT
    }
}
