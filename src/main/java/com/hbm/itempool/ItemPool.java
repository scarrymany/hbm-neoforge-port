package com.hbm.itempool;

import com.hbm.util.WeightedRandom;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPool} (104 lines) - the loot-pool registry framework
 * consumed by the satellite cargo system ({@code SatelliteMiner}/{@code SatelliteLunarMiner}) and,
 * eventually, C130 airdrops and the other {@code ItemPools*} content files. Per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md}'s Headline finding #6 and
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md}'s Open questions, this is a framework-only
 * port: {@code pools} (a registry populated by construction, following this port's own established
 * {@code BulletConfig}-style "constructor registers itself" idiom), {@link #getPool(String)}, and
 * {@link #getStack(ItemPool, RandomSource)} are the only 3 members downstream code needs.
 * <p>
 * Deliberate deviations from CE, already settled by the research reports above (do not re-litigate):
 * <ul>
 *   <li>CE's {@code meta} (1.12 metadata-subtype) parameter on every pool entry is dropped entirely -
 *   this port's Sedna gun/item content already gives every discrete variant its own real {@link
 *   net.minecraft.world.item.Item}, so there is nothing left for a metadata discriminator to select.</li>
 *   <li>The pool-entry type ({@link Entry}) builds directly on this port's already-real {@link
 *   WeightedRandom.Item} base (the same base {@code WeightedRandomObject} already extends) instead of
 *   porting a second, parallel weighted-random implementation - CE's {@code
 *   WeightedRandomChestContentFrom1710}/{@code WeightedRandomFrom1710} are NOT needed, since {@code
 *   WeightedRandom.getRandomItem(RandomSource, Collection)} already does exactly that job.</li>
 *   <li>CE's separate {@code add(...)}/{@code build()} fluent-builder path and {@code
 *   writeLootTable(...)} (vanilla {@code ILootContainer}/{@code TileEntityLockableLoot} integration)
 *   are not ported here - neither is used by {@code ItemPoolsSatellite} or {@code ItemPoolsC130} (the
 *   only two pool-content files researched so far); deferred to whichever future pool file first
 *   needs them.</li>
 * </ul>
 * <p>
 * <b>Pool-name key convention (settled here, not left open):</b> this port's already-committed {@code
 * SatelliteMiner}/{@code SatelliteLunarMiner} register their {@code CARGO} map with the literal keys
 * {@code "sat_miner"}/{@code "sat_lunar"}, not CE's own {@code ItemPoolsSatellite.POOL_SAT_MINER}/
 * {@code POOL_SAT_LUNAR} constant values ({@code "POOL_SAT_MINER"}/{@code "POOL_SAT_LUNAR"}). Per both
 * reports' explicit recommendation (editing the two already-shipped {@code Satellite*} files has zero
 * blast radius elsewhere, versus editing whichever pool-content file is written later), the resolution
 * is: whoever writes {@code ItemPoolsSatellite} must define its own pool-name constants with the
 * values {@code "sat_miner"}/{@code "sat_lunar"} (matching this port's own committed strings), NOT a
 * verbatim transcription of CE's {@code POOL_SAT_MINER}/{@code POOL_SAT_LUNAR} constant values. Do not
 * reintroduce the mismatch.
 */
public class ItemPool {

    private static final Map<String, ItemPool> POOLS = new HashMap<>();

    public final String name;

    /** Mutable on purpose: pool-content files (e.g. a future {@code ItemPoolsSatellite}) populate this
     *  directly, mirroring CE's own {@code this.pool = new WeightedRandomChestContentFrom1710[]{...}}
     *  double-brace-initializer idiom. */
    public List<Entry> pool = new ArrayList<>();

    public ItemPool(String name) {
        this.name = name;
        if (POOLS.containsKey(name)) {
            throw new IllegalStateException("Duplicate ItemPool name: " + name);
        }
        POOLS.put(name, this);
    }

    /** Non-registering constructor - used only by {@link #BACKUP}, mirroring CE's own unused no-arg
     *  {@code ItemPool()} constructor. Does not touch {@link #POOLS}. */
    private ItemPool() {
        this.name = null;
    }

    /**
     * Grabs the specified item pool out of the pool registry. Returns the hardcoded {@link #BACKUP}
     * pool if the given name is not registered (misconfiguration, a not-yet-ported pool file, etc) -
     * never throws and never returns {@code null}.
     */
    public static ItemPool getPool(String name) {
        ItemPool found = name == null ? null : POOLS.get(name);
        return found != null ? found : BACKUP;
    }

    /**
     * Rolls one weighted entry out of the given pool, then a random count in {@code [min, max]} for
     * that entry - CE's {@code ItemPool.getStack(WeightedRandomChestContentFrom1710[], Random)}.
     */
    public static ItemStack getStack(ItemPool pool, RandomSource rand) {
        if (pool == null || pool.pool.isEmpty()) return ItemStack.EMPTY;
        WeightedRandom.Item picked = WeightedRandom.getRandomItem(rand, pool.pool);
        if (!(picked instanceof Entry entry)) return ItemStack.EMPTY;
        ItemStack stack = entry.stack.copy();
        int count = entry.max <= entry.min ? entry.min : entry.min + rand.nextInt(entry.max - entry.min + 1);
        stack.setCount(count);
        return stack;
    }

    /** Convenience factory for pool-content files - CE's {@code HbmChestContents.weighted(...)}
     *  equivalent, with the {@code meta} parameter dropped per this class's javadoc. */
    public static Entry entry(ItemLike item, int min, int max, int weight) {
        return new Entry(new ItemStack(item), min, max, weight);
    }

    /** {@link #entry(ItemLike, int, int, int)} overload for a pre-built {@link ItemStack} (e.g. one
     *  already carrying NBT/component data). */
    public static Entry entry(ItemStack stack, int min, int max, int weight) {
        return new Entry(stack, min, max, weight);
    }

    /** One weighted pool entry: an item template plus the count range rolled when it's picked. */
    public static class Entry extends WeightedRandom.Item {
        public final ItemStack stack;
        public final int min;
        public final int max;

        public Entry(ItemStack stack, int min, int max, int weight) {
            super(weight);
            this.stack = stack;
            this.min = min;
            this.max = max;
        }
    }

    /**
     * CE's hardcoded 4-entry fallback pool (bread/stick/scrap/dust). Kept to pure vanilla items rather
     * than {@code ModItems} fields deliberately: this class (and therefore its static initializer) can
     * load before {@code RegisterEvent} fires for this mod's own item registry, and a {@code
     * DeferredItem#get()} call inside a static field initializer would then throw
     * {@code IllegalStateException} - the recurring bug pattern this port has hit repeatedly.
     * Vanilla's own {@code Items} fields have no such ordering hazard.
     */
    private static final ItemPool BACKUP = new ItemPool();

    static {
        BACKUP.pool.add(entry(Items.BREAD, 1, 3, 10));
        BACKUP.pool.add(entry(Items.STICK, 2, 5, 10));
        BACKUP.pool.add(entry(Items.IRON_NUGGET, 1, 3, 10));
        BACKUP.pool.add(entry(Items.COAL, 2, 5, 5));
    }
}
