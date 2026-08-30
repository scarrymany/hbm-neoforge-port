package com.hbm.blocks.generic;

import com.hbm.damage.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Damage-on-touch fence, ported from CE's {@code BarbedWire}. CE registers six distinct blocks
 * sharing one class and dispatching effect behavior via {@code this == ModBlocks.barbed_wire_x}
 * identity checks; the port instead flattens that dispatch into an explicit {@link Type} passed to
 * the constructor, one instance per CE registry entry (matching the port's metadata/variant
 * flattening convention). Uses {@link ModDamageTypes} in place of CE's {@code ModDamageSource}.
 */
public class BarbedWire extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public enum Type {
        STANDARD, FIRE, POISON, ACID, WITHER, ULTRADEATH
    }

    private final Type type;

    public BarbedWire(Properties properties, Type type) {
        super(properties);
        this.type = type;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        DamageSource cactusLike = level.damageSources().cactus();

        switch (type) {
            case STANDARD -> entity.hurt(cactusLike, 2.0F);
            case FIRE -> {
                entity.hurt(cactusLike, 2.0F);
                entity.igniteForSeconds(1);
            }
            case POISON -> {
                entity.hurt(cactusLike, 2.0F);
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 2));
                }
            }
            case ACID -> {
                entity.hurt(cactusLike, 2.0F);
                // CE also calls ArmorUtil.damageSuit(player, slot, 1) for each armor slot here;
                // that helper has no port equivalent yet (items-area concern), so acid contact
                // does not degrade worn armor until it lands.
            }
            case WITHER -> {
                entity.hurt(cactusLike, 2.0F);
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 5 * 20, 4));
                }
            }
            case ULTRADEATH -> {
                entity.hurt(level.damageSources().source(ModDamageTypes.PC), 5.0F);
                if (entity instanceof LivingEntity living) {
                    // HbmPotion.radiation has no port equivalent yet - the vanilla stand-in below
                    // preserves the "you touched the deadly wire" consequence without inventing a
                    // radiation potion effect that doesn't exist in this port.
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 5 * 20, 9));
                }
            }
        }
    }
}
