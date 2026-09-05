package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.dummyable.OilburnerMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityHeaterOilburner.java}:59-103 — FT_Flammable tank, setting mB/t,
 * maxHeat 100_000. {@code loadTank(0,1)}/{@code setType(2)} Exact CE {@code :63-64}.
 * {@code pollute(BURN, toBurn*5)} every 5t Exact CE {@code :87-88}.
 * Smoke overflow {@code incrementPollution} Exact CE {@code TileEntityMachinePolluting:53-76}.
 * ROR: CE {@code :247-279}. Audio stay skipped.
 */
public class HeaterOilburnerBlockEntity extends MachineBaseBlockEntity
        implements IHeatSource, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IControlReceiver, IRORValueProvider, IRORInteractive {

    public static final int MAX_HEAT = 100_000;

    public final FluidTankNTM tank;
    /** CE {@code TileEntityMachinePolluting} buffer 1000 from {@code super(3, 1000)}. */
    public final FluidTankNTM smoke;
    public final FluidTankNTM smokeLeaded;
    public final FluidTankNTM smokePoison;
    public boolean isOn;
    public int setting = 1;
    public int heatEnergy;

    public HeaterOilburnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, true, false);
        this.tank = new FluidTankNTM(Fluids.HEATINGOIL, 16_000).withOwner(this);
        this.smoke = new FluidTankNTM(Fluids.SMOKE, 1000).withOwner(this);
        this.smokeLeaded = new FluidTankNTM(Fluids.SMOKE_LEADED, 1000).withOwner(this);
        this.smokePoison = new FluidTankNTM(Fluids.SMOKE_POISON, 1000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterOilburner");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 2) return stack.getItem() instanceof IItemFluidIdentifier;
        return slot == 0;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 1;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityHeaterOilburner.java:63-64
        tank.loadTank(0, 1, inventory);
        tank.setType(2, inventory);

        for (DirPos pos : getConPos()) {
            if (level.getGameTime() % 20 == 0) trySubscribe(tank.getTankType(), level, pos);
            // CE TileEntityHeaterOilburner.java:68
            sendSmoke(pos);
        }

        boolean shouldCool = true;
        if (isOn && heatEnergy < MAX_HEAT) {
            FT_Flammable trait = tank.getTankType().getTrait(FT_Flammable.class);
            if (trait != null) {
                int toBurn = Math.min(setting, tank.getFill());
                tank.setFill(tank.getFill() - toBurn);
                heatEnergy += (int) (trait.getHeatEnergy() / 1000L) * toBurn;
                // CE TileEntityHeaterOilburner.java:87-88
                if (level.getGameTime() % 5 == 0 && toBurn > 0) {
                    pollute(tank.getTankType(), FluidTrait.FluidReleaseType.BURN, toBurn * 5);
                }
                shouldCool = false;
            }
        }
        if (heatEnergy >= MAX_HEAT) shouldCool = false;
        if (shouldCool) heatEnergy = Math.max(heatEnergy - Math.max(heatEnergy / 1000, 1), 0);

        dataChanged();
        networkPackMK2(25);
    }

    /** CE {@code TileEntityMachinePolluting#sendSmoke}. */
    private void sendSmoke(DirPos pos) {
        if (smoke.getFill() > 0) tryProvide(smoke, level, pos);
        if (smokeLeaded.getFill() > 0) tryProvide(smokeLeaded, level, pos);
        if (smokePoison.getFill() > 0) tryProvide(smokePoison, level, pos);
    }

    /**
     * Exact CE {@code TileEntityMachinePolluting#pollute(FluidType, FluidReleaseType, float)}
     * {@code :53-76}. Fire-extinguish sound stay skipped.
     */
    public void pollute(FluidType type, FluidTrait.FluidReleaseType release, float amount) {
        FT_Polluting trait = type.getTrait(FT_Polluting.class);
        if (trait == null) return;
        if (release == FluidTrait.FluidReleaseType.VOID) return;

        HashMap<PollutionHandler.PollutionType, Float> map = release == FluidTrait.FluidReleaseType.BURN
                ? trait.burnMap : trait.releaseMap;

        for (Map.Entry<PollutionHandler.PollutionType, Float> entry : map.entrySet()) {
            FluidTankNTM dest = entry.getKey() == PollutionHandler.PollutionType.SOOT ? smoke
                    : entry.getKey() == PollutionHandler.PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;
            int fluidAmount = (int) Math.ceil(entry.getValue() * amount * 100);
            dest.setFill(dest.getFill() + fluidAmount);
            if (dest.getFill() > dest.getMaxFill()) {
                int overflow = dest.getFill() - dest.getMaxFill();
                dest.setFill(dest.getMaxFill());
                PollutionHandler.incrementPollution(level, worldPosition, entry.getKey(), overflow / 100F);
            }
        }
    }

    public void toggleOn() {
        isOn = !isOn;
        setChanged();
    }

    public void bumpSetting(int delta) {
        setting = Math.max(1, Math.min(100, setting + delta));
        setChanged();
    }

    /** Exact CE {@code TileEntityHeaterOilburner.toggleSettingUp} :149-155. Wrap 100→1. */
    public void toggleSettingUp() {
        setting++;
        if (setting > 100) {
            setting = 1;
        }
    }

    /** Exact CE {@code TileEntityHeaterOilburner.toggleSettingDown} :157-163. Wrap 1→100. */
    public void toggleSettingDown() {
        setting--;
        if (setting < 1) {
            setting = 100;
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        // Exact CE TileEntityHeaterOilburner.java:192-194
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 256.0D;
    }

    /** Exact CE {@code TileEntityHeaterOilburner.receiveControl} :197-202. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            this.isOn = !this.isOn;
        }
        setChanged();
        dataChanged();
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
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void useUpHeat(int heat) {
        heatEnergy = Math.max(heatEnergy - Math.max(0, heat), 0);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        // CE TileEntityHeaterOilburner.java:226-227
        return List.of(smoke, smokeLeaded, smokePoison);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        // CE TileEntityHeaterOilburner.java:221-222 — fuel tank only
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("on", isOn);
        tag.putInt("heat", heatEnergy);
        tag.putInt("set", setting);
        tank.writeToNBT(tag, "t");
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isOn = tag.getBoolean("on");
        heatEnergy = tag.getInt("heat");
        setting = Math.max(1, tag.getInt("set"));
        tank.readFromNBT(tag, "t");
        smoke.readFromNBT(tag, "smoke0");
        smokeLeaded.readFromNBT(tag, "smoke1");
        smokePoison.readFromNBT(tag, "smoke2");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeInt(heatEnergy);
        buf.writeInt(setting);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        heatEnergy = buf.readInt();
        setting = buf.readInt();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new OilburnerMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :247-255
        return new String[]{
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "burnrate",
                PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "active",
                PREFIX_FUNCTION + "setburnrate" + NAME_SEPARATOR + "rate"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :259-264
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heatEnergy;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "burnrate").equals(name)) return "" + setting;
        if ((PREFIX_VALUE + "state").equals(name)) return isOn ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :268-279
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            this.isOn = params[0].equals("1");
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setburnrate").equals(name) && params.length > 0) {
            this.setting = IRORInteractive.parseInt(params[0], 1, 10);
            setChanged();
            return null;
        }
        return null;
    }
}
