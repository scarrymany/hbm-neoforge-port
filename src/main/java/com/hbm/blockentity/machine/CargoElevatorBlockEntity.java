package com.hbm.blockentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Port of CE {@code com.hbm.tileentity.machine.TileEntityCargoElevator} - hydraulic lift platform logic.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityCargoElevator.java
 * <p>
 * Ported: extension animation (CE :63-70), per-tick {@code networkPackNT(300)} (CE :72-73),
 * entity lifting on both sides with server-side player skip (CE :83-97), toggleElevator (CE :100-108),
 * lower elevator merging (CE :43-60), client interpol (CE :74-80, :111-125),
 * ROR {@code setextension} (CE :170-176) + {@code getFunctionInfo} (CE :180-185).
 * {@link IRORValueProvider} is the CE torch stay-check ({@code RadioTorchController.java:55}) —
 * CE advertises {@code VAL:extension} but omitted the interface, so the controller cannot attach.
 * TODO(CE): custom rendering.
 */
public class CargoElevatorBlockEntity extends LoadedBaseBlockEntity
        implements ITickableBE, IRORInteractive, IRORValueProvider {

    public int height = 0; // CE :26 - number of additional blocks above base
    public int targetExtension = 0; // CE :28 - target platform height
    public double extension = 0; // CE :29 - current platform height (interpolated)
    public double prevExtension = 0; // CE :30 - for rendering interpolation
    public boolean renderPlatform = true; // CE :31 - whether to render the platform (base elevator only)

    // CE :35-38 - client-side interpolation for smooth animation
    private double syncExtension = 0;
    private int sync = 0;

    public static final double SPEED = 2D / 20D; // CE :33 - 0.1 blocks per tick

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CARGO_ELEVATOR_ENTITY.get(), pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        this.prevExtension = this.extension;

        if (!level.isClientSide) {
            // CE :43-60: Merge with lower elevator if placed on top of another cargo_elevator
            BlockState downState = level.getBlockState(worldPosition.below());
            if (downState.getBlock() == ModBlocks.CARGO_ELEVATOR.get()) {
                BlockPos lowerCore = ((BlockDummyable) ModBlocks.CARGO_ELEVATOR.get()).findCore(level, worldPosition.below());
                if (lowerCore != null && lowerCore.getX() == worldPosition.getX() && lowerCore.getZ() == worldPosition.getZ()) {
                    BlockEntity lowerTile = level.getBlockEntity(lowerCore);
                    if (lowerTile instanceof CargoElevatorBlockEntity lower) {
                        lower.height += this.height + 1;
                        for (int x = worldPosition.getX() - 1; x < worldPosition.getX() + 2; x++) {
                            for (int z = worldPosition.getZ() - 1; z < worldPosition.getZ() + 2; z++) {
                                for (int y = worldPosition.getY(); y <= worldPosition.getY() + this.height; y++) {
                                    level.setBlock(new BlockPos(x, y, z),
                                            ModBlocks.CARGO_ELEVATOR.get().defaultBlockState().setValue(BlockDummyable.META, 1), 3);
                                }
                            }
                        }
                        lower.setChanged();
                        return;
                    }
                }
            }

            // CE :63-70: Extension animation (move platform up/down towards target)
            if (this.extension < this.targetExtension) {  // go up
                this.extension += SPEED;
                this.extension = Mth.clamp(this.extension, 0D, this.targetExtension);
            } else if (this.extension > this.targetExtension) {  // go down
                this.extension -= SPEED;
                this.extension = Mth.clamp(this.extension, this.targetExtension, this.height);
            }

            this.extension = Mth.clamp(this.extension, 0D, this.height);
            // CE :72-73 — clients interpolate the moving platform; skip-identical in networkPackNT
            this.renderPlatform = true;
            this.networkPackNT(300);
        } else {
            // CE :74-80 - client-side smooth interpolation
            if (this.sync > 0) {
                this.extension = this.extension + ((this.syncExtension - this.extension) / (float) this.sync);
                --this.sync;
            } else {
                this.extension = this.syncExtension;
            }
        }

        // CE :83-97: lift on both sides. Server skips players (client owns local player motion).
        if (this.extension != this.prevExtension) {
            double liftUpper = this.worldPosition.getY() + 1D + Math.max(this.extension, this.prevExtension);
            double liftLower = this.worldPosition.getY() + 1D + Math.min(this.extension, this.prevExtension);
            AABB liftBox = new AABB(
                    this.worldPosition.getX() - 0.99D, liftLower, this.worldPosition.getZ() - 0.99D,
                    this.worldPosition.getX() + 1.99D, liftUpper, this.worldPosition.getZ() + 1.99D
            );

            List<Entity> toLift = level.getEntities((Entity) null, liftBox);

            for (Entity entity : toLift) {
                if (entity instanceof Player && !level.isClientSide) continue; // CE :89
                AABB entityBox = entity.getBoundingBox();
                if (entityBox.minY >= liftLower && entityBox.minY <= liftUpper) {
                    double delta = entityBox.minY - (this.worldPosition.getY() + 1D + this.extension);
                    entity.move(MoverType.SELF, new Vec3(0.0D, -delta, 0.0D)); // CE :92
                    entity.setOnGround(true);
                    entity.move(MoverType.SELF, new Vec3(0.0D, -0.125D, 0.0D)); // CE :94
                }
            }
        }
    }

    // CE :100-108: Toggle elevator between retracted (0) and extended (height)
    public void toggleElevator() {
        if (this.targetExtension == 0) {
            this.targetExtension = this.height;
        } else {
            this.targetExtension = 0;
        }
        setChanged();
        // CE :106-107 markDirty/markChanged only — per-tick networkPackNT(300) carries extension
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.height = tag.getInt("height");
        this.targetExtension = tag.getInt("targetExtension");
        this.extension = tag.getDouble("extension");
        this.renderPlatform = tag.getBoolean("renderPlatform");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("height", height);
        tag.putInt("targetExtension", targetExtension);
        tag.putDouble("extension", extension);
        tag.putBoolean("renderPlatform", renderPlatform);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.renderPlatform); // CE :112
        buf.writeShort((short) this.height); // CE :113
        buf.writeDouble(this.extension); // CE :114
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.renderPlatform = buf.readBoolean(); // CE :120
        this.height = buf.readShort(); // CE :121
        this.syncExtension = buf.readDouble(); // CE :122
        // CE :123-125 - start smooth interpolation if extension changed
        if (this.syncExtension > 0 && this.syncExtension < this.height) {
            this.sync = 3;
        }
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :180-185
        return new String[]{
                PREFIX_VALUE + "extension",
                PREFIX_FUNCTION + "setextension"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :170-176
        if ((PREFIX_FUNCTION + "setextension").equals(name) && params.length > 0) {
            targetExtension = IRORInteractive.parseInt(params[0], 0, height);
            setChanged();
            return null;
        }
        return null;
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "extension").equals(name)) return String.valueOf((int) this.extension);
        return null;
    }
}
