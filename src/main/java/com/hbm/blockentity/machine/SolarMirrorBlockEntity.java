package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntitySolarMirror} (block {@code SolarMirror}, regname
 * {@code solar_mirror}, read in full): a sun-tracking heliostat, independently placed (not
 * dummyable - each mirror just points at one target). Every tick, checks real sky light
 * ({@code level.getBrightness(SKY, pos) - skyDarken - 11}) and, if lit, adds that value directly
 * into the target {@link SolarBoilerBlockEntity#heatInput} field by looking up the block entity one
 * block below the stored target - a direct cross-block-entity field write, not a capability or the
 * HE/fluid network, exactly matching CE.
 * <p>
 * CE's target is set via {@code setTarget(x, y, z)}, presumably called by a "mirror tool" item this
 * pass does not own (flagged by the research report as belonging to whichever Phase 2/3 area lands
 * the tool bucket). {@link #setTarget} is kept public and ready for that item; until it's called,
 * {@link #tX}/{@link #tY}/{@link #tZ} default to 0 (below any real boiler), so the mirror harmlessly
 * finds nothing and stays inert - matching CE's own behavior for an unaimed mirror.
 * <p>
 * Uses {@link MachineBaseBlockEntity} (0 slots, no capability wrappers) rather than CE's much
 * lighter {@code TileEntityTickingBase} for consistency with every other block entity in this
 * package, per this pass's brief; the extra inventory/capability plumbing is simply unused.
 */
public class SolarMirrorBlockEntity extends MachineBaseBlockEntity implements ITickableBE {

    public int tX;
    public int tY;
    public int tZ;
    public boolean isOn;

    public SolarMirrorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.solarMirror");
    }

    public void setTarget(int x, int y, int z) {
        this.tX = x;
        this.tY = y;
        this.tZ = z;
        setChanged();
        dataChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (tY < worldPosition.getY()) {
            isOn = false;
            return;
        }

        int sun = level.getBrightness(LightLayer.SKY, worldPosition) - level.getSkyDarken() - 11;
        if (sun <= 0 || !level.canSeeSky(worldPosition.above())) {
            isOn = false;
            return;
        }

        isOn = true;
        BlockEntity target = level.getBlockEntity(new BlockPos(tX, tY - 1, tZ));
        if (target instanceof SolarBoilerBlockEntity boiler) {
            boiler.heatInput += sun;
        }

        dataChanged();
        networkPackMK2(200);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("targetX", tX);
        tag.putInt("targetY", tY);
        tag.putInt("targetZ", tZ);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tX = tag.getInt("targetX");
        tY = tag.getInt("targetY");
        tZ = tag.getInt("targetZ");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(tX);
        buf.writeInt(tY);
        buf.writeInt(tZ);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tX = buf.readInt();
        tY = buf.readInt();
        tZ = buf.readInt();
        isOn = buf.readBoolean();
    }
}
