package com.hbm.blockentity.machine;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Port of CE {@code com.hbm.tileentity.machine.TileEntityCargoElevator} - hydraulic lift platform logic.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityCargoElevator.java
 * <p>
 * Ported: extension animation (CE :63-70), entity lifting (CE :83-97), toggleElevator (CE :100-108),
 * lower elevator merging (CE :43-60), client sync (CE :74-80, :111-125) via LoadedBaseBlockEntity.
 * TODO(CE): ROR integration (CE :170-185), custom rendering.
 */
public class CargoElevatorBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

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

        // CE :74-80 - client-side smooth interpolation
        if (level.isClientSide) {
            if (this.sync > 0) {
                this.extension = this.extension + ((this.syncExtension - this.extension) / (float) this.sync);
                --this.sync;
            } else {
                this.extension = this.syncExtension;
            }
            return;
        }

        // CE :43-60: Merge with lower elevator if placed on top of another cargo_elevator
        BlockState downState = level.getBlockState(worldPosition.below());
        if (downState.getBlock() == ModBlocks.CARGO_ELEVATOR.get()) {
            BlockPos lowerCore = ((BlockDummyable) ModBlocks.CARGO_ELEVATOR.get()).findCore(level, worldPosition.below());
            if (lowerCore != null && lowerCore.getX() == worldPosition.getX() && lowerCore.getZ() == worldPosition.getZ()) {
                BlockEntity lowerTile = level.getBlockEntity(lowerCore);
                if (lowerTile instanceof CargoElevatorBlockEntity lower) {
                    lower.height += this.height + 1;
                    // Convert this elevator's blocks to dummy parts of the lower elevator
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

        // CE :83-97: Entity lifting - move entities standing on the platform
        if (this.extension != this.prevExtension) {
            double liftUpper = this.worldPosition.getY() + 1D + Math.max(this.extension, this.prevExtension);
            double liftLower = this.worldPosition.getY() + 1D + Math.min(this.extension, this.prevExtension);
            AABB liftBox = new AABB(
                    this.worldPosition.getX() - 0.99D, liftLower, this.worldPosition.getZ() - 0.99D,
                    this.worldPosition.getX() + 1.99D, liftUpper, this.worldPosition.getZ() + 1.99D
            );

            List<Entity> toLift = level.getEntities((Entity) null, liftBox);

            for (Entity entity : toLift) {
                AABB entityBox = entity.getBoundingBox();
                if (entityBox.minY >= liftLower && entityBox.minY <= liftUpper) {
                    double delta = entityBox.minY - (this.worldPosition.getY() + 1D + this.extension);
                    entity.setPos(entity.getX(), entity.getY() - delta, entity.getZ());
                    entity.setOnGround(true);
                    entity.setPos(entity.getX(), entity.getY() - 0.125D, entity.getZ());
                }
            }
        }

        setChanged();
    }

    // CE :100-108: Toggle elevator between retracted (0) and extended (height)
    public void toggleElevator() {
        if (this.targetExtension == 0) {
            this.targetExtension = this.height;
        } else {
            this.targetExtension = 0;
        }
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.networkPackNT(20); // CE :106-107 - sync to clients within 20 blocks
        }
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
}
