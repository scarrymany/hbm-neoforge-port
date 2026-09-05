package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.container.machine.dummyable.HeatBoilerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.saveddata.TomSaveData;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * CE {@code TileEntityHeatBoiler} / {@code TileEntityHeatBoilerIndustrial} —
 * pull heat from {@link IHeatSource} below, {@link FT_Heatable} BOILER convert.
 * Overpressure explode Exact CE {@code TileEntityHeatBoiler.java:273-293} (small only,
 * {@code canExplode}); Tom fire heat {@code :82-85}. Groan Exact CE {@code :265-266}
 * / industrial {@code :231-233} ({@code boilerGroan} 0.5F/1.0F, 1/400).
 * {@link IConfigurableMachine} Exact CE {@code boiler}/{@code boilerIndustrial}
 * ({@code TileEntityHeatBoiler.java:359-375}, {@code TileEntityHeatBoilerIndustrial.java:315-328}).
 * ROR: CE {@code TileEntityHeatBoiler.java:396-412} / industrial {@code :348-360}.
 */
public class HeatBoilerBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider, IRORValueProvider {

    public static int maxHeatCfg = 12_800_000;
    public static double diffusion = 0.1D;
    public static boolean canExplode = true;
    public static int maxHeatIndustrial = 12_800_000;
    public static double diffusionIndustrial = 0.1D;

    public final FluidTankNTM water;
    public final FluidTankNTM steam;
    public final int maxHeat;
    public final double heatDiffusion;
    /** CE small boiler only — industrial has no explode path. */
    public final boolean explodable;
    public int heat;
    public boolean isOn;
    public boolean hasExploded;

    public static HeatBoilerBlockEntity small(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new HeatBoilerBlockEntity(type, pos, state, 16_000, 16_000 * 100, maxHeatCfg, diffusion, true);
    }

    public static HeatBoilerBlockEntity industrial(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new HeatBoilerBlockEntity(type, pos, state, 64_000, 64_000 * 100, maxHeatIndustrial, diffusionIndustrial, false);
    }

    public HeatBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 int waterCap, int steamCap, int maxHeat, double heatDiffusion, boolean explodable) {
        super(type, pos, state, 1, true, false);
        this.water = new FluidTankNTM(Fluids.WATER, waterCap).withOwner(this);
        this.steam = new FluidTankNTM(Fluids.STEAM, steamCap).withOwner(this);
        this.maxHeat = maxHeat;
        this.heatDiffusion = heatDiffusion;
        this.explodable = explodable;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heatBoiler");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (!hasExploded) {
            ItemStack id = inventory.getStackInSlot(0);
            if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
                water.setTankType(ident.getType(level, worldPosition, id));
            }

            tryPullHeat();
            // CE TileEntityHeatBoiler.java:82-85
            if (level instanceof ServerLevel server) {
                int light = level.getBrightness(LightLayer.SKY, worldPosition);
                if (light > 7 && TomSaveData.forWorld(server).fire > 1e-5) {
                    this.heat += (int) ((maxHeat - heat) * 0.000005D);
                }
            }
            isOn = false;
            tryConvert();

            if (level.getGameTime() % 20 == 0) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    trySubscribe(water.getTankType(), level, worldPosition.relative(d, 2), d);
                    if (steam.getFill() > 0) tryProvide(steam, level, worldPosition.relative(d, 2), d);
                }
                trySubscribe(water.getTankType(), level, worldPosition.above(4), Direction.UP);
                if (steam.getFill() > 0) tryProvide(steam, level, worldPosition.above(4), Direction.UP);
            }
        }

        dataChanged();
        networkPackMK2(25);
    }

    private void tryPullHeat() {
        if (heat >= maxHeat) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * heatDiffusion);
                source.useUpHeat(diff);
                heat = Math.min(heat + diff, maxHeat);
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    private void tryConvert() {
        if (!water.getTankType().hasTrait(FT_Heatable.class)) return;
        FT_Heatable trait = water.getTankType().getTrait(FT_Heatable.class);
        if (trait.getEfficiency(FT_Heatable.HeatingType.BOILER) <= 0) return;
        FT_Heatable.HeatingStep entry = trait.getFirstStep();
        int heatReq = (int) Math.max(entry.heatReq / trait.getEfficiency(FT_Heatable.HeatingType.BOILER), 1);
        int inputOps = water.getFill() / entry.amountReq;
        int outputOps = (steam.getMaxFill() - steam.getFill()) / entry.amountProduced;
        int heatOps = heat / heatReq;
        int ops = Math.min(inputOps, Math.min(outputOps, heatOps));
        water.setFill(water.getFill() - entry.amountReq * ops);
        steam.setFill(steam.getFill() + entry.amountProduced * ops);
        steam.setTankType(entry.typeProduced);
        heat -= heatReq * ops;
        // CE TileEntityHeatBoiler.java:265-266 / TileEntityHeatBoilerIndustrial.java:231-233
        if (ops > 0 && level != null && level.random.nextInt(400) == 0) {
            var groans = HBMSoundHandler.boilerGroanSounds();
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5,
                    groans[level.random.nextInt(3)], SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        if (ops > 0) isOn = true;
        // CE TileEntityHeatBoiler.java:273-293 — industrial has no explode
        if (outputOps == 0 && canExplode && explodable) {
            explode();
        }
    }

    private void explode() {
        if (level == null) return;
        this.hasExploded = true;
        BlockDummyable.safeRem = true;
        BlockPos base = worldPosition;
        for (int x = base.getX() - 1; x <= base.getX() + 1; x++) {
            for (int y = base.getY() + 2; y <= base.getY() + 3; y++) {
                for (int z = base.getZ() - 1; z <= base.getZ() + 1; z++) {
                    level.removeBlock(new BlockPos(x, y, z), false);
                }
            }
        }
        level.removeBlock(base.above(), false);
        ExplosionVNT xnt = new ExplosionVNT(level, base.getX() + 0.5D, base.getY() + 2D, base.getZ() + 0.5D, 5F);
        xnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(3F));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.setSFX(new ExplosionEffectStandard());
        xnt.explode();
        BlockDummyable.safeRem = false;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(water);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(water, steam);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        water.writeToNBT(tag, "water");
        steam.writeToNBT(tag, "steam");
        tag.putInt("heat", heat);
        tag.putBoolean("exploded", hasExploded);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(tag, "water");
        steam.readFromNBT(tag, "steam");
        heat = tag.getInt("heat");
        hasExploded = tag.getBoolean("exploded");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        water.serialize(buf);
        steam.serialize(buf);
        buf.writeInt(heat);
        buf.writeBoolean(isOn);
        buf.writeBoolean(hasExploded);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        water.deserialize(buf);
        steam.deserialize(buf);
        heat = buf.readInt();
        isOn = buf.readBoolean();
        hasExploded = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeatBoilerMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE HeatBoiler :396-401 / Industrial :348-353
        return new String[]{
                PREFIX_VALUE + "input",
                PREFIX_VALUE + "output"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE TileEntityHeatBoiler.java:405-412
        if (hasExploded) {
            if ((PREFIX_VALUE + "input").equals(name)) return "0";
            if ((PREFIX_VALUE + "output").equals(name)) return "0";
            return null;
        }
        if ((PREFIX_VALUE + "input").equals(name)) return "" + water.getFill();
        if ((PREFIX_VALUE + "output").equals(name)) return "" + steam.getFill();
        return null;
    }

    static void readBoiler(JsonObject obj) {
        // CE TileEntityHeatBoiler.java:365-367
        maxHeatCfg = IConfigurableMachine.grab(obj, "I:maxHeat", maxHeatCfg);
        diffusion = IConfigurableMachine.grab(obj, "D:diffusion", diffusion);
        canExplode = IConfigurableMachine.grab(obj, "B:canExplode", canExplode);
    }

    static void writeBoiler(JsonWriter writer) throws IOException {
        // CE TileEntityHeatBoiler.java:372-374
        writer.name("I:maxHeat").value(maxHeatCfg);
        writer.name("D:diffusion").value(diffusion);
        writer.name("B:canExplode").value(canExplode);
    }

    static void readIndustrial(JsonObject obj) {
        // CE TileEntityHeatBoilerIndustrial.java:321-322
        maxHeatIndustrial = IConfigurableMachine.grab(obj, "I:maxHeat", maxHeatIndustrial);
        diffusionIndustrial = IConfigurableMachine.grab(obj, "D:diffusion", diffusionIndustrial);
    }

    static void writeIndustrial(JsonWriter writer) throws IOException {
        // CE TileEntityHeatBoilerIndustrial.java:327-328
        writer.name("I:maxHeat").value(maxHeatIndustrial);
        writer.name("D:diffusion").value(diffusionIndustrial);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "boiler";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readBoiler(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeBoiler(writer);
        }
    }

    public static final class ConfigDummyIndustrial implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "boilerIndustrial";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readIndustrial(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeIndustrial(writer);
        }
    }
}
