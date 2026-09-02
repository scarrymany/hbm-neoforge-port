package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.machine.FloodlightBeamBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.Vec3NT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloodlightBlockEntity extends BlockEntity implements IEnergyReceiverMK2 {

    public float rotation;
    protected BlockPos[] lightPos = new BlockPos[15];
    public static final long maxPower = 5_000;
    public long power;

    public int delay;
    public boolean isOn;

    private boolean isLoaded = true;

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FLOODLIGHT_ENTITY.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockState().getValue(com.hbm.blocks.machine.Floodlight.META) % 6).getOpposite();
        this.trySubscribe(
                level,
                getBlockPos().getX() + dir.offsetX,
                getBlockPos().getY() + dir.offsetY,
                getBlockPos().getZ() + dir.offsetZ,
                dir.toDirection());

        if (delay > 0) {
            delay--;
            return;
        }

        if (power >= 100) {
            power -= 100;

            if (!isOn) {
                BlockState state = level.getBlockState(getBlockPos());

                this.isOn = true;
                this.castLights();
                setChanged();
                level.sendBlockUpdated(getBlockPos(), state, state, 3);
            } else {
                long timer = level.getGameTime();
                if (timer % 5 == 0) {
                    timer = timer / 5;
                    this.castLight((int) Math.abs(timer % this.lightPos.length));
                }
            }
        } else {
            if (isOn) {
                BlockState state = level.getBlockState(getBlockPos());

                this.isOn = false;
                this.delay = 60;
                this.destroyLights();
                setChanged();
                level.sendBlockUpdated(getBlockPos(), state, state, 3);
            }
        }
    }

    private void castLight(int index) {
        if (level == null) return;

        BlockPos newPos = this.getRayEndpoint(index);
        BlockPos oldPos = this.lightPos[index];
        this.lightPos[index] = null;

        if (newPos == null || !newPos.equals(oldPos)) {
            if (oldPos != null) {
                BlockEntity tile = level.getBlockEntity(oldPos);
                if (tile instanceof FloodlightBeamBlockEntity beam) {
                    if (beam.cache == this) {
                        level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        if (newPos == null) return;

        if (level.getBlockState(newPos).getBlock() == Blocks.AIR) {
            level.setBlock(newPos, ModBlocks.FLOODLIGHT_BEAM.get().defaultBlockState(), 2);
            BlockEntity tile = level.getBlockEntity(newPos);
            if (tile instanceof FloodlightBeamBlockEntity beam) {
                beam.setSource(this, newPos.getX(), newPos.getY(), newPos.getZ(), index);
            }
            this.lightPos[index] = newPos;
        }

        if (level.getBlockState(newPos).getBlock() == ModBlocks.FLOODLIGHT_BEAM.get()) {
            this.lightPos[index] = newPos;
        }
    }

    public BlockPos getRayEndpoint(int index) {
        if (index < 0 || index >= lightPos.length) return null;

        int meta = this.getBlockState().getValue(com.hbm.blocks.machine.Floodlight.META);
        Vec3NT dir = new Vec3NT(1, 0, 0);

        float[] angles = getVariation(index);

        float rotation = this.rotation;
        if (meta == 1 || meta == 7) rotation = 180 - rotation;
        if (meta == 6) rotation = 180 - rotation;
        dir.rotateRollSelf((float) (rotation / 180D * Math.PI) + angles[0]);

        if (meta == 6) dir.rotateYawSelf((float) (Math.PI / 2D));
        if (meta == 7) dir.rotateYawSelf((float) (Math.PI / 2D));
        if (meta == 2) dir.rotateYawSelf((float) (Math.PI / 2D));
        if (meta == 3) dir.rotateYawSelf((float) -(Math.PI / 2D));
        if (meta == 4) dir.rotateYawSelf((float) (Math.PI));
        dir.rotateYawSelf(angles[1]);

        for (int i = 1; i < 64; i++) {
            int x = getBlockPos().getX();
            int y = getBlockPos().getY();
            int z = getBlockPos().getZ();

            int iX = (int) Math.floor(x + 0.5 + dir.x * i);
            int iY = (int) Math.floor(y + 0.5 + dir.y * i);
            int iZ = (int) Math.floor(z + 0.5 + dir.z * i);

            if (iX == x && iY == y && iZ == z) continue;

            BlockState state = level.getBlockState(new BlockPos(iX, iY, iZ));
            if (state.getLightBlock(level, new BlockPos(iX, iY, iZ)) < 127) continue;

            int fX = (int) Math.floor(x + 0.5 + dir.x * (i - 1));
            int fY = (int) Math.floor(y + 0.5 + dir.y * (i - 1));
            int fZ = (int) Math.floor(z + 0.5 + dir.z * (i - 1));

            if (i > 1) return new BlockPos(fX, fY, fZ);
        }

        return null;
    }

    private void castLights() {
        for (int i = 0; i < this.lightPos.length; i++) this.castLight(i);
    }

    private void destroyLight(int index) {
        if (level == null) return;
        BlockPos pos = lightPos[index];
        if (pos != null && level.getBlockState(pos).getBlock() == ModBlocks.FLOODLIGHT_BEAM.get()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public void destroyLights() {
        for (int i = 0; i < this.lightPos.length; i++) destroyLight(i);
    }

    private float[] getVariation(int index) {
        return new float[]{
                ((((float) index / 3) - 2) * 7.5F) / 180F * (float) Math.PI,
                (((index % 3) - 1) * 15F) / 180F * (float) Math.PI
        };
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        saveAdditional(nbt, registries);
        return nbt;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.rotation = tag.getFloat("rotation");
        this.power = tag.getLong("power");
        this.isOn = tag.getBoolean("isOn");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("rotation", rotation);
        tag.putLong("power", power);
        tag.putBoolean("isOn", isOn);
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
        return maxPower;
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.isLoaded = false;
    }

    private AABB bb = null;

    public @NotNull AABB getRenderBoundingBox() {
        if (bb == null) {
            int x = getBlockPos().getX();
            int y = getBlockPos().getY();
            int z = getBlockPos().getZ();
            bb = new AABB(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2);
        }
        return bb;
    }
}
