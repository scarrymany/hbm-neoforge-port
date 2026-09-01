package com.hbm.blocks.machine.dummyable;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.block.IToolable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineStrandCasterBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code MachineStrandCaster}: Dummyable {0,0,6,0,1,0} offset 0 + extra {2,0,1,0,1,0}.
 * TODO(CE: MachineStrandCaster.java:60): TileEntityProxyCombo(true,false,true).moltenMetal()
 * on extras — ProxyCombo not ported. Pour accepted on META≥6 extras instead.
 * TODO(CE: RenderStrandCaster.java:22): TESR.
 */
public class MachineStrandCasterBlock extends BlockDummyable implements ICrucibleAcceptor, ILookOverlay, IToolable {

    public MachineStrandCasterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 6, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineStrandCasterBlockEntity(DummyableProcessBlockEntities.MACHINE_STRAND_CASTER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_STRAND_CASTER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        if (!MultiblockHandlerXR.checkSpace(level, core, getDimensions(), placedPos, dir)) return false;
        return MultiblockHandlerXR.checkSpace(level, core, new int[]{2, 0, 1, 0, 1, 0}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{2, 0, 1, 0, 1, 0}, this, dir);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(rot).relative(dir, -1));
        makeExtra(level, core.relative(dir, -1));
        makeExtra(level, core.relative(dir, -5));
        makeExtra(level, core.relative(rot).relative(dir, -5));
        makeExtra(level, core.relative(rot).relative(dir, -1).above(2));
        makeExtra(level, core.relative(dir, -1).above(2));
        makeExtra(level, core.relative(rot).above(2));
        makeExtra(level, core.above(2));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof MachineStrandCasterBlockEntity caster)) {
            return ItemInteractionResult.FAIL;
        }
        if (!stack.isEmpty() && stack.getItem() instanceof ItemMold && caster.inventory.getStackInSlot(0).isEmpty()) {
            ItemStack put = stack.copy();
            put.setCount(1);
            caster.inventory.setStackInSlot(0, put);
            stack.shrink(1);
            level.playSound(null, pos, HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            caster.setChanged();
            return ItemInteractionResult.CONSUME;
        }
        if (!stack.isEmpty() && stack.is(ItemTags.SHOVELS)) {
            if (caster.amount > 0 && caster.type != null) {
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(caster.type, caster.amount), false);
                if (!player.getInventory().add(scrap)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, scrap));
                }
                caster.amount = 0;
                caster.type = null;
                caster.setChanged();
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MachineStrandCasterBlockEntity caster
                && caster.amount > 0 && caster.type != null) {
            ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(caster.type, caster.amount), false);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, scrap));
            caster.amount = 0;
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        if (level.getBlockState(pos).getValue(META) < 6) return false;
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof MachineStrandCasterBlockEntity caster
                && caster.canAcceptPartialPour(level, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level level, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        if (level.getBlockState(pos).getValue(META) < 6) return stack;
        BlockPos core = findCore(level, pos);
        if (core == null) return stack;
        return level.getBlockEntity(core) instanceof MachineStrandCasterBlockEntity caster
                ? caster.pour(level, pos, dX, dY, dZ, side, stack)
                : stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return false;
    }

    @Override
    public Mats.MaterialStack flow(Level level, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        return null;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (world.isClientSide) return true;
        BlockPos core = findCore(world, new BlockPos(x, y, z));
        if (core == null || !(world.getBlockEntity(core) instanceof MachineStrandCasterBlockEntity caster)) return false;
        ItemStack mold = caster.inventory.getStackInSlot(0);
        if (mold.isEmpty()) return false;
        if (!player.getInventory().add(mold.copy())) {
            world.addFreshEntity(new ItemEntity(world, x + 0.5, y + 1, z + 0.5, mold.copy()));
        }
        caster.inventory.setStackInSlot(0, ItemStack.EMPTY);
        caster.setChanged();
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getBlockEntity(core) instanceof MachineStrandCasterBlockEntity caster)) return;
        List<Component> text = new ArrayList<>();
        ItemStack mold = caster.inventory.getStackInSlot(0);
        if (mold.isEmpty()) {
            text.add(Component.translatable("foundry.noCast").withStyle(ChatFormatting.RED));
        } else if (mold.getItem() instanceof ItemMold) {
            ItemMold.MoldEntry entry = ItemMold.getMold(mold);
            text.add(Component.literal(entry.getTitle()).withStyle(ChatFormatting.BLUE));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xFF4000, 0x401000, text);
    }
}
