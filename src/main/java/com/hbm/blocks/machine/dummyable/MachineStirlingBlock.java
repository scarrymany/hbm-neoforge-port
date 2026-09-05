package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineStirlingBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.items.machine.ItemGear;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code MachineStirling} — Dummyable {1,0,1,1,1,1} offset 1 + 4 extras. Shared by ×3 ids.
 * Cog install {@code upgradePlug} Exact CE {@code MachineStirling.java:97} (1.5F/0.75F PLAYERS).
 * printHook Exact CE {@code :149-185} (creative skips percent/overspeed/gear).
 * Tooltip Exact CE {@code :189-191} via existing {@code block.hbm.machine_stirling*.desc}.
 */
public class MachineStirlingBlock extends BlockDummyable implements ILookOverlay, ITooltipProvider {

    public MachineStirlingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineStirlingBlockEntity(DummyableProcessBlockEntities.MACHINE_STIRLING.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_STIRLING.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ItemGear) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof MachineStirlingBlockEntity stirling) {
                    if (stirling.tryInstallCog(stack)) {
                        if (!player.getAbilities().instabuild) stack.shrink(1);
                        // CE MachineStirling.java:97
                        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                HBMSoundHandler.upgradePlug.get(), SoundSource.PLAYERS, 1.5F, 0.75F);
                        return ItemInteractionResult.SUCCESS;
                    }
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        makeExtra(level, core.east());
        makeExtra(level, core.west());
        makeExtra(level, core.north());
        makeExtra(level, core.south());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineStirling.java:149-185
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof MachineStirlingBlockEntity stirling)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(stirling.heat + "TU/t"));
        text.add(Component.literal((stirling.hasCog ? stirling.powerBuffer : 0) + "HE/t"));

        if (this != DummyableProcessBlocks.MACHINE_STIRLING_CREATIVE.get()) {
            int maxHeat = stirling.maxHeat();
            double percent = (double) stirling.heat / (double) maxHeat;
            int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
            if (percent > 1D) color = 0xff0000;

            text.add(Component.literal(((stirling.heat * 1000 / maxHeat) / 10D) + "%").withColor(color));

            if (stirling.heat > maxHeat) {
                text.add(Component.literal("! ! ! OVERSPEED ! ! !")
                        .withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00));
            }

            if (!stirling.hasCog) {
                text.add(Component.literal("Gear missing!").withColor(0xff0000));
            }
        }

        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE MachineStirling.java:189-191 — addStandardInfo via existing block.hbm.machine_stirling*.desc
        addStandardInfo(tooltip);
    }
}
