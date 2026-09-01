package com.hbm.blockentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.PileBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityPileControl}. Writes {@code channel.control} while sitting on META_CONTROL.
 * RS rising → target 1, falling → 0. SPEED 1/60.
 * Field {@code rodLevel} is CE {@code level} — renamed so it does not shadow {@code BlockEntity.level}.
 * TODO(CE: TileEntityPileControl.java:165-182): OpenComputers callbacks.
 * TODO(CE: RenderPileControl.java:1): OBJ TESR.
 */
public class PileControlBlockEntity extends PileDeviceBaseBlockEntity implements IRORInteractive {

    public double syncLevel;
    public double rodLevel;
    public double lastLevel;
    public int turnProgress;

    public double targetLevel;
    public static final double SPEED = 1D / 60D;
    public boolean wasRedstone;

    public PileControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            boolean canMove = false;
            BlockPos below = worldPosition.below();
            BlockState belowState = level.getBlockState(below);

            if (belowState.getBlock() == PileBlocks.PILE_BLOCK.get()
                    && belowState.getValue(BlockPile.META) == BlockPile.META_CONTROL) {
                BlockEntity tile = level.getBlockEntity(below);
                if (tile instanceof PileBaseBlockEntity pile) {
                    PileCoreBlockEntity core = pile.getCore();
                    if (core != null) {
                        PileCoreBlockEntity.PileChannel controlChan = core.getControlChannel(below.getX(), below.getY(), below.getZ());
                        if (controlChan != null) {
                            canMove = true;
                            this.chanNum = core.controlChannels.indexOf(controlChan);
                            controlChan.control = this.rodLevel;
                        }
                    }
                }
            }

            if (canMove && this.rodLevel != this.targetLevel) {
                if (Math.abs(rodLevel - targetLevel) <= SPEED) {
                    this.rodLevel = this.targetLevel;
                } else if (rodLevel < targetLevel) {
                    this.rodLevel += SPEED;
                } else {
                    this.rodLevel -= SPEED;
                }
            }

            Direction dir = this.getOrientation();
            boolean redstone = level.getSignal(worldPosition.relative(dir), dir.getOpposite()) > 0;
            if (redstone && !wasRedstone) this.setTarget(1D);
            if (!redstone && wasRedstone) this.setTarget(0D);
            this.wasRedstone = redstone;

            this.networkPackNT(100);
        } else {
            this.lastLevel = this.rodLevel;
            if (this.turnProgress > 0) {
                this.rodLevel = this.rodLevel + ((this.syncLevel - this.rodLevel) / (double) this.turnProgress);
                --this.turnProgress;
            } else {
                this.rodLevel = this.syncLevel;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.rodLevel);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        double lastSync = this.syncLevel;
        this.syncLevel = buf.readDouble();
        if (this.syncLevel != lastSync) this.turnProgress = 2;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.rodLevel = nbt.getDouble("level");
        this.targetLevel = nbt.getDouble("targetLevel");
        this.wasRedstone = nbt.getBoolean("redstone");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putDouble("level", rodLevel);
        nbt.putDouble("targetLevel", targetLevel);
        nbt.putBoolean("wasRedstone", wasRedstone);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "extendrods" + NAME_SEPARATOR + "percent"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], 0, 100);
            this.setTarget(percent / 100D);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "extendrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], -100, 100);
            this.setTarget(Mth.clamp(this.targetLevel + percent / 100D, 0D, 1D));
            setChanged();
            return null;
        }
        return null;
    }

    public void setTarget(double target) {
        this.targetLevel = target;
    }
}
