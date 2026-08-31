package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.container.machine.MachineCombustionEngineMenu;
import com.hbm.items.machine.ItemPistons;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineCombustionEngine} (block {@code MachineCombustionEngine},
 * regname {@code machine_combustion_engine}, read in full): a multiblock diesel-burning HE engine
 * with a real inventory + GUI. Slot 0 must hold an {@link ItemPistons} (this port's four registered
 * {@code piston_set_*} items replace CE's single item + 4 metadata grades - see
 * {@link ItemPistons.EnumPistonType#eff}); {@link #setting} (0-30, redstone/GUI-controlled) is the
 * throttle. {@code fill}/{@code tenth} preserve CE's tenth-of-a-millibucket burn-rate precision
 * exactly (burning fractional mB/tick at low throttle would otherwise round to zero forever).
 * <p>
 * <b>Scope trims vs. CE</b> (see {@link MachineDieselBlockEntity}'s javadoc for the shared
 * rationale): no item-fill slot (fuel arrives purely by pipe); no smoke/pollution tanks
 * ({@code TileEntityMachinePolluting}'s bookkeeping - {@code PollutionHandler} is Phase 4 scope per
 * the research report, and the smoke tanks have no other consumer once the mechanic itself is
 * stubbed, so they are omitted rather than built inert); no OpenComputers/Redstone-over-Radio
 * integration (report recommends dropping both - neither mod has a confirmed NeoForge 1.21 build).
 */
public class MachineCombustionEngineBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 2_500_000L;
    private static final int PISTON_SLOT = 0;
    private static final int BATTERY_SLOT = 1;

    public final FluidTankNTM tank;
    public boolean isOn;
    public int setting;
    private long power;
    private int tenth;

    public MachineCombustionEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, true);
        tank = new FluidTankNTM(Fluids.DIESEL, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.combustionEngine");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == PISTON_SLOT) return stack.getItem() instanceof ItemPistons;
        if (i == BATTERY_SLOT) return Library.isBattery(stack);
        return false;
    }

    private Direction coreDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    public DirPos[] getConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.getStepX() + rot.getStepX(), y, z + dir.getStepZ() + rot.getStepZ(), dir),
                new DirPos(x + dir.getStepX() - rot.getStepX(), y, z + dir.getStepZ() - rot.getStepZ(), dir),
                new DirPos(x - dir.getStepX() * 2 + rot.getStepX(), y, z - dir.getStepZ() * 2 + rot.getStepZ(), dir.getOpposite()),
                new DirPos(x - dir.getStepX() * 2 - rot.getStepX(), y, z - dir.getStepZ() * 2 - rot.getStepZ(), dir.getOpposite())
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        int fill = tank.getFill() * 10 + tenth;
        ItemStack pistonStack = inventory.getStackInSlot(PISTON_SLOT);

        boolean wasOn = false;
        if (isOn && setting > 0 && pistonStack.getItem() instanceof ItemPistons piston
                && fill > 0 && tank.getTankType().hasTrait(FT_Combustible.class)) {
            FT_Combustible trait = tank.getTankType().getTrait(FT_Combustible.class);
            double eff = piston.getType().eff[trait.getGrade().ordinal()];

            if (eff > 0) {
                int speed = setting * 2;
                int toBurn = Math.min(fill, speed);
                power += (long) (toBurn * (trait.getCombustionEnergy() / 10_000D) * eff);
                fill -= toBurn;
                wasOn = toBurn > 0;

                tank.setFill(fill / 10);
                tenth = fill % 10;
            }
        }

        power = Library.chargeItemsFromTE(inventory, BATTERY_SLOT, power, MAX_POWER);

        for (DirPos dirPos : getConPos()) {
            BlockPos p = dirPos.getPos();
            this.tryProvide(level, p.getX(), p.getY(), p.getZ(), dirPos.getDir());
            this.trySubscribe(tank.getTankType(), level, p.getX(), p.getY(), p.getZ(), dirPos.getDir());
        }

        if (power > MAX_POWER) power = MAX_POWER;

        // CE: TileEntityMachineDiesel.getLoopedSound() - continuous AudioWrapper loop
        // (HBMSoundHandler.engine, 10-tick keepAlive) while burning fuel. No looped-block-audio
        // bridge ported yet (see ChemPlantBlockEntity's identical note); substituted with a periodic
        // broadcast every 10 ticks while actively combusting.
        if (wasOn && level.getGameTime() % 10 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.engine.get(), SoundSource.BLOCKS, 1F, 1.0F);
        }

        if (wasOn) dataChanged();
        networkPackMK2(50);
    }

    public void setOn(boolean on) {
        this.isOn = on;
        dataChanged();
        setChanged();
    }

    public void setThrottle(int value) {
        this.setting = Math.max(0, Math.min(30, value));
        dataChanged();
        setChanged();
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("setting", setting);
        tag.putLong("power", power);
        tag.putBoolean("isOn", isOn);
        tag.putInt("tenth", tenth);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setting = tag.getInt("setting");
        power = tag.getLong("power");
        isOn = tag.getBoolean("isOn");
        tenth = tag.getInt("tenth");
        tank.readFromNBT(tag, "tank");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(setting);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        setting = buf.readInt();
        power = buf.readLong();
        isOn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCombustionEngineMenu(containerId, playerInventory, this);
    }
}
