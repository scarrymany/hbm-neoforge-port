package com.hbm.handler.ability;

import com.hbm.config.ToolConfig;
import com.hbm.items.tool.ItemToolAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Area-of-effect tool abilities (vein miner, AoE hammer, explosive mining). Ported from CE's
 * {@code com.hbm.handler.ability.IToolAreaAbility}, retargeted at the 1.21 block/ray API (the
 * modern {@code Level.clip(ClipContext)} replaces CE's {@code World.rayTraceBlocks}, confirmed
 * against the real 1.21.1 API).
 *
 * <p>{@link #EXPLOSION} deviates from CE: CE's handler drives a custom {@code ExplosionNT} (block
 * allocator/processor pipeline with an "all-drop, no-hurt, no-particle" attribute set) which does
 * not exist anywhere in this port yet - it is a whole explosion subsystem, not a small helper, and
 * porting it is out of scope for the mining-tool ability framework. This port instead triggers one
 * plain vanilla {@link Level#explode} at the same strength, which breaks and drops blocks but
 * without CE's guaranteed-all-drops/no-hurt/no-particle guarantees. Noted as an open item for
 * whichever phase ports the custom explosion pipeline.
 */
public interface IToolAreaAbility extends IBaseAbility {

    /**
     * Should call {@code tool.breakExtraBlock} on a bunch of blocks. The initial block is
     * implicitly broken by the caller, so don't call breakExtraBlock on it. Returning true skips
     * the reference block from being broken by the normal harvest path.
     */
    boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool);

    /** Whether breakExtraBlock is called at all. Currently only false for explosion. */
    default boolean allowsHarvest(int level) {
        return true;
    }

    int SORT_ORDER_BASE = 0;

    IToolAreaAbility NONE = new IToolAreaAbility() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE;
        }

        @Override
        public boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool) {
            return false;
        }
    };

    IToolAreaAbility RECURSION = new IToolAreaAbility() {
        @Override
        public String getName() {
            return "tool.ability.recursion";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_VEIN.get();
        }

        private final int[] radiusAtLevel = { 3, 4, 5, 6, 7, 9, 10 };

        @Override
        public int levels() {
            return radiusAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + radiusAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 1;
        }

        private final Set<BlockPos> visited = new HashSet<>();

        private final List<BlockPos> offsets = buildOffsets();

        private List<BlockPos> buildOffsets() {
            List<BlockPos> list = new ArrayList<>(26);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dy != 0 || dz != 0) {
                            list.add(new BlockPos(dx, dy, dz));
                        }
                    }
                }
            }
            return list;
        }

        @Override
        public boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool) {
            BlockState state = world.getBlockState(pos);

            if (state.is(Blocks.STONE) && !ToolConfig.RECURSIVE_STONE.get()) {
                return false;
            }

            if (state.is(Blocks.NETHERRACK) && !ToolConfig.RECURSIVE_NETHERRACK.get()) {
                return false;
            }

            visited.clear();

            recurse(world, pos, pos, player, tool, 0, radiusAtLevel[level]);

            return false;
        }

        private void recurse(Level world, BlockPos pos, BlockPos ref, Player player, ItemToolAbility tool, int depth, int radius) {
            List<BlockPos> shuffled = new ArrayList<>(offsets);
            Collections.shuffle(shuffled);

            for (BlockPos offset : shuffled) {
                breakExtra(world, pos.offset(offset), ref, player, tool, depth, radius);
            }
        }

        private void breakExtra(Level world, BlockPos pos, BlockPos ref, Player player, ItemToolAbility tool, int depth, int radius) {
            if (!visited.add(pos)) {
                return;
            }

            depth += 1;

            if (depth > ToolConfig.RECURSION_DEPTH.get()) {
                return;
            }

            if (pos.equals(ref)) {
                return;
            }

            Vec3 delta = new Vec3(pos.getX() - ref.getX(), pos.getY() - ref.getY(), pos.getZ() - ref.getZ());
            if (delta.length() > radius) {
                return;
            }

            BlockState state = world.getBlockState(pos);
            BlockState refState = world.getBlockState(ref);

            if (!isSameBlock(state, refState)) {
                return;
            }

            if (player.getMainHandItem().isEmpty()) {
                return;
            }

            tool.breakExtraBlock(world, pos, player, ref);

            recurse(world, pos, ref, player, tool, depth, radius);
        }

        private boolean isSameBlock(BlockState a, BlockState b) {
            return a.is(b.getBlock());
        }
    };

    IToolAreaAbility HAMMER = new IToolAreaAbility() {
        @Override
        public String getName() {
            return "tool.ability.hammer";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_HAMMER.get();
        }

        private final int[] rangeAtLevel = { 1, 2, 3, 4 };

        @Override
        public int levels() {
            return rangeAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + rangeAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 2;
        }

        @Override
        public boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool) {
            int range = rangeAtLevel[level];
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            for (int a = x - range; a <= x + range; a++) {
                for (int b = y - range; b <= y + range; b++) {
                    for (int c = z - range; c <= z + range; c++) {
                        if (a == x && b == y && c == z) continue;

                        tool.breakExtraBlock(world, new BlockPos(a, b, c), player, pos);
                    }
                }
            }

            return false;
        }
    };

    IToolAreaAbility HAMMER_FLAT = new IToolAreaAbility() {
        @Override
        public String getName() {
            return "tool.ability.hammer_flat";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_HAMMER.get();
        }

        private final int[] rangeAtLevel = { 1, 2, 3, 4 };

        @Override
        public int levels() {
            return rangeAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + rangeAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 3;
        }

        @Override
        public boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool) {
            int range = rangeAtLevel[level];

            HitResult hit = raytraceFromEntity(world, player, 4.5D);
            if (hit.getType() != HitResult.Type.BLOCK) {
                return true;
            }

            Direction sideHit = ((BlockHitResult) hit).getDirection();

            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            int xRange = range;
            int yRange = range;
            int zRange;
            switch (sideHit) {
                case DOWN, UP -> {
                    yRange = 0;
                    zRange = range;
                }
                case NORTH, SOUTH -> zRange = 0;
                default -> {
                    xRange = 0;
                    zRange = range;
                }
            }

            for (int a = x - xRange; a <= x + xRange; a++) {
                for (int b = y - yRange; b <= y + yRange; b++) {
                    for (int c = z - zRange; c <= z + zRange; c++) {
                        if (a == x && b == y && c == z) continue;

                        tool.breakExtraBlock(world, new BlockPos(a, b, c), player, pos);
                    }
                }
            }

            return false;
        }

        private HitResult raytraceFromEntity(Level world, Player player, double range) {
            Vec3 eye = player.getEyePosition(1.0F);
            Vec3 look = player.getViewVector(1.0F);
            Vec3 end = eye.add(look.scale(range));
            ClipContext context = new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            return world.clip(context);
        }
    };

    IToolAreaAbility EXPLOSION = new IToolAreaAbility() {
        @Override
        public String getName() {
            return "tool.ability.explosion";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_EXPLOSION.get();
        }

        private final float[] strengthAtLevel = { 2.5F, 5F, 10F, 15F };

        @Override
        public int levels() {
            return strengthAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + strengthAtLevel[level] + ")";
        }

        @Override
        public boolean allowsHarvest(int level) {
            return false;
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 4;
        }

        @Override
        public boolean onDig(int level, Level world, BlockPos pos, Player player, ItemToolAbility tool) {
            float strength = strengthAtLevel[level];

            // CE drives a custom ExplosionNT pipeline (all-drop/no-hurt/no-particle) here; that
            // subsystem isn't part of this port yet, see class javadoc.
            world.explode(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, strength * 0.2F, Level.ExplosionInteraction.BLOCK);

            return true;
        }
    };

    IToolAreaAbility[] abilities = { NONE, RECURSION, HAMMER, HAMMER_FLAT, EXPLOSION };

    static IToolAreaAbility getByName(String name) {
        for (IToolAreaAbility ability : abilities) {
            if (ability.getName().equals(name)) {
                return ability;
            }
        }

        return NONE;
    }
}
