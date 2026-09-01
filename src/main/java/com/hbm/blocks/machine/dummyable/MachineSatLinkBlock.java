package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineSatLinkBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.items.ISatChip;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
 * CE {@code MachineSatLink}: Dummyable {6,0,1,0,1,0} offset 0. No GUI.
 * TODO(CE: MachineSatLink.java:41): TileEntityProxyCombo on extras.
 * TODO(CE: RenderSatLink.java:16): TESR.
 */
public class MachineSatLinkBlock extends BlockDummyable implements ILookOverlay {

    public MachineSatLinkBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{6, 0, 1, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineSatLinkBlockEntity(DummyableProcessBlockEntities.MACHINE_SATLINK.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_SATLINK.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getCounterClockWise();
        makeExtra(level, core.relative(dir.getOpposite()));
        makeExtra(level, core.relative(rot));
        makeExtra(level, core.relative(dir.getOpposite()).relative(rot));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.isEmpty() || !(stack.getItem() instanceof ISatChip)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof MachineSatLinkBlockEntity link)) {
            return ItemInteractionResult.FAIL;
        }
        link.freq = ISatChip.getFreqS(stack);
        player.displayClientMessage(Component.literal("Set frequency to " + link.freq)
                .withStyle(ChatFormatting.YELLOW), false);
        level.playSound(null, pos, HBMSoundHandler.techBleep.get(), SoundSource.BLOCKS, 1F, 1F);
        link.setChanged();
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getBlockEntity(core) instanceof MachineSatLinkBlockEntity link)) return;
        List<Component> text = new ArrayList<>();
        text.add(Component.translatable("tile.machine_satlink.freq").append(Component.literal(": " + link.freq)));
        text.add(Component.translatable("tile.machine_satlink.connected").append(Component.literal(": "))
                .append(Component.translatable(link.connected ? "tile.machine_satlink.yes" : "tile.machine_satlink.no")
                        .withStyle(link.connected ? ChatFormatting.GREEN : ChatFormatting.RED)));
        for (Component comp : link.info) {
            if (comp != null) text.add(comp);
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
