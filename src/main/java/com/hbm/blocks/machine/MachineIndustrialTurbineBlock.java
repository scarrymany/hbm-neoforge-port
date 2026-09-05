package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.MachineIndustrialTurbineBlockEntity;
import com.hbm.blockentity.machine.PowerGenBlockEntities;
import com.hbm.blockentity.machine.TurbineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import java.util.Locale;

/**
 * Ported from CE's {@code MachineIndustrialTurbine} (regname {@code machine_industrial_turbine}).
 * No GUI, no inventory in CE either - confirmed by source (implements neither {@code IGUIProvider}
 * nor holds an {@code ItemStackHandler}), see {@link MachineIndustrialTurbineBlockEntity}'s javadoc.
 * Compressor lever Exact CE {@code MachineIndustrialTurbine.java:53-78}
 * ({@code chungus_lever} 1.5F/1.0F BLOCKS when {@code !operational}).
 * fillSpace extras Exact CE {@code :85-99}. printHook Exact CE {@code :110-138}.
 */
public class MachineIndustrialTurbineBlock extends BlockDummyable implements ILookOverlay {

    public MachineIndustrialTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 3, 3, 1, 1};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    /**
     * Exact CE {@code MachineIndustrialTurbine.fillSpace} extras ({@code MachineIndustrialTurbine.java:85-99}).
     * After {@code super.fillSpace}: add {@code dir * o} (core), {@code rot = dir} clockwise around Y.
     * Front pair at {@code dir*3 ± rot} (y), rear pair at {@code -dir ± rot} (y), tops at
     * {@code dir*3 y+2} / {@code -dir y+2}, plus {@code -dir*3 y+1}. No ProxyCombo TE — extras are
     * {@code makeExtra} flags only.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(dir, 3).relative(rot));
        makeExtra(level, core.relative(dir, 3).relative(rot.getOpposite()));
        makeExtra(level, core.relative(dir.getOpposite()).relative(rot));
        makeExtra(level, core.relative(dir.getOpposite()).relative(rot.getOpposite()));
        makeExtra(level, core.relative(dir, 3).above(2));
        makeExtra(level, core.relative(dir.getOpposite()).above(2));
        makeExtra(level, core.relative(dir.getOpposite(), 3).above());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new MachineIndustrialTurbineBlockEntity(PowerGenBlockEntities.INDUSTRIAL_TURBINE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PowerGenBlockEntities.INDUSTRIAL_TURBINE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof TurbineBaseBlockEntity entity)) {
            return InteractionResult.PASS;
        }

        Direction dir = Direction.from3DDataValue(level.getBlockState(core).getValue(META) - offset);
        if (pos.getX() == core.getX() + dir.getStepX() * 3
                && pos.getZ() == core.getZ() + dir.getStepZ() * 3
                && pos.getY() == core.getY() + 1) {
            if (!level.isClientSide) {
                if (!entity.operational) {
                    // Exact CE MachineIndustrialTurbine.java:66-68
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            HBMSoundHandler.chungus_lever.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    entity.onLeverPull();
                } else {
                    player.displayClientMessage(Component.literal("Cannot change compressor setting while operational!")
                            .withStyle(ChatFormatting.RED), false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** CE {@code MachineIndustrialTurbine.java:107} — RHS quarter blocks break FontRenderer; same cheat. */
    private static final String[] SPIN_BLOCKS = {"▖ ", "▘ ", " ▘", " ▖"};

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineIndustrialTurbine.java:110-138
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof MachineIndustrialTurbineBlockEntity chungus)) return;

        List<Component> text = new ArrayList<>();
        FluidTankNTM tankInput = chungus.tanks[0];
        FluidTankNTM tankOutput = chungus.tanks[1];
        FluidType inputType = tankInput.getTankType();
        FluidType outputType = Fluids.NONE;
        if (inputType.hasTrait(FT_Coolable.class)) {
            outputType = inputType.getTrait(FT_Coolable.class).coolsTo;
        }

        // CE &[rgb&] default + §r on the spinner — RGB is red→green by spin (ILookOverlay.java:38-44)
        int color = ((int) (0xFF - 0xFF * chungus.spin)) << 16 | ((int) (0xFF * chungus.spin) << 8);
        int time = (int) ((world.getGameTime() / 4) % 4);
        String anim = SPIN_BLOCKS[chungus.powerBuffer <= 0 ? 0 : time];

        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(inputType.getLocalizedName())
                        .append(Component.literal(": "
                                + String.format(Locale.US, "%,d", tankInput.getFill())
                                + "/"
                                + String.format(Locale.US, "%,d", tankInput.getMaxFill())
                                + "mB"))));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.empty().withStyle(ChatFormatting.RESET)
                        .append(outputType.getLocalizedName())
                        .append(Component.literal(": "
                                + String.format(Locale.US, "%,d", tankOutput.getFill())
                                + "/"
                                + String.format(Locale.US, "%,d", tankOutput.getMaxFill())
                                + "mB"))));
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.literal(BobMathUtil.getShortNumber(chungus.powerBuffer) + "HE (")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(anim + (int) Math.round(chungus.spin * 100) + "%")
                        .withColor(color))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE)));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
