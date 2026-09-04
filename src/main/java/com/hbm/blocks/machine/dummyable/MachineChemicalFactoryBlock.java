package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineChemicalFactoryBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.lib.DirPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
 * CE {@code MachineChemicalFactory} — Dummyable {2,0,2,2,2,2} offset 2. ≠ {@code machine_chemical_plant}.
 * TODO(CE: MachineChemicalFactory.java:36): TileEntityProxyDyn().inventory().power().fluid() on META≥6.
 * ILookOverlay cool/IO: CE {@code MachineChemicalFactory.java:72-101}.
 * TODO(CE: RenderChemicalFactory.java:1): OBJ TESR.
 */
public class MachineChemicalFactoryBlock extends BlockDummyable implements ITooltipProvider, ILookOverlay {

    public MachineChemicalFactoryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineChemicalFactoryBlockEntity(DummyableProcessBlockEntities.MACHINE_CHEMICAL_FACTORY.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_CHEMICAL_FACTORY.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        FactoryDummyablePorts.fillFactoryExtras(this, level, placedPos, dir, placementOffset);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        this.addStandardInfo(tooltip);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // CE MachineChemicalFactory.java:72-101
        BlockPos corePos = findCore(world, pos);
        if (corePos == null) return;
        if (!(world.getBlockEntity(corePos) instanceof MachineChemicalFactoryBlockEntity chemfac)) return;

        for (DirPos dirPos : chemfac.getCoolPos()) {
            if (dirPos.compare(pos.getX() + dirPos.getDir().getStepX(), pos.getY(), pos.getZ() + dirPos.getDir().getStepZ())) {
                List<Component> text = new ArrayList<>();
                text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                        .append(chemfac.water.getTankType().getLocalizedName().copy().withStyle(ChatFormatting.RESET)));
                text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                        .append(chemfac.lps.getTankType().getLocalizedName().copy().withStyle(ChatFormatting.RESET)));
                ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
                break;
            }
        }

        DirPos[] io = chemfac.getIOPos();
        for (int i = 0; i < io.length; i++) {
            DirPos port = io[i];
            if (port.compare(pos.getX() + port.getDir().getStepX(), pos.getY(), pos.getZ() + port.getDir().getStepZ())) {
                List<Component> text = new ArrayList<>();
                text.add(Component.literal("-> ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("Recipe field [" + (i + 1) + "]").withStyle(ChatFormatting.RESET)));
                ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
                break;
            }
        }
    }
}
