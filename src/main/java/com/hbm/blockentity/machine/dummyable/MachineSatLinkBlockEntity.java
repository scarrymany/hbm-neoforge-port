package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * CE {@code TileEntityMachineSatLink}. Freq + sky check + ROR.
 * TODO(CE: RenderSatLink.java:16): TESR dish.
 * TODO(CE: TileEntityMachineSatLink.java:201-270): OpenComputers callbacks.
 */
public class MachineSatLinkBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IRORValueProvider, IRORInteractive {

    public boolean connected;
    public int freq;
    public Component[] info = new Component[0];

    public MachineSatLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_satlink");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.connected = false;
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, worldPosition.getX(), worldPosition.getZ()) <= worldPosition.getY()) {
            SatelliteSavedData dat = SatelliteSavedData.getData(level);
            this.connected = dat.isFreqTaken(freq);
        }
        updateInfo(connected);
        dataChanged();
        networkPackMK2(150);
    }

    private void updateInfo(boolean canConnect) {
        if (!canConnect) {
            if (this.info.length > 0) this.info = new Component[0];
            return;
        }
        Satellite sat = SatelliteSavedData.getData(level).getSatFromFreq(freq);
        if (sat != null) this.info = sat.getInfo(level);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "connected",
                PREFIX_VALUE + "freq",
                PREFIX_VALUE + "rx",
                PREFIX_VALUE + "type",
                PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq",
                PREFIX_FUNCTION + "tx" + NAME_SEPARATOR + "payload"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if (name.equals(PREFIX_VALUE + "connected")) return this.connected ? "TRUE" : "FALSE";
        if (name.equals(PREFIX_VALUE + "freq")) return "" + this.freq;
        if (level == null || level.isClientSide) return null;
        SatelliteSavedData dat = SatelliteSavedData.getData(level);
        Satellite sat = dat.getSatFromFreq(this.freq);
        if (name.equals(PREFIX_VALUE + "type")) return sat != null ? sat.getType() : "";
        if (name.equals(PREFIX_VALUE + "rx")) return sat != null ? sat.tx : "";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if (name.equals(PREFIX_FUNCTION + "setfreq") && params.length == 1) {
            this.freq = IRORInteractive.parseInt(params[0], 0, 100_000);
            setChanged();
        }
        if (name.equals(PREFIX_FUNCTION + "tx") && level != null && !level.isClientSide) {
            SatelliteSavedData dat = SatelliteSavedData.getData(level);
            Satellite sat = dat.getSatFromFreq(this.freq);
            String[] cmd = String.join(IRORInteractive.PARAM_SEPARATOR, params).split(" ");
            if (sat != null) {
                sat.onCommand(level, cmd);
                dat.setDirty();
            }
            SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    SatelliteRayScan.RayEvent.INFO_RADIO, 300);
            setChanged();
        }
        return null;
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 4, worldPosition.getZ() + 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("freq", freq);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        freq = tag.getInt("freq");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(connected);
        buf.writeInt(freq);
        buf.writeVarInt(info.length);
        for (Component comp : info) {
            buf.writeJsonWithCodec(ComponentSerialization.CODEC, comp);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        connected = buf.readBoolean();
        freq = buf.readInt();
        this.info = new Component[buf.readVarInt()];
        for (int i = 0; i < info.length; i++) {
            Component comp = buf.readJsonWithCodec(ComponentSerialization.CODEC);
            this.info[i] = comp != null ? comp : Component.empty();
        }
    }
}
