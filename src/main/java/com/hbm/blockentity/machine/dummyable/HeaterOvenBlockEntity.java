package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.container.machine.dummyable.HeaterOvenMenu;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;

/**
 * CE {@code TileEntityHeaterOven.java}:26-75 — firebox with baseHeat 500, timeMult 0.125,
 * maxHeat 500_000, plus heat pull from the {@code IHeatSource} below.
 * {@link IConfigurableMachine} Exact CE {@code TileEntityHeaterOven.java:115-138} ({@code heatingoven}).
 */
public class HeaterOvenBlockEntity extends HeaterFireboxBlockEntity {

    // CE TileEntityHeaterOven.java:25-46
    public static int baseHeat = 500;
    public static double timeMult = 0.125D;
    public static int maxHeatEnergy = 500_000;
    public static double heatEff = 0.5D;

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

    public HeaterOvenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterOven");
    }

    @Override
    public ModuleBurnTime getModule() {
        return burnModule;
    }

    @Override
    public int getBaseHeat() {
        return baseHeat;
    }

    @Override
    public double getTimeMult() {
        return timeMult;
    }

    @Override
    public int getMaxHeat() {
        return maxHeatEnergy;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        tryPullHeat();
        tickBurn();
        dataChanged();
        networkPackMK2(50);
    }

    private void tryPullHeat() {
        // CE TileEntityHeaterOven.java:66-75
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source && below != this) {
            int room = getMaxHeat() - heatEnergy;
            int toPull = Math.max(Math.min(source.getHeatStored(), room), 0);
            if (toPull > 0) {
                heatEnergy += (int) (toPull * heatEff);
                source.useUpHeat(toPull);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeaterOvenMenu(id, inv, this);
    }

    static void readOven(JsonObject obj) {
        // CE TileEntityHeaterOven.java:121-127 — has() uses write key M:burnModule
        baseHeat = IConfigurableMachine.grab(obj, "I:baseHeat", baseHeat);
        timeMult = IConfigurableMachine.grab(obj, "D:burnTimeMult", timeMult);
        heatEff = IConfigurableMachine.grab(obj, "D:heatPullEff", heatEff);
        maxHeatEnergy = IConfigurableMachine.grab(obj, "I:heatCap", maxHeatEnergy);
        if (obj.has("M:burnModule")) {
            burnModule.readIfPresent(obj.get("M:burnModule").getAsJsonObject());
        }
    }

    static void writeOven(JsonWriter writer) throws IOException {
        // CE TileEntityHeaterOven.java:132-137
        writer.name("I:baseHeat").value(baseHeat);
        writer.name("D:burnTimeMult").value(timeMult);
        writer.name("D:heatPullEff").value(heatEff);
        writer.name("I:heatCap").value(maxHeatEnergy);
        writer.name("M:burnModule").beginObject();
        burnModule.writeConfig(writer);
        writer.endObject();
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "heatingoven";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readOven(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeOven(writer);
        }
    }
}
