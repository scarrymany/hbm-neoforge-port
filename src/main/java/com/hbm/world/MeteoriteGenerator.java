package com.hbm.world;

import com.hbm.blocks.BlockEnums.EnumMeteorType;
import com.hbm.config.WorldConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.weapon.legacy.LegacyWeaponItems;
import com.hbm.world.feature.OreShapeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Ported from CE's {@code com.hbm.world.Meteorite} (625 lines, full read) - the shared block-shape
 * generator behind both the passive ambient world-gen feature
 * ({@link com.hbm.world.feature.MeteoriteFeature}, which always calls this with
 * {@code safe=false, allowSpecials=false, damagingImpact=false}, matching CE's own
 * {@code new Meteorite().generate(world, rand, x, y, z, false, false, false)} call exactly) and, per
 * docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 2a, a future live-strike {@code EntityMeteor}
 * (out of this package's scope, owned by a separate content-wave agent) that will call this same
 * method with {@code allowSpecials=true, damagingImpact=true}. Kept as a stateless static method
 * operating on the {@link LevelAccessor} interface - implemented by both {@code WorldGenLevel} (chunk
 * generation time) and {@code ServerLevel}/{@code Level} (live entity time) - specifically so one
 * call site here serves both callers without duplicating this 625-line algorithm.
 * <p>
 * <b>CE's {@code safeMode}/{@code replacables} static-mutable-field pattern</b> is replaced with an
 * explicit, per-call {@link GenCtx} record threaded through every helper method - behaviorally
 * identical (both are read-only for the duration of one {@link #generate} call), but removes CE's own
 * real footgun of two concurrent meteorite generations clobbering each other's static state.
 * <p>
 * <b>The {@code allowSpecials} 300-roll branch is ported in full for forward compatibility with the
 * future {@code EntityMeteor} caller</b>, even though it is dead code for this package's own feature
 * (which always passes {@code allowSpecials=false}). Two of its thirteen outcomes reference CE blocks
 * not yet registered anywhere in this port - {@code toxic_block} (case 8, "large nuclear meteorite")
 * and a taint block carrying CE's {@code BlockTaint.TAINTAGE} property (case 10, "tainted
 * meteorite") - confirmed absent by repo-wide grep. Both degrade gracefully (the rest of that case's
 * shape still generates; only the missing block's own placement is skipped) rather than throwing or
 * silently substituting a different block; see this package's own knownGaps for the exact scope of
 * what remains once those blocks land elsewhere. CE's switch also has no {@code case 11} (and no
 * {@code default}) - a real CE 1-in-300 gap where execution silently falls through to the ordinary
 * large/medium/small tier roll below; preserved verbatim via this method's own {@code default} arm.
 */
public final class MeteoriteGenerator {

    private MeteoriteGenerator() {
    }

    private record GenCtx(LevelAccessor level, RandomSource random, boolean safe, Set<Block> replacables) {
    }

    private record ShellLists(List<Block> hull, List<Block> op, List<Block> ip, List<Block> core, int coreTier) {
    }

    public static void generate(LevelAccessor level, RandomSource random, int x, int y, int z,
                                 boolean safe, boolean allowSpecials, boolean damagingImpact) {
        GenCtx ctx = new GenCtx(level, random, safe, replacables());

        if (damagingImpact && level instanceof ServerLevel serverLevel) {
            AABB box = new AABB(x - 7.5, y - 7.5, z - 7.5, x + 7.5, y + 7.5, z + 7.5);
            DamageSource source = serverLevel.damageSources().source(ModDamageTypes.METEORITE);
            for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
                entity.hurt(source, 1000F);
            }
        }

        if (allowSpecials && WorldConfig.ENABLE_SPECIAL_METEORS.get()) {
            switch (random.nextInt(300)) {
                case 0 -> {
                    // Meteor-only tiny meteorite.
                    generateBox(ctx, x, y, z, List.of(block("block_meteor")));
                    return;
                }
                case 1 -> {
                    // Large ore-only meteorite.
                    List<Block> list1 = new ArrayList<>(randomOre());
                    Block broken = block("block_meteor_broken");
                    int i = list1.size();
                    for (int j = 0; j < i; j++) list1.add(broken);
                    sphere7x7(ctx, x, y, z, list1);
                    return;
                }
                case 2 -> {
                    // Medium ore-only meteorite.
                    List<Block> list2 = new ArrayList<>(randomOre());
                    Block broken = block("block_meteor_broken");
                    int k = list2.size() / 2;
                    for (int j = 0; j < k; j++) list2.add(broken);
                    sphere5x5(ctx, x, y, z, list2);
                    return;
                }
                case 3 -> {
                    // Small pure ore meteorite.
                    generateBox(ctx, x, y, z, randomOre());
                    return;
                }
                case 4 -> {
                    // Bamboozle. Both the explosion and the rubble scatter need a full Level (chunk
                    // gen never reaches this branch - allowSpecials is always false there - so this
                    // only ever runs for a live-entity caller, which always has one).
                    if (level instanceof Level fullLevel) {
                        fullLevel.explode(null, x + 0.5, y + 0.5, z + 0.5, 15F, true, Level.ExplosionInteraction.TNT);
                        ExplosionLarge.spawnRubble(fullLevel, x, y, z, 25);
                    }
                    return;
                }
                case 5 -> {
                    // Large treasure-only meteorite.
                    sphere7x7(ctx, x, y, z, List.of(block("block_meteor_treasure"), block("block_meteor_broken")));
                    return;
                }
                case 6 -> {
                    // Medium treasure-only meteorite.
                    sphere5x5(ctx, x, y, z, List.of(
                            block("block_meteor_treasure"), block("block_meteor_treasure"), block("block_meteor_broken")));
                    return;
                }
                case 7 -> {
                    // Small pure treasure meteorite.
                    generateBox(ctx, x, y, z, List.of(block("block_meteor_treasure")));
                    return;
                }
                case 8 -> {
                    // Large nuclear meteorite. toxic_block is not registered anywhere in this port
                    // yet (confirmed absent by grep) - the treasure shell still generates; only the
                    // inner toxic core is skipped until that block lands elsewhere.
                    sphere7x7(ctx, x, y, z, List.of(block("block_meteor_treasure")));
                    Block toxic = block("toxic_block");
                    if (toxic != null) sphere5x5(ctx, x, y, z, List.of(toxic));
                    return;
                }
                case 9 -> {
                    // Giant ore meteorite.
                    sphere9x9(ctx, x, y, z, List.of(block("block_meteor_broken")));
                    sphere7x7(ctx, x, y, z, randomOre());
                    return;
                }
                case 10 -> {
                    // Tainted meteorite. A taint block carrying CE's BlockTaint.TAINTAGE=9 property
                    // is not registered anywhere in this port yet (confirmed absent by grep) - the
                    // broken-rock shell still generates; only the taint core is skipped until that
                    // block lands elsewhere.
                    sphere5x5(ctx, x, y, z, List.of(block("block_meteor_broken")));
                    Block taint = block("taint");
                    if (taint != null) {
                        level.setBlock(new BlockPos(x, y, z), taint.defaultBlockState(), 2);
                    }
                    return;
                }
                case 12 -> {
                    // Star Blaster.
                    if (level instanceof Level fullLevel) {
                        fullLevel.explode(null, x + 0.5, y + 0.5, z + 0.5, 10F, true, Level.ExplosionInteraction.TNT);
                        ItemStack stack = new ItemStack(LegacyWeaponItems.GUN_B92.get());
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Star Blaster").withStyle(ChatFormatting.BLUE));
                        fullLevel.addFreshEntity(new ItemEntity(fullLevel, x + 0.5, y + 0.5, z + 0.5, stack));
                    }
                    return;
                }
                default -> {
                    // CE's own switch defines no case 11 and no default - values 11 and 13-299 (288
                    // of 300 outcomes) fall through to the ordinary tier roll below untouched. Kept
                    // verbatim rather than "fixed".
                }
            }
        }

        switch (random.nextInt(3)) {
            case 0 -> generateLarge(ctx, x, y, z);
            case 1 -> generateMedium(ctx, x, y, z);
            default -> generateSmall(ctx, x, y, z);
        }
    }

    // ==================== size tiers ====================

    private static void generateLarge(GenCtx ctx, int x, int y, int z) {
        ShellLists s = buildShellLists(ctx.random());
        switch (ctx.random().nextInt(5)) {
            case 0 -> genL1(ctx, x, y, z, s);
            case 1 -> genL2(ctx, x, y, z, s);
            case 2 -> genL3(ctx, x, y, z, s);
            case 3 -> genL4(ctx, x, y, z, s);
            default -> genL5(ctx, x, y, z, s);
        }
    }

    private static void generateMedium(GenCtx ctx, int x, int y, int z) {
        ShellLists s = buildShellLists(ctx.random());

        List<Block> sCore = new ArrayList<>();
        switch (s.coreTier()) {
            case 0 -> sCore.add(block("block_meteor"));
            case 1 -> sCore.add(block("block_meteor_treasure"));
            case 2 -> {
                sCore.add(block("block_meteor_treasure"));
                sCore.add(block("block_meteor"));
            }
        }

        switch (ctx.random().nextInt(6)) {
            case 0 -> genM1(ctx, x, y, z, s.hull(), s.op(), s.ip(), sCore);
            case 1 -> genM2(ctx, x, y, z, s.hull(), s.op(), s.ip(), s.core());
            case 2 -> genM3(ctx, x, y, z, s.hull(), s.op(), s.ip(), s.core());
            case 3 -> genM4(ctx, x, y, z, s.hull(), s.op(), s.ip(), s.core());
            case 4 -> genM5(ctx, x, y, z, s.hull(), s.op(), s.ip(), s.core());
            default -> genM6(ctx, x, y, z, s.hull(), s.op(), s.ip(), s.core());
        }
    }

    private static void generateSmall(GenCtx ctx, int x, int y, int z) {
        RandomSource random = ctx.random();
        int hullType = random.nextInt(4);
        int core = random.nextInt(3);

        List<Block> hull = buildHull(hullType);

        List<Block> sCore = new ArrayList<>();
        switch (core) {
            case 0 -> sCore.add(block("block_meteor"));
            case 1 -> sCore.add(block("block_meteor_treasure"));
            case 2 -> {
                sCore.add(block("block_meteor_treasure"));
                sCore.add(block("block_meteor"));
            }
        }

        generateBox(ctx, x, y, z, hull);
        setRandomBlock(ctx, new BlockPos(x, y, z), sCore);
    }

    /** Shared hull/outer-padding/inner-padding/core construction identical between L and M tiers. */
    private static ShellLists buildShellLists(RandomSource random) {
        int hullType = random.nextInt(4);
        int outerPadding = 0;
        if (hullType == 2) outerPadding = 1 + random.nextInt(2);
        else if (hullType == 3) outerPadding = 2;

        int innerPadding = random.nextInt(hullType == 0 ? 3 : 2);

        int core = random.nextInt(2);
        if (innerPadding > 0) core = 2;

        List<Block> hull = buildHull(hullType);

        List<Block> op = new ArrayList<>();
        switch (outerPadding) {
            case 0 -> op.add(block("block_meteor_cobble"));
            case 1 -> {
                Block broken = block("block_meteor_broken");
                for (int i = 0; i < 99; i++) op.add(broken);
                op.add(block("block_meteor_treasure"));
            }
            case 2 -> {
                op.add(block("block_meteor_cobble"));
                op.add(block("block_meteor_broken"));
            }
        }

        List<Block> ip = new ArrayList<>();
        switch (innerPadding) {
            case 0 -> {
                Block broken = block("block_meteor_broken");
                for (int i = 0; i < 99; i++) ip.add(broken);
                ip.add(block("block_meteor_treasure"));
            }
            case 1 -> ip.add(block("block_meteor_broken"));
            case 2 -> ip.add(block("block_meteor_cobble"));
        }

        List<Block> core_ = new ArrayList<>();
        switch (core) {
            case 0 -> core_.add(block("block_meteor"));
            case 1 -> core_.add(block("block_meteor_treasure"));
            case 2 -> core_.addAll(randomOre());
        }

        return new ShellLists(hull, op, ip, core_, core);
    }

    private static List<Block> buildHull(int hullType) {
        List<Block> hull = new ArrayList<>();
        switch (hullType) {
            case 0 -> hull.add(block("block_meteor_molten"));
            case 1 -> hull.add(block("block_meteor_cobble"));
            case 2 -> {
                Block broken = block("block_meteor_broken");
                for (int i = 0; i < 99; i++) hull.add(broken);
                hull.add(block("block_meteor_treasure"));
            }
            case 3 -> {
                hull.add(block("block_meteor_molten"));
                hull.add(block("block_meteor_broken"));
            }
        }
        return hull;
    }

    // ==================== large-tier composition variants ====================

    private static void genL1(GenCtx ctx, int x, int y, int z, ShellLists s) {
        sphere7x7(ctx, x, y, z, s.hull());
        star5x5(ctx, x, y, z, s.op());
        star3x3(ctx, x, y, z, s.ip());
        setRandomBlock(ctx, new BlockPos(x, y, z), s.core());
    }

    private static void genL2(GenCtx ctx, int x, int y, int z, ShellLists s) {
        sphere7x7(ctx, x, y, z, s.hull());
        sphere5x5(ctx, x, y, z, s.op());
        star3x3(ctx, x, y, z, s.ip());
        setRandomBlock(ctx, new BlockPos(x, y, z), s.core());
    }

    private static void genL3(GenCtx ctx, int x, int y, int z, ShellLists s) {
        sphere7x7(ctx, x, y, z, s.hull());
        sphere5x5(ctx, x, y, z, s.op());
        generateBox(ctx, x, y, z, s.ip());
        setRandomBlock(ctx, new BlockPos(x, y, z), s.core());
    }

    private static void genL4(GenCtx ctx, int x, int y, int z, ShellLists s) {
        sphere7x7(ctx, x, y, z, s.hull());
        sphere5x5(ctx, x, y, z, s.op());
        generateBox(ctx, x, y, z, s.ip());
        star3x3(ctx, x, y, z, randomOre());
        setRandomBlock(ctx, new BlockPos(x, y, z), s.core());
    }

    private static void genL5(GenCtx ctx, int x, int y, int z, ShellLists s) {
        sphere7x7(ctx, x, y, z, s.hull());
        sphere5x5(ctx, x, y, z, s.op());
        star5x5(ctx, x, y, z, s.ip());
        star3x3(ctx, x, y, z, randomOre());
        setRandomBlock(ctx, new BlockPos(x, y, z), s.core());
    }

    // ==================== medium-tier composition variants ====================

    private static void genM1(GenCtx ctx, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        sphere5x5(ctx, x, y, z, hull);
        setRandomBlock(ctx, new BlockPos(x, y, z), core);
    }

    private static void genM2(GenCtx ctx, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        sphere5x5(ctx, x, y, z, hull);
        star3x3(ctx, x, y, z, op);
        setRandomBlock(ctx, new BlockPos(x, y, z), core);
    }

    private static void genM3(GenCtx ctx, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        sphere5x5(ctx, x, y, z, hull);
        generateBox(ctx, x, y, z, op);
        setRandomBlock(ctx, new BlockPos(x, y, z), core);
    }

    private static void genM4(GenCtx ctx, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        sphere5x5(ctx, x, y, z, hull);
        generateBox(ctx, x, y, z, op);
        star3x3(ctx, x, y, z, ip);
        setRandomBlock(ctx, new BlockPos(x, y, z), core);
    }

    private static void genM5(GenCtx ctx, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        sphere5x5(ctx, x, y, z, hull);
        generateBox(ctx, x, y, z, ip);
        setRandomBlock(ctx, new BlockPos(x, y, z), core);
    }

    private static void genM6(GenCtx ctx, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        sphere5x5(ctx, x, y, z, hull);
        generateBox(ctx, x, y, z, ip);
        star3x3(ctx, x, y, z, randomOre());
        setRandomBlock(ctx, new BlockPos(x, y, z), core);
    }

    // ==================== shape primitives ====================

    private static void sphere7x7(GenCtx ctx, int x, int y, int z, List<Block> set) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = -3; a < 4; a++) for (int b = -1; b < 2; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -3; b < 4; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -1; b < 2; b++) for (int c = -3; c < 4; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -2; a < 3; a++) for (int b = -2; b < 3; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -2; b < 3; b++) for (int c = -2; c < 3; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -2; a < 3; a++) for (int b = -1; b < 2; b++) for (int c = -2; c < 3; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
    }

    private static void sphere5x5(GenCtx ctx, int x, int y, int z, List<Block> set) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = -2; a < 3; a++) for (int b = -1; b < 2; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -2; b < 3; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -1; b < 2; b++) for (int c = -2; c < 3; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
    }

    private static void sphere9x9(GenCtx ctx, int x, int y, int z, List<Block> set) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = -4; a < 5; a++) for (int b = -1; b < 2; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -4; b < 5; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -1; b < 2; b++) for (int c = -4; c < 5; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -1; a < 2; a++) for (int b = -3; b < 4; b++) for (int c = -3; c < 4; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -3; a < 4; a++) for (int b = -1; b < 2; b++) for (int c = -3; c < 4; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -3; a < 4; a++) for (int b = -3; b < 4; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -3; a < 4; a++) for (int b = -2; b < 3; b++) for (int c = -2; c < 3; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -2; a < 3; a++) for (int b = -3; b < 4; b++) for (int c = -2; c < 3; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        for (int a = -2; a < 3; a++) for (int b = -2; b < 3; b++) for (int c = -3; c < 4; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
    }

    private static void generateBox(GenCtx ctx, int x, int y, int z, List<Block> set) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = -1; a < 2; a++) for (int b = -1; b < 2; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
    }

    private static void star5x5(GenCtx ctx, int x, int y, int z, List<Block> set) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = -1; a < 2; a++) for (int b = -1; b < 2; b++) for (int c = -1; c < 2; c++)
            setRandomBlock(ctx, pos.set(x + a, y + b, z + c), set);
        setRandomBlock(ctx, pos.set(x + 2, y, z), set);
        setRandomBlock(ctx, pos.set(x - 2, y, z), set);
        setRandomBlock(ctx, pos.set(x, y + 2, z), set);
        setRandomBlock(ctx, pos.set(x, y - 2, z), set);
        setRandomBlock(ctx, pos.set(x, y, z + 2), set);
        setRandomBlock(ctx, pos.set(x, y, z - 2), set);
    }

    private static void star3x3(GenCtx ctx, int x, int y, int z, List<Block> set) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        setRandomBlock(ctx, pos.set(x, y, z), set);
        setRandomBlock(ctx, pos.set(x + 1, y, z), set);
        setRandomBlock(ctx, pos.set(x - 1, y, z), set);
        setRandomBlock(ctx, pos.set(x, y + 1, z), set);
        setRandomBlock(ctx, pos.set(x, y - 1, z), set);
        setRandomBlock(ctx, pos.set(x, y, z + 1), set);
        setRandomBlock(ctx, pos.set(x, y, z - 1), set);
    }

    private static void setRandomBlock(GenCtx ctx, BlockPos pos, List<Block> set) {
        if (set.isEmpty()) return;
        placeBlock(ctx, pos, set.get(ctx.random().nextInt(set.size())));
    }

    private static void placeBlock(GenCtx ctx, BlockPos pos, @Nullable Block toPlace) {
        if (toPlace == null) return; // not-yet-registered upstream dependency - skip, don't crash.

        LevelAccessor level = ctx.level();
        BlockState targetState = level.getBlockState(pos);

        if (ctx.safe() && !targetState.canBeReplaced() && !ctx.replacables().contains(targetState.getBlock())) {
            return;
        }
        if (targetState.getDestroySpeed(level, pos) < 10_000F) {
            level.setBlock(pos, toPlace.defaultBlockState(), 2 | 16);
        }
    }

    // ==================== lookups ====================

    @Nullable
    private static Block block(String name) {
        return OreShapeUtil.block(name);
    }

    private static List<Block> randomOre() {
        List<Block> ores = new ArrayList<>(EnumMeteorType.VALUES.length);
        for (EnumMeteorType type : EnumMeteorType.VALUES) {
            Block ore = block("block_meteor_ore_" + type.name().toLowerCase(Locale.ROOT));
            if (ore != null) ores.add(ore);
        }
        return ores;
    }

    /** Ported from CE's {@code Meteorite.generateReplacables} - resolved fresh every call. */
    private static Set<Block> replacables() {
        Set<Block> set = new HashSet<>();
        addIfPresent(set, "block_meteor");
        addIfPresent(set, "block_meteor_broken");
        addIfPresent(set, "block_meteor_cobble");
        addIfPresent(set, "block_meteor_molten");
        addIfPresent(set, "block_meteor_treasure");
        for (EnumMeteorType type : EnumMeteorType.VALUES) {
            addIfPresent(set, "block_meteor_ore_" + type.name().toLowerCase(Locale.ROOT));
        }
        return set;
    }

    private static void addIfPresent(Set<Block> set, String name) {
        Block b = block(name);
        if (b != null) set.add(b);
    }

}
