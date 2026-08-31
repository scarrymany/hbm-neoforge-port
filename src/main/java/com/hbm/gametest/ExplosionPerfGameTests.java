package com.hbm.gametest;

import com.hbm.blockentity.bomb.NukeTsarBlockEntity;
import com.hbm.blocks.bomb.NukeCasingBlocks;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.EmptyTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bonus GameTest automation for docs/phase6/playtest_scenarios.md, scenario 5 ("explosion perf
 * benchmark, Tsar-scale") - the one part of that scenario that is a pure server-side stress test
 * with no real player input needed (place casing -> fill it -> trigger -> watch server tick health),
 * per this task's own instruction on what belongs in GameTest.
 *
 * <p><b>Deliberate deviation from the manual script</b>: docs/phase6/playtest_scenarios.md §6
 * triggers the real casing via a placed-and-flipped redstone lever, because a human tester has
 * hands. {@link GameTestHelper} has no player to place one, so this test calls
 * {@code NukeCasingBlockBase.explode(Level, BlockPos, Entity)} directly instead (confirmed public
 * on the {@link IBomb} interface it implements, {@code interfaces/IBomb.java:18}) - this reaches
 * the exact same code path CE's own {@code neighborChanged} redstone trigger calls
 * ({@code blocks/bomb/NukeCasingBlockBase.java:103-107}: {@code neighborChanged} itself just calls
 * {@code explode(level, pos, null)} when powered), so nothing about the detonation logic itself is
 * bypassed - only the redstone-signal delivery mechanism is skipped.
 *
 * <p><b>What this test actually measures</b>: not "did the explosion look right" (this port's
 * committed algorithm, {@code com.hbm.explosion.ExplosionNukeRayBatched}, is a real 2-phase,
 * tick-spread state machine per that class's own javadoc - correctness of its destruction shape is
 * out of this bonus test's scope) but whether any single tick between successive polls takes long
 * enough to be a real watchdog-kill risk, and whether the whole sequence finishes at all within a
 * generous timeout. See docs/phase6/playtest_scenarios.md §6 for the full reasoning on why this is
 * a real, non-obvious question for this specific algorithm.
 *
 * <p><b>Verification status</b>: never compiled or run (see this task's structured-output notes and
 * the manual scenario document's own §0). {@link IBomb}, {@code NukeTsarBlockEntity}'s slot layout,
 * and {@code NukeCasingBlocks.NUKE_TSAR} are all confirmed real by direct source read (cited
 * inline); the {@code @GameTestHolder}/{@code @PrefixGameTestTemplate}/{@code @EmptyTemplate}
 * annotation shapes carry the same unverified-against-a-real-compile caveat documented in
 * {@link ProgressionChainGameTests}'s own javadoc, and so does this file's specific use of
 * {@code GameTestHelper#absolutePos} (relative-to-world coordinate conversion, needed here because
 * {@link IBomb#explode} takes real world coordinates, unlike most other {@code GameTestHelper}
 * calls in this file which operate in the test's own relative space) - if that exact method name
 * differs, the fix is a one-line rename, not a logic change.
 */
@GameTestHolder("hbm")
@PrefixGameTestTemplate(false)
public final class ExplosionPerfGameTests {

    /**
     * Generous per-poll wall-clock ceiling, well under the vanilla/NeoForge watchdog's default
     * 60_000ms {@code max-tick-time} (see docs/phase6/playtest_scenarios.md §6) - this is a
     * proxy tripwire meant to fail loudly long before an actual watchdog kill would occur, not an
     * attempt to reproduce the watchdog's own exact threshold.
     */
    private static final long MAX_TICK_TO_TICK_MILLIS = 5_000L;

    private ExplosionPerfGameTests() {
    }

    private static ItemStack itemOf(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm", path);
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item);
    }

    /**
     * Places and fully arms a {@code hbm:nuke_tsar} casing (6-slot layout confirmed
     * {@code blockentity/bomb/NukeTsarBlockEntity.java:27-34}: slots 0-3
     * {@code hbm:explosive_lenses}, slot 4 {@code hbm:man_core}, slot 5 {@code hbm:tsar_core}),
     * detonates it directly, then polls once per tick recording the wall-clock gap since the
     * previous poll - fails immediately if any gap exceeds {@link #MAX_TICK_TO_TICK_MILLIS}, and
     * succeeds once a live {@code EntityNukeExplosionMK5} has been observed near the epicenter and
     * has subsequently discarded itself (confirmed {@code this.discard()} call at
     * {@code entity/logic/EntityNukeExplosionMK5.java:158} on completion). Deliberately does
     * <b>not</b> treat "the casing block is air" as the completion signal, even though that
     * happens synchronously inside {@code explode()} itself (before any of the actual multi-tick
     * destruction work runs) - using it would make this test pass almost immediately, defeating
     * its whole purpose. {@code timeoutTicks} below is deliberately generous (6000 ticks = 5 real
     * minutes at normal tick rate) to match the manual script's own suggested default budget - see
     * docs/phase6/playtest_scenarios.md §6's note on adjusting this to real hardware.
     */
    @GameTest(template = "", timeoutTicks = 6000, batch = "hbm.explosion_perf")
    @EmptyTemplate(value = {9, 9, 9})
    public static void tsarDetonationStaysWithinTickBudget(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, NukeCasingBlocks.NUKE_TSAR.get());

        if (!(helper.getBlockEntity(pos) instanceof NukeTsarBlockEntity be)) {
            helper.fail("Expected an NukeTsarBlockEntity at " + pos);
            return;
        }

        ItemStack lenses = itemOf("explosive_lenses");
        be.inventory.setStackInSlot(0, lenses.copy());
        be.inventory.setStackInSlot(1, lenses.copy());
        be.inventory.setStackInSlot(2, lenses.copy());
        be.inventory.setStackInSlot(3, lenses.copy());
        be.inventory.setStackInSlot(4, itemOf("man_core"));
        be.inventory.setStackInSlot(5, itemOf("tsar_core"));

        if (!be.isFilled()) {
            helper.fail("NukeTsarBlockEntity.isFilled() returned false after loading all 6 slots - "
                    + "slot contents did not match the item identity checks in that class");
            return;
        }

        BlockPos absolutePos = helper.absolutePos(pos);
        // Generous fixed-radius search box around the epicenter for the tracking entity itself
        // (the ray-cast algorithm's *destruction* reaches out to TSAR_RADIUS=500, but the
        // EntityNukeExplosionMK5 instance driving it does not relocate itself while working -
        // confirmed by reading its tick(): all per-tick work is dispatched through the stored
        // IExplosionRay against the fixed epicenter, the entity's own position is set once at
        // spawn and never updated - so a modest search box centered there is sufficient to find
        // it for as long as it is alive).
        AABB searchBox = new AABB(absolutePos).inflate(32.0D);

        AtomicLong lastPollNanos = new AtomicLong(System.nanoTime());
        AtomicBoolean detonated = new AtomicBoolean(false);
        AtomicBoolean sawLiveExplosionEntity = new AtomicBoolean(false);
        AtomicLong pollsSinceDetonation = new AtomicLong(0);

        helper.runAfterDelay(1, () -> {
            IBomb bomb = (IBomb) NukeCasingBlocks.NUKE_TSAR.get();
            IBomb.BombReturnCode result = bomb.explode(helper.getLevel(), absolutePos, null);
            if (result != IBomb.BombReturnCode.DETONATED) {
                helper.fail("Expected BombReturnCode.DETONATED, got " + result);
                return;
            }
            detonated.set(true);
            lastPollNanos.set(System.nanoTime());
        });

        helper.succeedWhen(() -> {
            if (!detonated.get()) {
                // Still waiting on the runAfterDelay(1, ...) callback above to fire; not a failure.
                throw new net.minecraft.gametest.framework.GameTestAssertException(
                        "waiting for detonation to start");
            }

            long now = System.nanoTime();
            long gapMillis = (now - lastPollNanos.get()) / 1_000_000L;
            lastPollNanos.set(now);
            long pollsSoFar = pollsSinceDetonation.incrementAndGet();

            helper.assertTrue(gapMillis <= MAX_TICK_TO_TICK_MILLIS,
                    "a tick-to-tick gap of " + gapMillis + "ms exceeded the " + MAX_TICK_TO_TICK_MILLIS
                            + "ms proxy threshold for a watchdog-kill risk, poll #" + pollsSoFar
                            + " since detonation");

            List<EntityNukeExplosionMK5> live =
                    helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class, searchBox);

            if (!live.isEmpty()) {
                sawLiveExplosionEntity.set(true);
                // Not done yet - keep polling (succeedWhen re-throws until it does not throw).
                throw new net.minecraft.gametest.framework.GameTestAssertException(
                        "detonation still in progress, poll #" + pollsSoFar);
            }

            // The tracking entity is gone. Only treat this as real completion if we actually
            // observed it alive at least once first - otherwise this could be a false-positive
            // "success" on the very first poll after explode(), before the entity has even
            // spawned/ticked once (see this method's own javadoc on why the naive "casing block
            // is air now" check was rejected as a completion signal).
            helper.assertTrue(sawLiveExplosionEntity.get(),
                    "EntityNukeExplosionMK5 was never observed alive in the search box - either it "
                            + "spawned somewhere unexpected or finished within a single tick, neither "
                            + "of which this test's search-box assumption accounts for");
        });
    }
}
