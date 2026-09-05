package com.hbm.blocks.generic;

import com.hbm.explosion.ExplosionThermo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * CE {@code blocks/generic/RedBarrel.java}. Kind is constructor-baked because 1.21 has one class
 * instance per registry id (CE compared {@code this == ModBlocks.red_barrel}).
 * <p>
 * {@code BlockTaint} scatter (taint) stays skipped — taint block is not registered.
 */
public class RedBarrel extends BaseBarrel {

    public enum Kind {
        RED,
        PINK,
        LOX,
        TAINT
    }

    // MrNorwood (CE): prevent infinite recursion. Limit 100 chained explosions.
    private int explosionCount = 0;
    private static final int MAX_EXPLOSION_DEPTH = 100;

    private final Kind kind;

    public RedBarrel(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Static fluid barrel"));
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) return;
        if (explosionCount >= MAX_EXPLOSION_DEPTH) return;
        explosionCount++;
        server.getServer().execute(() -> {
            explode(level, pos.getX(), pos.getY(), pos.getZ());
            explosionCount--;
        });
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, movedByPiston);
        if (kind != Kind.RED && kind != Kind.PINK) return;
        if (level.isClientSide() || !(level instanceof ServerLevel server)) return;
        if (!adjacentFire(level, pos)) return;
        server.getServer().execute(() -> {
            explode(level, pos.getX(), pos.getY(), pos.getZ());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        });
    }

    public void explode(Level level, int x, int y, int z) {
        double cx = x + 0.5D;
        double cy = y + 0.5D;
        double cz = z + 0.5D;
        if (kind == Kind.RED || kind == Kind.PINK) {
            // CE: newExplosion(null, +0.5, 2.5F, flaming=true, smoking=true)
            level.explode(null, cx, cy, cz, 2.5F, true, Level.ExplosionInteraction.TNT);
        }
        if (kind == Kind.LOX) {
            // Exact CE RedBarrel.java:86-90
            level.explode(null, cx, cy, cz, 1.0F, false, Level.ExplosionInteraction.NONE);
            ExplosionThermo.freezer(level, x, y, z, 7);
        }
        if (kind == Kind.TAINT) {
            // CE: 1F, no fire/terrain, then 100-block BlockTaint scatter — taint block not ported
            level.explode(null, cx, cy, cz, 1.0F, false, Level.ExplosionInteraction.NONE);
        }
    }

    private static boolean adjacentFire(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.FIRE)) {
                return true;
            }
        }
        return false;
    }
}
