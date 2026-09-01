package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * CE {@code TileEntityMachinePumpBase}/{@code PumpSteam}/{@code PumpElectric}.
 * Config JSON skipped. Rotor audio client-side skipped.
 */
public class MachinePumpBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE {

    public static final int GROUND_HEIGHT = 70;
    public static final int GROUND_DEPTH = 4;
    public static final int STEAM_SPEED = 1_000;
    public static final int ELECTRIC_SPEED = 10_000;
    public static final int NON_WATER_DEBUFF = 100;
    public static final long MAX_POWER = 10_000L;

    private static final Set<String> VALID_HBM = Set.of(
            "waste_earth", "dirt_dead", "dirt_oily", "sand_dirty", "sand_dirty_red");

    public final FluidTankNTM water;
    public final FluidTankNTM steam;
    public final FluidTankNTM lps;
    public final boolean electric;
    public long power;
    public boolean isOn;
    public boolean onGround;
    private int groundCheckDelay;

    public static MachinePumpBlockEntity steam(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new MachinePumpBlockEntity(type, pos, state, false);
    }

    public static MachinePumpBlockEntity electric(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new MachinePumpBlockEntity(type, pos, state, true);
    }

    public MachinePumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean electric) {
        super(type, pos, state, 0, true, electric);
        this.electric = electric;
        int speed = electric ? ELECTRIC_SPEED : STEAM_SPEED;
        this.water = new FluidTankNTM(Fluids.WATER, speed * 100).withOwner(this);
        this.steam = electric ? null : new FluidTankNTM(Fluids.STEAM, 1_000).withOwner(this);
        this.lps = electric ? null : new FluidTankNTM(Fluids.SPENTSTEAM, 10).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(electric ? "block.hbm.pump_electric" : "block.hbm.pump_steam");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (DirPos pos : getConPos()) {
            if (water.getFill() > 0) tryProvide(water, level, pos);
            if (!electric) {
                trySubscribe(steam.getTankType(), level, pos);
                if (lps.getFill() > 0) tryProvide(lps, level, pos);
            } else if (level.getGameTime() % 20 == 0) {
                trySubscribe(level, pos);
            }
        }

        if (groundCheckDelay > 0) {
            groundCheckDelay--;
        } else {
            onGround = checkGround();
        }

        isOn = false;
        if (canOperate() && worldPosition.getY() <= GROUND_HEIGHT && onGround) {
            isOn = true;
            operate();
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean checkGround() {
        if (!level.dimensionType().hasSkyLight()) return false;
        int valid = 0;
        int invalid = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y >= -GROUND_DEPTH; y--) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos p = worldPosition.offset(x, y, z);
                    BlockState st = level.getBlockState(p);
                    Block b = st.getBlock();
                    if (y == -1 && !st.isSolidRender(level, p)) return false;
                    if (isValidGround(b)) valid++;
                    else invalid++;
                }
            }
        }
        return valid >= invalid;
    }

    private static boolean isValidGround(Block block) {
        if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.SAND
                || block == Blocks.MYCELIUM || block == Blocks.COARSE_DIRT || block == Blocks.PODZOL) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return "hbm".equals(id.getNamespace()) && VALID_HBM.contains(id.getPath());
    }

    private boolean canOperate() {
        if (electric) return power >= 1_000 && water.getFill() < water.getMaxFill();
        return steam.getFill() >= 100 && lps.getMaxFill() - lps.getFill() > 0 && water.getFill() < water.getMaxFill();
    }

    private void operate() {
        if (electric) {
            power -= 1_000;
        } else {
            steam.setFill(steam.getFill() - 100);
            lps.setFill(lps.getFill() + 1);
        }
        int base = electric ? ELECTRIC_SPEED : STEAM_SPEED;
        int speed = water.getTankType() == Fluids.WATER ? base : base / NON_WATER_DEBUFF;
        water.setFill(Math.min(water.getFill() + speed, water.getMaxFill()));
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.relative(Direction.EAST, 2), Direction.EAST),
                new DirPos(p.relative(Direction.WEST, 2), Direction.WEST),
                new DirPos(p.relative(Direction.SOUTH, 2), Direction.SOUTH),
                new DirPos(p.relative(Direction.NORTH, 2), Direction.NORTH)
        };
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
        return electric ? List.of() : List.of(steam);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return electric ? List.of(water) : List.of(water, lps);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return electric ? List.of(water) : List.of(water, steam, lps);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putBoolean("onGround", onGround);
        water.writeToNBT(tag, "water");
        if (steam != null) steam.writeToNBT(tag, "steam");
        if (lps != null) lps.writeToNBT(tag, "lps");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        onGround = tag.getBoolean("onGround");
        water.readFromNBT(tag, "water");
        if (steam != null) steam.readFromNBT(tag, "steam");
        if (lps != null) lps.readFromNBT(tag, "lps");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(onGround);
        buf.writeLong(power);
        water.serialize(buf);
        if (steam != null) steam.serialize(buf);
        if (lps != null) lps.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        onGround = buf.readBoolean();
        power = buf.readLong();
        water.deserialize(buf);
        if (steam != null) steam.deserialize(buf);
        if (lps != null) lps.deserialize(buf);
    }
}
