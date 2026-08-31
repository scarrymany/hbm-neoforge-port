package com.hbm.blocks.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.List;

/**
 * CE {@code RailGeneric} ({@code ModBlocks.java}:838-839). {@code rail_wood} uses
 * {@code setMaxSpeed(0.2F)}; {@code rail_narrow} keeps the 0.4F vanilla default
 * ({@code RailGeneric.java}:26-27, 66-68).
 */
public class RailGeneric extends BaseRailBlock {

    public static final MapCodec<RailGeneric> CODEC = simpleCodec(RailGeneric::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;
    private static final float BASE_SPEED = 0.4F;

    private final float maxSpeed;

    public RailGeneric(Properties properties) {
        this(properties, BASE_SPEED);
    }

    public RailGeneric(Properties properties, float maxSpeed) {
        super(false, properties);
        this.maxSpeed = maxSpeed;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return CODEC;
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, WATERLOGGED);
    }

    @Override
    public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return maxSpeed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        float ratio = maxSpeed / BASE_SPEED;
        if (ratio != 1.0F) {
            ChatFormatting color = ratio > 1.0F ? ChatFormatting.BLUE : ChatFormatting.RED;
            tooltip.add(Component.literal("Speed: " + ((int) (ratio * 100)) + "%").withStyle(color));
        }
    }
}
