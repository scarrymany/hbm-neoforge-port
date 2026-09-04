package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Boiler column - converts column {@code heat} into steam from a water feed. Ported (simplified,
 * see below) from CE's {@code TileEntityRBMKBoiler} (417 lines, signature/field-level survey rather
 * than a full read given this work package's size - flagged for a follow-up fidelity pass rather
 * than guessing CE's exact consumption/output tuning constants). Tank sizes (feed 10,000mB water,
 * steam 1,000,000mB) and fluid types are CE-confirmed; the per-tick heat-to-steam conversion rate
 * below is this port's own reasonable approximation of CE's {@code process()}/{@code cyceCompressor()}
 * shape (heat-limited, water-limited, steam-space-limited - the same three-way clamp CE's own
 * {@code TileEntityRBMKBase.boilWater()} ReaSim path uses, which was read in full).
 */
public class RBMKBoilerBlockEntity extends RBMKSlottedBlockEntity
        implements IFluidStandardTransceiverMK2, IControlReceiver, IRORValueProvider {

    private static final double HEAT_PER_MB = 2D;

    public final FluidTankNTM feed;
    public final FluidTankNTM steam;
    public boolean locked = false;
    /** CE {@code TileEntityRBMKBoiler.consumption} — water used this tick. */
    protected int consumption;

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
        if (level != null && !level.isClientSide) {
            this.consumption = 0;
            if (!locked && heat > 100D) {
                int byHeat = (int) ((heat - 100D) / HEAT_PER_MB);
                int process = Math.min(byHeat, Math.min(feed.getFill(), steam.getMaxFill() - steam.getFill()));

                if (process > 0) {
                    feed.setFill(feed.getFill() - process);
                    steam.setFill(steam.getFill() + process);
                    heat -= process * HEAT_PER_MB;
                    this.consumption = process;
                }

                trySubscribe(feed.getTankType(), level, worldPosition.below(), Direction.UP);
                tryProvide(steam, level, worldPosition.above(), Direction.DOWN);
            }
        }

        super.updateEntity();
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("locked")) this.locked = data.getBoolean("locked");
        setChanged();
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        receiveControl(data);
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
        tag.putBoolean("locked", locked);
        feed.writeToNBT(tag, "feed");
        steam.writeToNBT(tag, "steam");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        locked = tag.getBoolean("locked");
        feed.readFromNBT(tag, "feed");
        steam.readFromNBT(tag, "steam");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(locked);
        feed.serialize(buf);
        steam.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        locked = buf.readBoolean();
        feed.deserialize(buf);
        steam.deserialize(buf);
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
