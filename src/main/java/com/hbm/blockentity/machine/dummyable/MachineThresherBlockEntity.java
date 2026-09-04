package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.PlantEnums.EnumTallPlantType;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.machine.MachineThresherBlock;
import com.hbm.damage.ModDamageTypes;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineThresher}. Arm sweep / tall-plant / cane / crop / entity Exact CE
 * {@code TileEntityMachineThresher.java:101-204} + helpers {@code :289-402}.
 * Audio / TESR / AuxParticle burst skipped (polish). {@code nitra_small} not registered.
 */
public class MachineThresherBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardReceiverMK2, ITickableBE {

    public final FluidTankNTM tank;
    public boolean isOn;
    public boolean isSuspended;
    public int delay;
    public float angle;
    /** 0 waiting, 1 extending, 2 retracting — CE :64. */
    private int state;

    public MachineThresherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, true, false);
        this.tank = new FluidTankNTM(Fluids.WOODOIL, 100).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_thresher");
    }

    private Direction getDir() {
        // CE :75-77 — opposite of FACING meta
        Direction facing = getBlockState().hasProperty(MachineThresherBlock.FACING)
                ? getBlockState().getValue(MachineThresherBlock.FACING) : Direction.NORTH;
        return facing.getOpposite();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        Direction dir = getDir();
        Direction rot = dir.getClockWise();

        if (!isSuspended && level.getGameTime() % 20 == 0) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                isOn = true;
            } else {
                isOn = false;
            }
            trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(rot), rot));
            trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.relative(rot.getOpposite()), rot.getOpposite()));
            trySubscribe(tank.getTankType(), level, new DirPos(worldPosition.below(), Direction.DOWN));
        }

        if (isOn && !isSuspended) {
            sweepArm(dir, rot);
        }

        dataChanged();
        networkPackMK2(100);
    }

    /** CE :101-204 — articulated arm + hit strip + living shred. */
    private void sweepArm(Direction dir, Direction rot) {
        if (state == 0) {
            delay--;
            if (delay <= 0) state = 1;
        }

        if (state == 1) {
            angle += 82.5F / 60F;
            if (angle >= 82.5F) {
                angle = 82.5F;
                state = 2;
            }
        } else if (state == 2) {
            angle -= 82.5F / 60F;
            if (angle <= 0F) {
                angle = 0F;
                state = 0;
                delay = 200 + level.random.nextInt(100);
            }
        }

        if (angle == 0) return;

        double pivotX = worldPosition.getX() + 0.5 - dir.getStepX();
        double pivotZ = worldPosition.getZ() + 0.5 - dir.getStepZ();

        Vec3 upperArm = new Vec3(-dir.getStepX() * 4, 0, -dir.getStepZ() * 4);
        Vec3 lowerArm = new Vec3(-dir.getStepX() * 4, 0, -dir.getStepZ() * 4);
        float rad = (float) Math.toRadians(82.5 - angle);

        if (dir.getStepZ() != 0) {
            upperArm = upperArm.xRot(rad);
            lowerArm = lowerArm.xRot(-rad);
        }
        if (dir.getStepX() != 0) {
            upperArm = rotateAroundZ(upperArm, rad);
            lowerArm = rotateAroundZ(lowerArm, -rad);
        }

        double endX = pivotX + upperArm.x + lowerArm.x + (-dir.getStepX() * 2);
        double endZ = pivotZ + upperArm.z + lowerArm.z + (-dir.getStepZ() * 2);

        for (int i = -3; i <= 3; i++) {
            BlockPos hit = new BlockPos(
                    Mth.floor(endX + rot.getStepX() * i),
                    worldPosition.getY(),
                    Mth.floor(endZ + rot.getStepZ() * i));
            BlockState hitState = level.getBlockState(hit);
            Block b = hitState.getBlock();

            if (hitState.isCollisionShapeFullBlock(level, hit) && !canCut(b)) {
                state = 2;
                break;
            }

            if (b == Blocks.SUNFLOWER) {
                if (level.random.nextInt(250) == 0) {
                    level.levelEvent(2001, hit, Block.getId(hitState));
                    dropItem(new ItemStack(Blocks.SUNFLOWER));
                }
                continue;
            }
            if (b == Blocks.TALL_GRASS) {
                if (level.random.nextInt(100) == 0) {
                    level.levelEvent(2001, hit, Block.getId(hitState));
                    dropItem(new ItemStack(Items.WHEAT_SEEDS));
                }
                continue;
            }

            if (b instanceof BlockTallPlant tall) {
                cutTallPlant(tall, hit);
                continue;
            }

            if (b == Blocks.SUGAR_CANE || b == Blocks.CACTUS) {
                cutCane(b, hit);
                continue;
            }

            if (canCut(b) && !shouldIgnore(hit, hitState, b)) {
                cutCrop(b, hitState, hit);
            }
        }

        AABB box = new AABB(endX, worldPosition.getY() + 0.5, endZ, endX, worldPosition.getY() + 0.5, endZ)
                .inflate(Math.abs(dir.getStepX() * 0.5) + Math.abs(rot.getStepX() * 4.5), 0.5,
                        Math.abs(dir.getStepZ() * 0.5) + Math.abs(rot.getStepZ() * 4.5));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e.isAlive() && e.hurt(e.damageSources().source(ModDamageTypes.BLENDER), 100)) {
                // TODO(CE: TileEntityMachineThresher.java:192): nitra_small not registered
                level.playSound(null, e.getX(), e.getY(), e.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS,
                        2.0F, 0.95F + level.random.nextFloat() * 0.2F);
            }
        }
    }

    private static Vec3 rotateAroundZ(Vec3 vec, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        return new Vec3(vec.x * cos + vec.y * sin, vec.y * cos - vec.x * sin, vec.z);
    }

    public static boolean canCut(Block b) {
        if (b instanceof BonemealableBlock) return true;
        if (b == Blocks.NETHER_WART) return true;
        return b == Blocks.MELON || b == Blocks.PUMPKIN;
    }

    public boolean shouldIgnore(BlockPos pos, BlockState state, Block b) {
        if (b instanceof StemBlock) return true;
        if (b == Blocks.NETHER_WART) return state.getValue(NetherWartBlock.AGE) < 3;
        if (b instanceof BonemealableBlock grow) {
            return grow.isValidBonemealTarget(level, pos, state);
        }
        return false;
    }

    /** CE :308-329 — lower half redirects up; skip immature willow CD2/CD3. */
    protected void cutTallPlant(BlockTallPlant plant, BlockPos pos) {
        EnumTallPlantType type = plant.type;
        if (type.name().endsWith("_LOWER")) {
            pos = pos.above();
            BlockState above = level.getBlockState(pos);
            if (!(above.getBlock() instanceof BlockTallPlant up) || up.type != pairedUpper(type)) return;
            type = up.type;
        }
        if (type == EnumTallPlantType.MUSTARD_WILLOW_2_UPPER || type == EnumTallPlantType.MUSTARD_WILLOW_3_UPPER) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        level.levelEvent(2001, pos, Block.getId(state));
        if (level instanceof ServerLevel server) {
            for (ItemStack drop : Block.getDrops(state, server, pos, null)) {
                dropItem(drop);
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static EnumTallPlantType pairedUpper(EnumTallPlantType lower) {
        return EnumTallPlantType.valueOf(lower.name().replace("_LOWER", "_UPPER"));
    }

    /** CE :332-350 — top two of a three-block cane/cactus. */
    protected void cutCane(Block target, BlockPos pos) {
        int offset = level.getBlockState(pos.below()).is(target) ? -1 : 0;
        for (int i = 2 + offset; i > offset; i--) {
            BlockPos target2 = pos.above(i);
            BlockState state = level.getBlockState(target2);
            if (!state.is(target)) continue;
            level.levelEvent(2001, target2, Block.getId(state));
            if (level instanceof ServerLevel server) {
                for (ItemStack drop : Block.getDrops(state, server, target2, null)) {
                    dropItem(drop);
                }
            }
            level.setBlock(target2, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** CE :353-388 — harvest + replant from seed drop. */
    protected void cutCrop(Block b, BlockState state, BlockPos pos) {
        level.levelEvent(2001, pos, Block.getId(state));
        BlockState replacement = Blocks.AIR.defaultBlockState();
        if (level instanceof ServerLevel server) {
            boolean replanted = false;
            for (ItemStack drop : Block.getDrops(state, server, pos, null)) {
                if (!replanted && drop.getItem() instanceof BlockItem bi) {
                    BlockState plant = bi.getBlock().defaultBlockState();
                    if (plant.canSurvive(level, pos)) {
                        replacement = plant;
                        replanted = true;
                        drop.shrink(1);
                    }
                }
                if (!drop.isEmpty()) dropItem(drop);
            }
            if (b == Blocks.WHEAT && !replanted) {
                replacement = b.defaultBlockState();
            }
        }
        level.setBlock(pos, replacement, 3);
    }

    /** CE :390-402. */
    protected void dropItem(ItemStack drop) {
        if (drop.isEmpty() || level == null) return;
        Direction dir = getDir().getOpposite();
        double spawnX = worldPosition.getX() + 0.5 - dir.getStepX() * 0.75;
        double spawnZ = worldPosition.getZ() + 0.5 - dir.getStepZ() * 0.75;
        ItemEntity entity = new ItemEntity(level, spawnX, worldPosition.getY(), spawnZ, drop);
        entity.setPickUpDelay(10);
        entity.setDeltaMovement(dir.getStepX() * -0.2 + 0.2, 0, dir.getStepZ() * -0.2);
        level.addFreshEntity(entity);
    }

    public void toggleSuspended() {
        isSuspended = !isSuspended;
        setChanged();
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("suspended", isSuspended);
        tag.putFloat("angle", angle);
        tag.putInt("state", state);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isOn = tag.getBoolean("isOn");
        isSuspended = tag.getBoolean("suspended");
        angle = tag.getFloat("angle");
        state = tag.getInt("state");
        tank.readFromNBT(tag, "tank");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(isSuspended);
        buf.writeFloat(angle);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        isSuspended = buf.readBoolean();
        angle = buf.readFloat();
        tank.deserialize(buf);
    }
}
