package com.hbm.lib;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

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
 * Phase 2's fluid-duct package adds {@link #canConnectFluid}, ported from CE's {@code Library:973-997}
 * (read in full) for {@code com.hbm.blockentity.network.PipeBaseBlockEntity}'s connection-mask cache
 * and the {@code IBlockFluidDuct} type-propagation flood fill - the per-neighbor adjacency test both
 * mechanisms share, translating {@code IBlockAccess}/{@code ForgeDirection} to {@code BlockGetter}/
 * {@code Direction} the same way every other {@code lib}/{@code api} class in this port already does.
 * <p>
 * Phase 2's storage-machines package (mass crates/batteries/capacitors) adds the six
 * battery/item-charging helpers CE's own {@code Library} carries at lines 282-387 (read in full):
 * {@link #chargeTEFromItems}, {@link #chargeItemsFromTE}, {@link #chargeBatteryIfValid},
 * {@link #dischargeBatteryIfValid}, {@link #isBattery}, {@link #isChargeableBattery},
 * {@link #isDischargeableBattery}, {@link #isEmptyBattery}, {@link #isFullBattery} - the exact gap
 * {@code docs/phase2/machines_storage.md}'s "Open questions / risks" section flagged as expected for
 * this pass to close. <b>One deliberate scope narrowing</b>: CE's versions of these methods have a
 * second branch reading a stack's Forge-Energy ({@code IEnergyStorage}) capability as a fallback when
 * the stack isn't an {@link IBatteryItem} (a same-item HE&lt;-&gt;FE bridge, gated behind
 * {@code GeneralConfig.conversionRateHeToRF}). That branch is dropped here, matching the precedent
 * already set by this port's {@link com.hbm.api.energymk2.IEnergyProviderMK2} javadoc (its own
 * neighbour-FE-bridge is "intentionally deferred to a later phase behind a config flag") - every
 * battery item this port has (Phase 1's {@code ItemBattery}/{@code ItemBatteryPack}/
 * {@code ItemBatterySC}/{@code ItemBatteryCreative}) is an {@link IBatteryItem}, so the FE branch has
 * no in-repo caller yet and would be dead, unverifiable code (no FE-capability item exists in this
 * port to test it against). Re-add it alongside that same deferred FE-bridge work, not independently
 * here.
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

    /**
     * Charges a machine's HE pool from a battery item sitting in one of its inventory slots (the
     * TE-side half of a charge/discharge slot pair - e.g. {@code MachineBaseBlockEntity} subclasses'
     * "insert a battery to top yourself up" slot). Ported from CE's {@code Library.chargeTEFromItems}
     * (see this class's javadoc for the one dropped branch: no Forge-Energy fallback).
     */
    public static long chargeTEFromItems(IItemHandlerModifiable inventory, int index, long power, long maxPower) {
        ItemStack stack = inventory.getStackInSlot(index);
        long powerNeeded = maxPower - power;
        if (powerNeeded <= 0) return power;
        long heExtracted = dischargeBatteryIfValid(stack, powerNeeded, false);
        return power + heExtracted;
    }

    /**
     * Charges a battery item sitting in a machine's inventory slot from the machine's own HE pool -
     * the mirror image of {@link #chargeTEFromItems}. Ported from CE's {@code Library.chargeItemsFromTE}.
     */
    public static long chargeItemsFromTE(IItemHandlerModifiable inventory, int index, long power, long maxPower) {
        ItemStack stackToCharge = inventory.getStackInSlot(index);
        if (stackToCharge.isEmpty() || power <= 0) {
            return power;
        }
        long heCharged = chargeBatteryIfValid(stackToCharge, power, false);
        return power - heCharged;
    }

    /**
     * Charges {@code stack} (an {@link IBatteryItem}) by up to {@code chargeAmountHE}, respecting
     * both the item's remaining headroom and its own {@link IBatteryItem#getChargeRate} unless
     * {@code instant} is set. Ported from CE's {@code Library.chargeBatteryIfValid} (Forge-Energy
     * branch dropped - see this class's javadoc).
     *
     * @return the actual amount charged, in HE.
     * @throws IllegalArgumentException if {@code chargeAmountHE <= 0}.
     */
    public static long chargeBatteryIfValid(@NotNull ItemStack stack, long chargeAmountHE, boolean instant) {
        if (stack.isEmpty()) return 0;
        if (chargeAmountHE <= 0) throw new IllegalArgumentException("chargeAmountHE must be > 0");
        if (!(stack.getItem() instanceof IBatteryItem battery)) return 0;
        long max = Math.max(0L, battery.getMaxCharge(stack));
        long cur = Math.max(0L, Math.min(max, battery.getCharge(stack)));
        long room = Math.max(0L, max - cur);
        long rate = Math.max(0L, battery.getChargeRate(stack));
        long req = instant ? chargeAmountHE : Math.min(chargeAmountHE, rate);
        long added = Math.min(req, room);
        if (added > 0) battery.chargeBattery(stack, added);
        return added;
    }

    /**
     * Discharges {@code stack} (an {@link IBatteryItem}) by up to {@code dischargeAmountHE},
     * respecting both its current charge and its own {@link IBatteryItem#getDischargeRate} unless
     * {@code instant} is set. Ported from CE's {@code Library.dischargeBatteryIfValid} (Forge-Energy
     * branch dropped - see this class's javadoc).
     *
     * @return the actual amount discharged, in HE.
     * @throws IllegalArgumentException if {@code dischargeAmountHE <= 0}.
     */
    public static long dischargeBatteryIfValid(@NotNull ItemStack stack, long dischargeAmountHE, boolean instant) {
        if (stack.isEmpty()) return 0;
        if (dischargeAmountHE <= 0) throw new IllegalArgumentException("dischargeAmountHE must be > 0");
        if (!(stack.getItem() instanceof IBatteryItem battery)) return 0;
        long cur = Math.max(0L, battery.getCharge(stack));
        long rate = Math.max(0L, battery.getDischargeRate(stack));
        long req = instant ? dischargeAmountHE : Math.min(dischargeAmountHE, rate);
        long take = Math.min(req, cur);
        if (take > 0) battery.dischargeBattery(stack, take);
        return take;
    }

    /** @return true if {@code stack} is an {@link IBatteryItem}. Ported from CE's {@code Library.isBattery}. */
    public static boolean isBattery(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
    }

    /** Ported from CE's {@code Library.isDischargeableBattery}. */
    public static boolean isDischargeableBattery(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getCharge(stack) > 0 && battery.getDischargeRate(stack) > 0;
    }

    /** Ported from CE's {@code Library.isChargeableBattery}. */
    public static boolean isChargeableBattery(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getMaxCharge(stack) > battery.getCharge(stack) && battery.getChargeRate(stack) > 0;
    }

    /** Ported from CE's {@code Library.isEmptyBattery}. */
    public static boolean isEmptyBattery(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getCharge(stack) <= 0;
    }

    /** Ported from CE's {@code Library.isFullBattery}. */
    public static boolean isFullBattery(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getCharge(stack) >= battery.getMaxCharge(stack);
    }

    /**
     * Whether the block/block-entity at {@code pos} accepts a fluid-duct connection of {@code type}
     * from the given side. {@code dir} is the duct's own connecting side (the direction pointing
     * <em>from</em> the duct <em>into</em> {@code pos}) - flipped to the neighbor's own incoming side
     * ({@code dir.getOpposite()}) before either check runs, exactly like CE's own
     * {@code Library.canConnectFluid}. Checks the block-level {@link IFluidConnectorBlockMK2} first
     * (a fixed machine port with no per-side block-entity state), then the block-entity-level
     * {@link IFluidConnectorMK2} (every duct/pipe and every {@code IFluidStandardReceiverMK2}/
     * {@code IFluidStandardSenderMK2} machine) - CE checks both because a block can implement one, the
     * other, or (rarely) neither.
     */
    public static boolean canConnectFluid(BlockGetter level, BlockPos pos, Direction dir, FluidType type) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return false;

        Direction incoming = dir.getOpposite();

        if (level.getBlockState(pos).getBlock() instanceof IFluidConnectorBlockMK2 blockCon
                && blockCon.canConnect(type, level, pos, incoming)) {
            return true;
        }

        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IFluidConnectorMK2 tileCon && tileCon.canConnect(type, incoming);
    }

    /**
     * Phase 4's {@code com.hbm.handler.radiation.RadiationSystemNT} bit-packing helpers, net-new to
     * this port (CE's own equivalents live scattered across CE's much larger 2500-line {@code Library}
     * god-class this port does not carry over wholesale - see class javadoc). Confirmed absent from
     * this file before this pass (zero matches for any of these names).
     * <p>
     * A <b>section key</b> packs one 16x16x16 subchunk section's owning chunk X/Z plus its subchunk Y
     * index into a single {@code long}: X in bits 0-23, Z in bits 24-47 (both signed 24-bit fields,
     * plenty of headroom past the vanilla +-30,000,000 block world border), subchunk Y (bias +128) in
     * bits 48-55. A <b>chunk key</b> is the same encoding with the Y field zeroed
     * ({@link #sectionToChunkLong}), so it groups every section of one chunk column under one map key.
     * A <b>local index</b> packs a block's position within its own section into one {@code int} in
     * {@code 0..4095} ({@code (y<<8)|(z<<4)|x} ordering).
     * <p>
     * This is a self-consistent internal format for this port's own runtime bookkeeping only - it is
     * <em>not</em> required to bit-match CE's 1.12.2 on-disk encoding (chunk/NBT formats are already
     * completely incompatible across that version gap for unrelated reasons), only to round-trip
     * correctly within this port.
     */
    public static long sectionToLong(int sectionX, int sectionZ, int sectionY) {
        return (((long) sectionX) & 0xFFFFFFL)
                | ((((long) sectionZ) & 0xFFFFFFL) << 24)
                | ((((long) (sectionY + 128)) & 0xFFL) << 48);
    }

    /** Replaces just the subchunk-Y field of an existing section key, keeping its chunk X/Z intact. */
    public static long setSectionY(long sectionKey, int sectionY) {
        long cleared = sectionKey & ~(0xFFL << 48);
        return cleared | ((((long) (sectionY + 128)) & 0xFFL) << 48);
    }

    public static int getSectionX(long sectionKey) {
        return (int) ((sectionKey << 40) >> 40);
    }

    public static int getSectionZ(long sectionKey) {
        return (int) ((sectionKey << 16) >> 40);
    }

    public static int getSectionY(long sectionKey) {
        return (int) (((sectionKey >>> 48) & 0xFFL) - 128);
    }

    /** Section key for the subchunk section containing {@code pos}. */
    public static long blockPosToSectionLong(BlockPos pos) {
        return sectionToLong(pos.getX() >> 4, pos.getZ() >> 4, pos.getY() >> 4);
    }

    /** The owning chunk's key (section key with the subchunk-Y field zeroed out). */
    public static long sectionToChunkLong(long sectionKey) {
        return sectionKey & 0xFFFFFFFFFFFFL;
    }

    /** Packs {@code pos}'s position within its own 16x16x16 section into {@code 0..4095}. */
    public static int blockPosToLocal(BlockPos pos) {
        return ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
    }

    public static int getLocalX(int local) {
        return local & 15;
    }

    public static int getLocalZ(int local) {
        return (local >> 4) & 15;
    }

    public static int getLocalY(int local) {
        return (local >> 8) & 15;
    }

    /** Full-precision world-block-position key; delegates to Mojang's own {@link BlockPos#asLong()}. */
    public static long blockPosToLong(BlockPos pos) {
        return pos.asLong();
    }
}
