package com.hbm.blocks.machine.dummyable;

import com.hbm.api.block.IToolable;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineConveyorPressBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.items.machine.ItemStamp;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code MachineConveyorPress} — Dummyable {2,0,0,0,0,0} offset 0.
 * Live I/O: {@code PressRecipes} on {@code EntityMovingItem} above the core + 1 stamp slot.
 * Stamp insert {@code upgradePlug} Exact CE {@code MachineConveyorPress.java:83} (1.0F/1.0F).
 */
public class MachineConveyorPressBlock extends BlockDummyable
        implements IConveyorBelt, IToolable, ILookOverlay, ITooltipProvider {

    public MachineConveyorPressBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineConveyorPressBlockEntity(DummyableProcessBlockEntities.MACHINE_CONVEYOR_PRESS.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_CONVEYOR_PRESS.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ItemStamp) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof MachineConveyorPressBlockEntity press
                        && press.inventory.getStackInSlot(0).isEmpty()) {
                    ItemStack stamp = stack.copy();
                    stamp.setCount(1);
                    press.inventory.setStackInSlot(0, stamp);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    // CE MachineConveyorPress.java:83
                    level.playSound(null, pos, HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    press.setChanged();
                    press.dataChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (world.isClientSide) return true;

        BlockPos core = findCore(world, new BlockPos(x, y, z));
        if (core == null) return false;
        if (!(world.getBlockEntity(core) instanceof MachineConveyorPressBlockEntity press)) return false;

        ItemStack stamp = press.inventory.getStackInSlot(0);
        if (stamp.isEmpty()) return false;

        if (!player.getInventory().add(stamp.copy())) {
            player.drop(stamp.copy(), false);
        }
        press.inventory.setStackInSlot(0, ItemStack.EMPTY);
        press.setChanged();
        press.dataChanged();
        return true;
    }

    private Direction getTravelDirection(Level world, int x, int y, int z) {
        BlockState below = world.getBlockState(new BlockPos(x, y - 1, z));
        if (below.getBlock() != this || !below.hasProperty(META) || below.getValue(META) < 12) {
            below = world.getBlockState(new BlockPos(x, y, z));
        }
        int meta = below.hasProperty(META) ? below.getValue(META) : 12;
        return Direction.from3DDataValue(meta - offset).getClockWise(Direction.Axis.Y);
    }

    @Override
    public boolean canItemStay(Level world, int x, int y, int z, Vec3 itemPos) {
        BlockState below = world.getBlockState(new BlockPos(x, y - 1, z));
        return below.getBlock() == this && below.hasProperty(META) && below.getValue(META) >= 12;
    }

    @Override
    public Vec3 getTravelLocation(Level world, int x, int y, int z, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(world, x, y, z);
        Vec3 snap = getClosestSnappingPosition(world, new BlockPos(x, y, z), itemPos);
        Vec3 dest = new Vec3(
                snap.x - dir.getStepX() * speed,
                snap.y - dir.getStepY() * speed,
                snap.z - dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(itemPos);
        double len = motion.length();
        if (len < 1.0e-6) {
            return new Vec3(
                    itemPos.x - dir.getStepX() * speed,
                    itemPos.y - dir.getStepY() * speed,
                    itemPos.z - dir.getStepZ() * speed);
        }
        return new Vec3(
                itemPos.x + motion.x / len * speed,
                itemPos.y + motion.y / len * speed,
                itemPos.z + motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(world, pos.getX(), pos.getY(), pos.getZ());
        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;
        if (dir.getStepX() != 0) posX = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1);
        if (dir.getStepZ() != 0) posZ = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1);
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        this.addStandardInfo(tooltip);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        if (!(findCoreBlockEntity(world, pos) instanceof MachineConveyorPressBlockEntity press)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(Library.getShortNumber(press.power) + "HE / "
                + Library.getShortNumber(MachineConveyorPressBlockEntity.MAX_POWER) + "HE"));
        ItemStack stamp = press.inventory.getStackInSlot(0);
        if (stamp.isEmpty()) stamp = press.syncStack;
        text.add(Component.literal("Installed stamp: ").append(
                stamp.isEmpty()
                        ? Component.literal("NONE").withStyle(ChatFormatting.RED)
                        : stamp.getHoverName()));

        ILookOverlay.printGeneric(event, Component.translatable(this.getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
