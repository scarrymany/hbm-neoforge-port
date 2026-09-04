package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.container.machine.dummyable.FireboxMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.util.ItemStackUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityHeaterFirebox} / {@code TileEntityFireboxBase.java}:50-113 —
 * 2 fuel slots.
 * {@code pollute(SOOT, SOOT_PER_SECOND*3)} every 20t while burning Exact CE {@code :99}.
 * Smoke overflow {@code incrementPollution} Exact CE {@code TileEntityMachinePolluting:39-48}.
 * {@link IConfigurableMachine} Exact CE {@code TileEntityHeaterFirebox.java:86-108} ({@code firebox}).
 * Ashpit dump Exact CE {@code :71-79}. Door anim / crackle / particles stay skipped.
 */
public class HeaterFireboxBlockEntity extends MachineBaseBlockEntity
        implements IHeatSource, IFluidStandardSenderMK2, ITickableBE, MenuProvider {

    // CE TileEntityHeaterFirebox.java:24-41
    public static int baseHeat = 100;
    public static double timeMult = 1D;
    public static int maxHeatEnergy = 100_000;

    public static ModuleBurnTime burnModule = new ModuleBurnTime()
            .setLigniteTimeMod(1.25)
            .setCoalTimeMod(1.25)
            .setCokeTimeMod(1.25)
            .setSolidTimeMod(1.5)
            .setRocketTimeMod(1.5)
            .setBalefireTimeMod(0.5)
            .setLigniteHeatMod(2)
            .setCoalHeatMod(2)
            .setCokeHeatMod(2)
            .setSolidHeatMod(3)
            .setRocketHeatMod(5)
            .setBalefireHeatMod(15);

    public int maxBurnTime;
    public int burnTime;
    public int burnHeat;
    public int heatEnergy;
    public boolean wasOn;
    /** CE {@code TileEntityMachinePolluting} buffer 50 from {@code super(2, 50)}. */
    public final FluidTankNTM smoke;
    public final FluidTankNTM smokeLeaded;
    public final FluidTankNTM smokePoison;

    public HeaterFireboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, false);
        this.smoke = new FluidTankNTM(Fluids.SMOKE, 50).withOwner(this);
        this.smokeLeaded = new FluidTankNTM(Fluids.SMOKE_LEADED, 50).withOwner(this);
        this.smokePoison = new FluidTankNTM(Fluids.SMOKE_POISON, 50).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterFirebox");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 2 && getModule().getBurnTime(stack) > 0;
    }

    public ModuleBurnTime getModule() {
        return burnModule;
    }

    /**
     * Exact CE {@code TileEntityFireboxBase.java}:138-152.
     * Tag path / last-segment so {@code contains("Coal")}/{@code startsWith("log")} still hit 1.21 tags.
     */
    public static EnumAshType getAshFromFuel(ItemStack stack) {
        for (String name : ashOreNames(stack)) {
            if (name.contains("Coke")) return EnumAshType.COAL;
            if (name.contains("Coal")) return EnumAshType.COAL;
            if (name.contains("Lignite")) return EnumAshType.COAL;
            if (name.startsWith("log")) return EnumAshType.WOOD;
            if (name.contains("Wood")) return EnumAshType.WOOD;
            if (name.contains("Sapling")) return EnumAshType.WOOD;
        }
        return EnumAshType.MISC;
    }

    /** Same expansion as {@link ModuleBurnTime} oreNames — CE ore-dict fragments on 1.21 tags. */
    private static List<String> ashOreNames(ItemStack stack) {
        List<String> raw = ItemStackUtil.getOreDictNames(stack);
        List<String> out = new ArrayList<>(raw);
        for (String name : raw) {
            int colon = name.indexOf(':');
            String path = colon >= 0 ? name.substring(colon + 1) : name;
            out.add(path);
            int slash = path.lastIndexOf('/');
            String last = slash >= 0 ? path.substring(slash + 1) : path;
            if (!last.isEmpty()) {
                out.add(Character.toUpperCase(last.charAt(0)) + last.substring(1));
            }
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null) {
            String p = key.getPath();
            out.add(p);
            if (!p.isEmpty()) {
                out.add(Character.toUpperCase(p.charAt(0)) + p.substring(1));
            }
        }
        return out;
    }

    public int getBaseHeat() {
        return baseHeat;
    }

    public double getTimeMult() {
        return timeMult;
    }

    public int getMaxHeat() {
        return maxHeatEnergy;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        tickBurn();
        dataChanged();
        networkPackMK2(50);
    }

    protected void tickBurn() {
        // CE TileEntityFireboxBase.java:51-58 sendSmoke, :61-113 burn
        sendSmoke();
        wasOn = false;
        if (burnTime <= 0) {
            for (int i = 0; i < 2; i++) {
                ItemStack fuel = inventory.getStackInSlot(i);
                int base = getModule().getBurnTime(fuel);
                if (base > 0) {
                    // Exact CE TileEntityFireboxBase.java:71-79 — classify then dump to ashpit below
                    BlockEntity below = level.getBlockEntity(worldPosition.below());
                    if (below instanceof MachineAshpitBlockEntity ashpit) {
                        EnumAshType type = getAshFromFuel(fuel);
                        if (type == EnumAshType.WOOD) ashpit.ashLevelWood += base;
                        if (type == EnumAshType.COAL) ashpit.ashLevelCoal += base;
                        if (type == EnumAshType.MISC) ashpit.ashLevelMisc += base;
                    }
                    maxBurnTime = burnTime = Math.max(1, (int) (base * getTimeMult()));
                    burnHeat = getModule().getBurnHeat(getBaseHeat(), fuel);
                    inventory.extractItem(i, 1, false);
                    wasOn = true;
                    setChanged();
                    break;
                }
            }
        } else {
            if (heatEnergy < getMaxHeat()) {
                burnTime--;
                // CE TileEntityFireboxBase.java:99
                if (level.getGameTime() % 20 == 0) {
                    pollute(PollutionHandler.PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
                }
            }
            wasOn = true;
        }
        if (wasOn) {
            heatEnergy = Math.min(heatEnergy + burnHeat, getMaxHeat());
        } else {
            heatEnergy = Math.max(heatEnergy - Math.max(heatEnergy / 1000, 1), 0);
            burnHeat = 0;
        }
    }

    /** CE {@code TileEntityFireboxBase.java:51-58}. */
    private void sendSmoke() {
        if (level == null) return;
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            Direction rot = dir.getClockWise();
            for (int j = -1; j <= 1; j++) {
                BlockPos dest = worldPosition.offset(
                        dir.getStepX() * 2 + rot.getStepX() * j, 0,
                        dir.getStepZ() * 2 + rot.getStepZ() * j);
                if (smoke.getFill() > 0) tryProvide(smoke, level, dest, dir);
                if (smokeLeaded.getFill() > 0) tryProvide(smokeLeaded, level, dest, dir);
                if (smokePoison.getFill() > 0) tryProvide(smokePoison, level, dest, dir);
            }
        }
    }

    /** Exact CE {@code TileEntityMachinePolluting#pollute(PollutionType, float)} {@code :39-48}. */
    public void pollute(PollutionHandler.PollutionType type, float amount) {
        FluidTankNTM tank = type == PollutionHandler.PollutionType.SOOT ? smoke
                : type == PollutionHandler.PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;
        int fluidAmount = (int) Math.ceil(amount * 100);
        tank.setFill(tank.getFill() + fluidAmount);
        if (tank.getFill() > tank.getMaxFill()) {
            int overflow = tank.getFill() - tank.getMaxFill();
            tank.setFill(tank.getMaxFill());
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);
        }
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        // CE TileEntityFireboxBase.java:253-254 getSmokeTanks
        return List.of(smoke, smokeLeaded, smokePoison);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        // CE TileEntityFireboxBase.java:248-249
        return List.of();
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("burn", burnTime);
        tag.putInt("maxBurn", maxBurnTime);
        tag.putInt("burnHeat", burnHeat);
        tag.putInt("heat", heatEnergy);
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("burn");
        maxBurnTime = tag.getInt("maxBurn");
        burnHeat = tag.getInt("burnHeat");
        heatEnergy = tag.getInt("heat");
        smoke.readFromNBT(tag, "smoke0");
        smokeLeaded.readFromNBT(tag, "smoke1");
        smokePoison.readFromNBT(tag, "smoke2");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(maxBurnTime);
        buf.writeInt(burnTime);
        buf.writeInt(burnHeat);
        buf.writeInt(heatEnergy);
        buf.writeBoolean(wasOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        maxBurnTime = buf.readInt();
        burnTime = buf.readInt();
        burnHeat = buf.readInt();
        heatEnergy = buf.readInt();
        wasOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FireboxMenu(id, inv, this);
    }

    static void readFirebox(JsonObject obj) {
        // CE TileEntityHeaterFirebox.java:92-97 — write key is M:burnModule (rotaryfurnace :480)
        baseHeat = IConfigurableMachine.grab(obj, "I:baseHeat", baseHeat);
        timeMult = IConfigurableMachine.grab(obj, "D:burnTimeMult", timeMult);
        maxHeatEnergy = IConfigurableMachine.grab(obj, "I:heatCap", maxHeatEnergy);
        if (obj.has("M:burnModule")) {
            burnModule.readIfPresent(obj.get("M:burnModule").getAsJsonObject());
        }
    }

    static void writeFirebox(JsonWriter writer) throws IOException {
        // CE TileEntityHeaterFirebox.java:102-107
        writer.name("I:baseHeat").value(baseHeat);
        writer.name("D:burnTimeMult").value(timeMult);
        writer.name("I:heatCap").value(maxHeatEnergy);
        writer.name("M:burnModule").beginObject();
        burnModule.writeConfig(writer);
        writer.endObject();
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "firebox";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readFirebox(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeFirebox(writer);
        }
    }
}
