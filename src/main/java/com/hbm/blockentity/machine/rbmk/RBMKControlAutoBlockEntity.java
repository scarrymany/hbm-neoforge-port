package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.rbmk.RBMKControlMath;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ICopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Heat-setpoint auto-control rod: interpolates a target extraction level from {@code heat} between
 * {@code heatLower}/{@code heatUpper} via one of three curve shapes. Ported from CE's
 * {@code TileEntityRBMKControlAuto} (196 lines).
 */
public class RBMKControlAutoBlockEntity extends RBMKControlBlockEntity implements IControlReceiver, ICopiable {

    public RBMKFunction function = RBMKFunction.LINEAR;
    public double levelLower;
    public double levelUpper;
    public double heatLower;
    public double heatUpper;

    public RBMKControlAutoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkControlAuto");
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.position().distanceToSqr(Vec3.atCenterOf(worldPosition)) < 400D;
    }

    @Override
    public void updateEntity() {
        if (!this.level.isClientSide) {
            double fauxLevel = RBMKControlMath.autoLevel(this.heat, this.heatLower, this.heatUpper,
                    this.levelLower, this.levelUpper, RBMKControlMath.AutoFunction.valueOf(this.function.name()));

            this.targetLevel = Math.max(0D, Math.min(1D, fauxLevel * 0.01D));
        }

        super.updateEntity();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("function")) {
            int c = Math.abs(data.getInt("function")) % RBMKFunction.VALUES.length;
            this.function = RBMKFunction.VALUES[c];
        } else {
            this.levelLower = data.getDouble("levelLower");
            this.levelUpper = data.getDouble("levelUpper");
            this.heatLower = data.getDouble("heatLower");
            this.heatUpper = data.getDouble("heatUpper");
        }
        setChanged();
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        receiveControl(data);
    }

    public enum RBMKFunction {
        LINEAR, QUAD_UP, QUAD_DOWN;

        public static final RBMKFunction[] VALUES = values();
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.CONTROL_AUTO;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("levelLower", levelLower);
        tag.putDouble("levelUpper", levelUpper);
        tag.putDouble("heatLower", heatLower);
        tag.putDouble("heatUpper", heatUpper);
        tag.putInt("function", function.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        levelLower = tag.getDouble("levelLower");
        levelUpper = tag.getDouble("levelUpper");
        heatLower = tag.getDouble("heatLower");
        heatUpper = tag.getDouble("heatUpper");
        function = tag.contains("function") ? RBMKFunction.VALUES[tag.getInt("function") % RBMKFunction.VALUES.length] : RBMKFunction.LINEAR;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.levelLower);
        buf.writeDouble(this.levelUpper);
        buf.writeDouble(this.heatLower);
        buf.writeDouble(this.heatUpper);
        buf.writeByte(function.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.levelLower = buf.readDouble();
        this.levelUpper = buf.readDouble();
        this.heatLower = buf.readDouble();
        this.heatUpper = buf.readDouble();
        this.function = RBMKFunction.VALUES[buf.readByte()];
    }

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putDouble("levelLower", levelLower);
        data.putDouble("levelUpper", levelUpper);
        data.putDouble("heatLower", heatLower);
        data.putDouble("heatUpper", heatUpper);
        data.putInt("function", function.ordinal());
        return data;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        if (nbt.contains("levelLower")) levelLower = nbt.getDouble("levelLower");
        if (nbt.contains("levelUpper")) levelUpper = nbt.getDouble("levelUpper");
        if (nbt.contains("heatLower")) heatLower = nbt.getDouble("heatLower");
        if (nbt.contains("heatUpper")) heatUpper = nbt.getDouble("heatUpper");
        if (nbt.contains("function")) {
            int f = nbt.getInt("function");
            function = f >= 0 && f < RBMKFunction.VALUES.length ? RBMKFunction.VALUES[f] : RBMKFunction.LINEAR;
        }
    }
}
