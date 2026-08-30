package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.rbmk.RBMKControlMath;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blocks.machine.rbmk.RBMKControlBlock;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ICopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Operator-facing manual control rod, ported from CE's {@code TileEntityRBMKControlManual}
 * (279 lines). Owns the operator {@link RBMKColor} tag and the power-surge formula on rod
 * withdrawal - per the research report, "one of the highest-value functions to unit-test given the
 * project's own framing" (the Chernobyl-reference positive-void-coefficient effect).
 */
public class RBMKControlManualBlockEntity extends RBMKControlBlockEntity implements IControlReceiver, ICopiable {

    public RBMKColor color;
    public double startingLevel;

    public RBMKControlManualBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkControl");
    }

    @Override
    public boolean isModerated() {
        return getBlockState().getBlock() instanceof RBMKControlBlock cb && cb.moderated;
    }

    @Override
    public void setTarget(double target) {
        this.targetLevel = target;
        this.startingLevel = this.extraction;
    }

    /**
     * The Chernobyl-reference positive-void/scram-coefficient surge: withdrawing a rod that was
     * previously more inserted produces a transient extra flux multiplier that spikes sharply right
     * as the rod starts moving and vanishes almost immediately after, because
     * {@code pow(1 - extraction, 15)} concentrates nearly the whole {@code sin} swing into the last
     * few percent of the rod's travel. CE: {@code TileEntityRBMKControlManual.getMult()}.
     */
    @Override
    public double getMult() {
        if (!(this.level instanceof ServerLevel serverLevel)) return this.extraction;

        // com.hbm.api.rbmk.RBMKControlMath.getEffectiveMult - the sibling package's pure extraction
        // of this exact surge formula, called here rather than reimplementing it inline.
        return RBMKControlMath.getEffectiveMult(this.extraction, this.startingLevel, this.targetLevel,
                RBMKDials.getSurgeMod(serverLevel));
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.position().distanceToSqr(Vec3.atCenterOf(worldPosition)) < 400D; // 20 blocks
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("level")) {
            this.setTarget(data.getDouble("level"));
        }
        if (data.contains("color")) {
            int c = Math.abs(data.getInt("color")) % RBMKColor.VALUES.length;
            RBMKColor newColor = RBMKColor.VALUES[c];
            this.color = (newColor == this.color) ? null : newColor;
        }
        setChanged();
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        receiveControl(data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("startingLevel", this.startingLevel);
        if (color != null) tag.putInt("color", color.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("startingLevel")) this.startingLevel = tag.getDouble("startingLevel");
        this.color = tag.contains("color") ? RBMKColor.VALUES[tag.getInt("color") % RBMKColor.VALUES.length] : null;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.startingLevel);
        buf.writeByte(this.color != null ? this.color.ordinal() : -1);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.startingLevel = buf.readDouble();
        int c = buf.readByte();
        this.color = c >= 0 ? RBMKColor.VALUES[Math.min(c, RBMKColor.VALUES.length - 1)] : null;
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.CONTROL;
    }

    @Override
    public RBMKColumn getConsoleData() {
        RBMKColumn.ControlColumn data = (RBMKColumn.ControlColumn) super.getConsoleData();
        data.color = (short) (this.color != null ? this.color.ordinal() : -1);
        return data;
    }

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        if (color != null) data.putInt("color", color.ordinal());
        return data;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        if (nbt.contains("color")) {
            int c = nbt.getInt("color");
            this.color = c >= 0 && c < RBMKColor.VALUES.length ? RBMKColor.VALUES[c] : null;
        }
    }

    /** CE nests this on {@code TileEntityRBMKControlManual} - kept nested here for naming stability, per the research report. */
    public enum RBMKColor {
        RED, YELLOW, GREEN, BLUE, PURPLE;

        public static final RBMKColor[] VALUES = values();
    }
}
