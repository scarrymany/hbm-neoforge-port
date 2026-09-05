package com.hbm.blockentity.network;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.blockentity.network.RTTYSystem.RTTYChannel;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityRadioTorchController}. */
public class RadioTorchControllerBlockEntity extends RadioTorchBaseBlockEntity {

    public String prev = "";

    public RadioTorchControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.polling = true;
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            if (channel != null && !channel.isEmpty()) {
                Direction dir = getTorchFacing().getOpposite();
                BlockEntity tile = level.getBlockEntity(worldPosition.relative(dir));
                if (tile instanceof IRORInteractive ror) {
                    RTTYChannel chan = RTTYSystem.listen(level, channel);
                    if (chan != null) {
                        String rec = "" + chan.signal;
                        if ("selfdestruct".equals(rec)) {
                            BlockPos blow = worldPosition;
                            level.destroyBlock(blow, false);
                            ExplosionVNT vnt = new ExplosionVNT(level, blow.getX() + 0.5, blow.getY() + 0.5, blow.getZ() + 0.5, 5, null);
                            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
                            vnt.setPlayerProcessor(new PlayerProcessorStandard());
                            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                            vnt.explode();
                            return;
                        }
                        if ((this.polling && chan.timeStamp >= level.getGameTime() - 1) || !rec.equals(prev)) {
                            try {
                                if (!rec.isEmpty()) {
                                    ror.runRORFunction(
                                            IRORInteractive.PREFIX_FUNCTION + IRORInteractive.getCommand(rec),
                                            IRORInteractive.getParams(rec));
                                }
                            } catch (RORFunctionException ignored) {
                            }
                            prev = rec;
                        }
                    }
                }
            }
        }
        super.updateEntity();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("prev", prev == null ? "" : prev);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        prev = tag.getString("prev");
        if (!tag.contains("isPolling") && !tag.contains("polling")) polling = true;
    }
}
