package com.hbm.blockentity.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blocks.machine.foundry.BlockFoundryChannel;
import com.hbm.blocks.machine.foundry.BlockFoundryTank;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NeoForge port of CE {@code TileEntityFoundryTank} - molten metal storage tank BlockEntity.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityFoundryTank.java
 * <p>
 * Extends {@link FoundryBaseBlockEntity} for shared molten metal storage logic.
 * Update logic (CE :22-110):
 * - Gravity flow: drains down into lower tanks
 * - Horizontal flow: spreads to adjacent tanks or outlets
 * - Random update interval (5-10 ticks) to prevent excessive processing
 */
public class FoundryTankBlockEntity extends FoundryBaseBlockEntity implements ITickableBE {

    public int nextUpdate;

    public FoundryTankBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryBlockEntities.FOUNDRY_TANK_BE_TYPE.get(), pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (this.type == null && this.amount != 0) {
            this.amount = 0;
        }

        nextUpdate--;

        if (nextUpdate <= 0 && this.amount > 0 && this.type != null) {
            boolean hasOp = false;
            RandomSource rand = level.getRandom();
            nextUpdate = rand.nextInt(6) + 5;

            BlockEntity te = level.getBlockEntity(worldPosition.below());

            if (te instanceof FoundryTankBlockEntity tank) {
                if ((tank.type == null || tank.type == this.type) && tank.amount < tank.getCapacity()) {
                    tank.type = this.type;
                    int toFill = Math.min(this.amount, tank.getCapacity() - tank.amount);
                    this.amount -= toFill;
                    tank.amount += toFill;
                    hasOp = true;
                }
            }

            List<Integer> ints = new ArrayList<>(List.of(2, 3, 4, 5));
            Collections.shuffle(ints);

            if (!hasOp) {
                for (Integer i : ints) {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    BlockPos target = worldPosition.offset(dir.offsetX, 0, dir.offsetZ);
                    Block b = level.getBlockState(target).getBlock();

                    // CE TileEntityFoundryTank.java:62 — skip foundry_channel (own flow); outlet/mold only
                    if (b instanceof ICrucibleAcceptor && !(b instanceof BlockFoundryChannel)) {
                        ICrucibleAcceptor acc = (ICrucibleAcceptor) b;

                        if (acc.canAcceptPartialFlow(level, target, dir.getOpposite().toDirection(), new Mats.MaterialStack(this.type, this.amount))) {
                            Mats.MaterialStack left = acc.flow(level, target, dir.getOpposite().toDirection(), new Mats.MaterialStack(this.type, this.amount));
                            if (left == null) {
                                this.type = null;
                                this.amount = 0;
                            } else {
                                this.amount = left.amount;
                            }
                            hasOp = true;
                            break;
                        }
                    }
                }
            }

            if (!hasOp) {
                for (Integer i : ints) {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    BlockEntity b = level.getBlockEntity(worldPosition.offset(dir.offsetX, 0, dir.offsetZ));

                    if (b instanceof FoundryTankBlockEntity acc) {
                        if (acc.type == null || acc.type == this.type || acc.amount == 0) {
                            acc.type = this.type;
                            if (rand.nextInt(5) == 0) {
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
    }

    @Override
    public int getCapacity() {
        return MaterialShapes.BLOCK.q(4);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("nextUpdate", nextUpdate);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        nextUpdate = tag.getInt("nextUpdate");
    }
}
