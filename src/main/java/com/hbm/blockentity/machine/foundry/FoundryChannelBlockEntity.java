package com.hbm.blockentity.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blockentity.ITickableBE;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NeoForge port of CE {@code TileEntityFoundryChannel} - molten metal channel BlockEntity.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityFoundryChannel.java
 * <p>
 * Flow logic (CE :54-120):
 * - Priority: flows to non-channel ICrucibleAcceptors first (outlets, molds)
 * - Then spreads to adjacent channels (equalization + random swap)
 * - Material type propagation to prevent clogs (CE :180-217)
 * - lastFlow tracking for flow direction bias (CE :62-64)
 */
public class FoundryChannelBlockEntity extends FoundryBaseBlockEntity implements ITickableBE {

    public int nextUpdate;
    public int lastFlow = 0;

    protected NTMMaterial neighborType;
    protected boolean hasCheckedNeighbors;
    protected int unpropagateTime;

    public FoundryChannelBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryBlockEntities.FOUNDRY_CHANNEL_BE_TYPE.get(), pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (!hasCheckedNeighbors) {
            List<FoundryChannelBlockEntity> visited = new ArrayList<>();
            visited.add(this);
            neighborType = checkNeighbors(visited);
            hasCheckedNeighbors = true;
        }

        if (this.type == null && this.amount != 0) {
            this.amount = 0;
        }

        nextUpdate--;

        if (nextUpdate <= 0 && this.amount > 0 && this.type != null) {
            boolean hasOp = false;
            nextUpdate = 5;

            List<Integer> ints = new ArrayList<>(List.of(2, 3, 4, 5));
            Collections.shuffle(ints);
            if (lastFlow > 0) {
                ints.remove((Integer) this.lastFlow);
                ints.add(this.lastFlow);
            }

            for (Integer i : ints) {
                ForgeDirection dir = ForgeDirection.getOrientation(i);
                BlockPos target = worldPosition.offset(dir.offsetX, 0, dir.offsetZ);
                Block b = level.getBlockState(target).getBlock();

                if (b instanceof ICrucibleAcceptor && !(b instanceof com.hbm.blocks.machine.foundry.BlockFoundryChannel)) {
                    ICrucibleAcceptor acc = (ICrucibleAcceptor) b;

                    if (acc.canAcceptPartialFlow(level, target, dir.getOpposite().toDirection(), new Mats.MaterialStack(this.type, this.amount))) {
                        Mats.MaterialStack left = acc.flow(level, target, dir.getOpposite().toDirection(), new Mats.MaterialStack(this.type, this.amount));
                        if (left == null) {
                            this.type = null;
                            this.amount = 0;
                            propagateMaterial(null);
                        } else {
                            this.amount = left.amount;
                        }
                        hasOp = true;
                        break;
                    }
                }
            }

            if (!hasOp) {
                RandomSource rand = level.getRandom();
                for (Integer i : ints) {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    BlockEntity b = level.getBlockEntity(worldPosition.offset(dir.offsetX, 0, dir.offsetZ));

                    if (b instanceof FoundryChannelBlockEntity acc) {
                        if (acc.type == null || acc.type == this.type || acc.amount == 0) {
                            acc.type = this.type;
                            acc.lastFlow = dir.getOpposite().ordinal();

                            if (rand.nextInt(5) == 0 || this.amount == 1) {
                                int buf = this.amount;
                                this.amount = acc.amount;
                                acc.amount = buf;
                            } else {
                                int diff = this.amount - acc.amount;

                                if (diff > 0) {
                                    diff /= 2;
                                    this.amount -= diff;
                                    acc.amount += diff;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (neighborType != null && amount == 0) unpropagateTime++;

        if (unpropagateTime > 100) {
            propagateMaterial(null);
        }

        if (this.amount == 0) {
            this.lastFlow = 0;
            this.nextUpdate = 5;
        } else {
            unpropagateTime = 0;
        }
    }

    @Override
    public int getCapacity() {
        return MaterialShapes.INGOT.q(2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("flow", (byte) this.lastFlow);
        tag.putInt("nType", this.neighborType != null ? this.neighborType.id : -1);
        tag.putBoolean("init", hasCheckedNeighbors);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.lastFlow = tag.getByte("flow");
        if (tag.contains("nType")) {
            int typeId = tag.getInt("nType");
            this.neighborType = typeId == -1 ? null : Mats.matById.get(typeId);
        }
        this.hasCheckedNeighbors = tag.getBoolean("init");
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        if (!hasCheckedNeighbors || (neighborType != null && neighborType != stack.material)) return false;
        return super.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        propagateMaterial(stack.material);
        return super.pour(world, pos, dX, dY, dZ, side, stack);
    }

    public void propagateMaterial(NTMMaterial propType) {
        if (propType != null && neighborType != null) return;

        List<FoundryChannelBlockEntity> visited = new ArrayList<>();
        visited.add(this);

        boolean hasMaterial = propagateMaterial(propType, visited, false);

        if (propType == null && !hasMaterial) {
            for (FoundryChannelBlockEntity acc : visited) {
                acc.neighborType = null;
            }
        }
    }

    protected boolean propagateMaterial(NTMMaterial propType, List<FoundryChannelBlockEntity> visited, boolean hasMaterial) {
        if (propType != null) {
            neighborType = propType;
        } else {
            unpropagateTime = 0;
        }

        for (ForgeDirection dir : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
            BlockEntity b = level.getBlockEntity(worldPosition.offset(dir.offsetX, 0, dir.offsetZ));

            if (b instanceof FoundryChannelBlockEntity acc && !visited.contains(b)) {
                visited.add(acc);

                if (acc.amount > 0) hasMaterial = true;

                hasMaterial = acc.propagateMaterial(propType, visited, hasMaterial);
            }
        }

        return hasMaterial;
    }

    protected NTMMaterial checkNeighbors(List<FoundryChannelBlockEntity> visited) {
        if (neighborType != null) return neighborType;

        for (ForgeDirection dir : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
            BlockEntity b = level.getBlockEntity(worldPosition.offset(dir.offsetX, 0, dir.offsetZ));

            if (b instanceof FoundryChannelBlockEntity acc && !visited.contains(b)) {
                visited.add(acc);

                NTMMaterial neighborMaterial = acc.checkNeighbors(visited);

                if (neighborMaterial != null) return neighborMaterial;
            }
        }

        return null;
    }
}
