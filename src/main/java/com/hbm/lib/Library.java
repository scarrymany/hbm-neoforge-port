package com.hbm.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CE's {@code Library} is a self-annotated {@code @Spaghetti} ~2500-line god class mixing ray
 * tracing, AABB/cone math, chest-loot rolling, NBT compatibility comparison, energy-transfer glue
 * and number formatting, most of it reaching directly into {@code ModBlocks}, {@code ModItems},
 * capability, entity and tile-entity types that do not exist yet in this port. Porting it wholesale
 * here would either produce dead code (nothing to call the block/item/entity-facing methods) or
 * half-finished stubs, both of which are against the Phase 0 ground rules.
 * <p>
 * This Phase 0 stub carries forward only the one piece of {@code Library} that other in-scope
 * {@code lib}/{@code util} classes call: the busy-wait hint used by the lock-free collectors and
 * queues. CE routed this through a reflective {@code LambdaMetafactory} shim because it still had to
 * run on Java 8, where {@link Thread#onSpinWait()} does not exist; on Java 21 that indirection is
 * unnecessary and the call below is direct.
 * <p>
 * Phase 2 adds {@link #checkForPlayerEyePositions}, ported from CE's {@code Library:116} for
 * {@link com.hbm.handler.MultiblockHandlerXR#checkSpace} and
 * {@link com.hbm.blocks.BlockDummyableMBB#checkRequirement}.
 * <p>
 * Phase 2's GUI-framework package adds {@link #getShortNumber(long)}/{@link #getShortNumber(BigDecimal)}
 * and {@link #roundFloat(float, int)}, ported from CE's {@code Library:192-242} (read in full) for
 * {@link com.hbm.inventory.gui.GuiInfoContainer#drawElectricityInfo} - every CE/Neo-Edition machine GUI
 * that shows an HE power/maxPower hover tooltip formats both {@code long} values through this exact
 * method (Neo Edition's confirmed-real {@code InfoScreen.drawElectricityInfo} calls the equivalent
 * {@code BobMathUtil.getShortNumber}), so it belongs on this shared class rather than duplicated per
 * machine once a second caller needs it.
 * <p>
 * The remaining ~2500 lines of {@code Library} must be triaged method-by-method against the areas
 * that call them (energy/fluid, capability, blocks/items, entity, tileentity) once those areas exist,
 * per the Phase 0 research plan; this class is expected to grow incrementally as later phases port
 * the methods they actually need.
 */
public final class Library {

    private Library() {
    }

    public static void onSpinWait() {
        Thread.onSpinWait();
    }

    /**
     * Fix for players getting suffocated/trapped when a multiblock is placed around them: refuses
     * the placement if a non-creative, non-spectator player's eye position falls inside {@code aabb}
     * and the space directly above their head either has a solid collision shape or is itself part
     * of {@code aabb} (i.e. they couldn't even jump out).
     * <p>
     * Ported from CE's {@code Library.checkForPlayerEyePositions(World, AxisAlignedBB)}. CE computed
     * eye height as {@code posY + eyeHeight}; kept identical here ({@code getY() + getEyeHeight()})
     * rather than the newer {@code Entity#getEyeY()} shortcut, since {@code getEyeHeight()} is the
     * accessor already proven to compile elsewhere in this port (see
     * {@code com.hbm.hazard.type.HazardTypeExplosive}).
     */
    public static boolean checkForPlayerEyePositions(Level level, AABB aabb) {
        // only check for players (cuz fuck off if a sheep gets in way)
        List<Player> players = level.getEntitiesOfClass(Player.class, aabb);
        for (Player player : players) {
            // imagine building modular turbine in LCA and you can't place large turbine blocks between others
            if (player.isCreative() || player.isSpectator()) continue;

            // only check for eye positions
            double eyeY = player.getY() + player.getEyeHeight();
            if (!aabb.contains(new Vec3(player.getX(), eyeY, player.getZ()))) continue;

            BlockPos above = BlockPos.containing(player.getX(), eyeY + 1, player.getZ());
            // finally, if the player cannot escape the block by simply jumping
            boolean solidAbove = !level.getBlockState(above).getCollisionShape(level, above).isEmpty();
            boolean aboveAlsoInside = aabb.contains(Vec3.atLowerCornerOf(above).add(0.5, 0.5, 0.5));
            if (solidAbove || aboveAlsoInside) return false;
        }
        return true;
    }

    private static final int[] POWERS_OF_TEN =
            {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    private static final DecimalFormat SHORT_NUMBER_FORMAT = new DecimalFormat("0.00");

    /** Magnitude-suffix table for {@link #getShortNumber(BigDecimal)}, ported unchanged from CE. */
    private static final Map<Integer, String> SHORT_NUMBER_SUFFIXES = buildShortNumberSuffixes();

    private static Map<Integer, String> buildShortNumberSuffixes() {
        // TreeMap so entrySet() iterates smallest exponent first, matching CE's own iteration order
        // (the loop below keeps overwriting `result` as long as the value clears the next threshold,
        // so it must walk the thresholds ascending to end on the largest one that still fits).
        Map<Integer, String> map = new TreeMap<>();
        map.put(3, "k");
        map.put(6, "M");
        map.put(9, "G");
        map.put(12, "T");
        map.put(15, "P");
        map.put(18, "E");
        map.put(21, "Z");
        map.put(24, "Y");
        map.put(27, "R");
        map.put(30, "Q");
        return map;
    }

    public static float roundFloat(float number, int decimals) {
        return (float) (Math.round(number * POWERS_OF_TEN[decimals]) / (float) POWERS_OF_TEN[decimals]);
    }

    /**
     * Formats a (potentially very large, HE-power-sized) {@code long} with an SI-ish magnitude
     * suffix instead of a wall of digits - e.g. {@code 4200000L -> "4.20M"}. Ported from CE's
     * {@code Library.getShortNumber(long)}.
     */
    public static String getShortNumber(long l) {
        return getShortNumber(BigDecimal.valueOf(l));
    }

    /** {@link BigDecimal} overload, ported from CE's {@code Library.getShortNumber(BigDecimal)}. */
    public static String getShortNumber(BigDecimal l) {
        boolean negative = l.signum() < 0;
        if (negative) l = l.negate();

        String result = l.toPlainString();
        for (Map.Entry<Integer, String> suffix : SHORT_NUMBER_SUFFIXES.entrySet()) {
            BigDecimal threshold = new BigDecimal("1E" + suffix.getKey());
            if (l.compareTo(threshold) >= 0) {
                // exact: threshold is always a power of ten, so this never hits a non-terminating
                // decimal expansion and never needs a MathContext/RoundingMode.
                double scaled = l.divide(threshold).doubleValue();
                result = SHORT_NUMBER_FORMAT.format(roundFloat((float) scaled, 2)) + suffix.getValue();
            } else {
                break;
            }
        }

        return negative ? "-" + result : result;
    }
}
