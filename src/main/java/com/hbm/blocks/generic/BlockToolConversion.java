package com.hbm.blocks.generic;

import com.hbm.api.block.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Right-click-with-tool block converter, ported from CE's {@code BlockToolConversion}
 * ({@code watz_casing}). CE's actual conversion lookup goes through {@code NTMToolHandler}/
 * {@code RecipesCommon}, neither of which exists in this port yet (a whole recipe-lookup framework,
 * not a simple utility - genuinely out of scope for "simple items and blocks"); {@link #onScrew}
 * therefore always returns {@code false} (no conversion available) until that framework lands. The
 * {@code TOOLED} on/off state itself - CE's actual metadata values 0/1 - is preserved as a real
 * {@code BooleanProperty} rather than two separate registry entries, since it is a runtime tool
 * interaction result, not a fixed content variant (same reasoning as {@code BlockRedBrick}'s
 * {@code FACING}).
 */
public class BlockToolConversion extends Block implements IToolable {

    public static final BooleanProperty TOOLED = BooleanProperty.create("tooled");

    public BlockToolConversion(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TOOLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOOLED);
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side,
                            float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        return false;
    }
}
