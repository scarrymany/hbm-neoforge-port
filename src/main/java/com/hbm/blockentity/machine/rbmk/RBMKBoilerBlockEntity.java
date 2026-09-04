package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Boiler column. Exact CE {@code TileEntityRBMKBoiler.java:84-241}: steam-type heat cap / expansion
 * factor, {@code getBoilerHeatConsumption}, ULTRAHOTSTEAM path, {@code cyceCompressor} on
 * {@code compression}. Vent VFX / {@code rbmk_loader} getConPos stay skipped.
 */
public class RBMKBoilerBlockEntity extends RBMKSlottedBlockEntity
        implements IFluidStandardTransceiverMK2, IControlReceiver, IRORValueProvider {

    public final FluidTankNTM feed;
    public final FluidTankNTM steam;
    /** CE {@code TileEntityRBMKBoiler.consumption} — water used this tick. */
    protected int consumption;
    /** CE {@code TileEntityRBMKBoiler.output} — steam produced this tick. */
    protected int output;

    public RBMKBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        feed = new FluidTankNTM(Fluids.WATER, 10_000).withOwner(this);
        steam = new FluidTankNTM(Fluids.STEAM, 1_000_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkBoiler");
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            this.consumption = 0;
            this.output = 0;

            double heatCap = getHeatFromSteam(steam.getTankType());
            double heatProvided = this.heat - heatCap;

            if (heatProvided > 0) {
                double heatPerMb = RBMKDials.getBoilerHeatConsumption(serverLevel);
                double steamFactor = getFactorFromSteam(steam.getTankType());
                int waterUsed;
                int steamProduced;

                if (steam.getTankType() == Fluids.ULTRAHOTSTEAM) {
                    steamProduced = (int) Math.floor((heatProvided / heatPerMb) * 100D / steamFactor);
                    waterUsed = (int) Math.floor(steamProduced / 100D * steamFactor);

                    if (feed.getFill() < waterUsed) {
                        steamProduced = (int) Math.floor(feed.getFill() * 100D / steamFactor);
                        waterUsed = (int) Math.floor(steamProduced / 100D * steamFactor);
                    }
                } else {
                    waterUsed = (int) Math.floor(heatProvided / heatPerMb);
                    waterUsed = Math.min(waterUsed, feed.getFill());
                    steamProduced = (int) Math.floor((waterUsed * 100D) / steamFactor);
                }

                this.consumption = waterUsed;
                this.output = steamProduced;

                feed.setFill(feed.getFill() - waterUsed);
                steam.setFill(steam.getFill() + steamProduced);

                if (steam.getFill() > steam.getMaxFill()) {
                    steam.setFill(steam.getMaxFill());
                    // CE :123-128 RBMKSteam AuxParticle + steamEngineOperate — VFX skip.
                }

                this.heat -= waterUsed * heatPerMb;
            }

            trySubscribe(feed.getTankType(), level, worldPosition.below(), Direction.DOWN);
            if (this.steam.getFill() > 0) {
                for (DirPos pos : getConPos()) {
                    tryProvide(steam, level, pos);
                }
            }
        }

        super.updateEntity();
    }

    /** CE {@code :144-149}. */
    public static double getHeatFromSteam(FluidType type) {
        if (type == Fluids.STEAM) return 100D;
        if (type == Fluids.HOTSTEAM) return 300D;
        if (type == Fluids.SUPERHOTSTEAM) return 450D;
        if (type == Fluids.ULTRAHOTSTEAM) return 600D;
        return 0D;
    }

    /** CE {@code :152-157}. */
    public static double getFactorFromSteam(FluidType type) {
        if (type == Fluids.STEAM) return 1D;
        if (type == Fluids.HOTSTEAM) return 10D;
        if (type == Fluids.SUPERHOTSTEAM) return 100D;
        if (type == Fluids.ULTRAHOTSTEAM) return 1000D;
        return 0D;
    }

    /** CE {@code :160-184} default (no {@code rbmk_loader}). */
    public DirPos[] getConPos() {
        int height = level instanceof ServerLevel serverLevel ? RBMKDials.getColumnHeight(serverLevel) : 0;
        return new DirPos[]{
                new DirPos(worldPosition.getX(), worldPosition.getY() + height + 1, worldPosition.getZ(), Direction.UP)
        };
    }

    @Override
    public boolean hasPermission(Player player) {
        // CE :218 — length from integer pos < 20
        double dx = worldPosition.getX() - player.getX();
        double dy = worldPosition.getY() - player.getY();
        double dz = worldPosition.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) < 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("compression")) {
            this.cyceCompressor();
            setChanged();
        }
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        // NBTControlPacket calls both overloads — cycle only in receiveControl(nbt).
    }

    /** CE {@code :230-241}. Sequential ifs on captured type — only one branch fires. */
    public void cyceCompressor() {
        if (this.heat > 50 && this.feed.getFill() > 0) return;

        FluidType type = steam.getTankType();
        if (type == Fluids.STEAM) { steam.setTankType(Fluids.HOTSTEAM); steam.setFill(steam.getFill() / 10); }
        if (type == Fluids.HOTSTEAM) { steam.setTankType(Fluids.SUPERHOTSTEAM); steam.setFill(steam.getFill() / 10); }
        if (type == Fluids.SUPERHOTSTEAM) { steam.setTankType(Fluids.ULTRAHOTSTEAM); steam.setFill(steam.getFill() / 10); }
        if (type == Fluids.ULTRAHOTSTEAM) { steam.setTankType(Fluids.STEAM); steam.setFill(Math.min(steam.getFill() * 1000, steam.getMaxFill())); }

        setChanged();
    }

    @Override
    public void onMelt(int reduce) {
        for (int i = 0; i < 2; i++) spawnDebris("BLANK");
        standardMelt(reduce);
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.BOILER;
    }

    @Override
    public RBMKColumn getConsoleData() {
        RBMKColumn.BoilerColumn data = (RBMKColumn.BoilerColumn) super.getConsoleData();
        data.water = feed.getFill();
        data.maxWater = feed.getMaxFill();
        data.steam = steam.getFill();
        data.maxSteam = steam.getMaxFill();
        data.steamType = (short) steam.getTankType().getID();
        return data;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(feed, steam);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(feed);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        feed.writeToNBT(tag, "feed");
        steam.writeToNBT(tag, "steam");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        feed.readFromNBT(tag, "feed");
        steam.readFromNBT(tag, "steam");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        steam.serialize(buf);
        feed.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.steam.deserialize(buf);
        this.feed.deserialize(buf);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :402-407
        return new String[]{
                PREFIX_VALUE + "feed",
                PREFIX_VALUE + "steam",
                PREFIX_VALUE + "consumption"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :411-415
        if ((PREFIX_VALUE + "feed").equals(name)) return "" + this.feed.getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + this.steam.getFill();
        if ((PREFIX_VALUE + "consumption").equals(name)) return "" + this.consumption;
        return null;
    }
}
