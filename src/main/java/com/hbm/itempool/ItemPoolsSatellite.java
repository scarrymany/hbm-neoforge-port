package com.hbm.itempool;

import com.hbm.items.BilletPowderItems;
import com.hbm.items.PlateCrystalWasteItems;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.itempool.ItemPoolsSatellite} (59 lines, read in full) - the two
 * loot pools {@code SatelliteMiner}/{@code SatelliteLunarMiner} deliver via
 * {@code getCargo()}/{@link ItemPool#getStack}, per
 * {@code docs/phase4/satellites_followup_and_loot_pools.md}.
 * <p>
 * <b>Pool-name values - deliberately not CE's own constant values.</b> CE's real
 * {@code POOL_SAT_MINER}/{@code POOL_SAT_LUNAR} constants equal {@code "POOL_SAT_MINER"}/
 * {@code "POOL_SAT_LUNAR"}, but this port's already-committed {@code SatelliteMiner}/
 * {@code SatelliteLunarMiner} register their {@code CARGO} map with the literal strings
 * {@code "sat_miner"}/{@code "sat_lunar"} instead (Headline finding #5 / Open questions of the
 * report above). Per that report's own recommendation, this file's constant <i>values</i> are
 * changed to match those two already-shipped, zero-blast-radius strings rather than the other way
 * around - do not "fix" this back to CE's literal constant values, that would silently break
 * {@code SatelliteMiner}/{@code SatelliteLunarMiner}'s existing pool lookups.
 * <p>
 * <b>3 of CE's ~32 combined entries are skipped</b> (Headline finding #5): {@code ModItems.fluorite}
 * and {@code ModBlocks.gravel_diamond} (both confirmed absent from this port's registry by
 * repo-wide grep - only the higher-tier {@code crystal_fluorite} exists) and all 3 weighted rolls of
 * {@code ModBlocks.moon_turf} (likewise absent). Every other entry (23 of {@code POOL_SAT_MINER}'s
 * 26, all 7 of {@code POOL_SAT_LUNAR}'s) is ported with CE's exact min/max/weight values. CE's
 * {@code meta = 0} parameter on every entry is dropped per {@link ItemPool}'s own class javadoc -
 * this port's Sedna item content gives every discrete variant its own real {@code Item}, so there is
 * nothing left for a metadata discriminator to select.
 */
public final class ItemPoolsSatellite {

    public static final String POOL_SAT_MINER = "sat_miner";
    public static final String POOL_SAT_LUNAR = "sat_lunar";

    private ItemPoolsSatellite() {
    }

    public static void init() {

        ItemPool miner = new ItemPool(POOL_SAT_MINER);
        miner.pool.addAll(List.of(
                ItemPool.entry(BilletPowderItems.POWDER_ALUMINIUM.get(), 3, 3, 10),
                ItemPool.entry(BilletPowderItems.POWDER_IRON.get(), 3, 3, 10),
                ItemPool.entry(BilletPowderItems.POWDER_TITANIUM.get(), 2, 2, 8),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_TUNGSTEN.get(), 2, 2, 7),
                ItemPool.entry(BilletPowderItems.POWDER_COAL.get(), 4, 4, 15),
                ItemPool.entry(BilletPowderItems.POWDER_URANIUM.get(), 2, 2, 5),
                ItemPool.entry(BilletPowderItems.POWDER_PLUTONIUM.get(), 1, 1, 5),
                ItemPool.entry(BilletPowderItems.POWDER_THORIUM.get(), 2, 2, 7),
                ItemPool.entry(BilletPowderItems.POWDER_DESH_MIX.get(), 3, 3, 5),
                ItemPool.entry(BilletPowderItems.POWDER_DIAMOND.get(), 2, 2, 7),
                ItemPool.entry(Items.REDSTONE, 5, 5, 15),
                ItemPool.entry(BilletPowderItems.POWDER_NITAN_MIX.get(), 2, 2, 5),
                ItemPool.entry(BilletPowderItems.POWDER_POWER.get(), 2, 2, 5),
                ItemPool.entry(BilletPowderItems.POWDER_COPPER.get(), 5, 5, 15),
                ItemPool.entry(BilletPowderItems.POWDER_LEAD.get(), 3, 3, 10),
                // SKIPPED: CE weighted(ModItems.fluorite, 0, 4, 4, 15) - fluorite is not registered
                // in this port (only the higher-tier crystal_fluorite exists) - see class javadoc.
                ItemPool.entry(BilletPowderItems.POWDER_LAPIS.get(), 4, 4, 10),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_ALUMINIUM.get(), 1, 1, 5),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_GOLD.get(), 1, 1, 5),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_PHOSPHORUS.get(), 1, 1, 10),
                // SKIPPED: CE weighted(ModBlocks.gravel_diamond, 0, 1, 1, 3) - not registered
                // anywhere in this port - see class javadoc.
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_URANIUM.get(), 1, 1, 3),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_PLUTONIUM.get(), 1, 1, 3),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_TRIXITE.get(), 1, 1, 1),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_STARMETAL.get(), 1, 1, 1),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_LITHIUM.get(), 2, 2, 4)
        ));

        ItemPool lunar = new ItemPool(POOL_SAT_LUNAR);
        lunar.pool.addAll(List.of(
                // SKIPPED: CE's 3 weighted(ModBlocks.moon_turf, 0, {48,32,16}, {48,32,16}, {5,7,5})
                // rolls - moon_turf is not registered anywhere in this port - see class javadoc.
                ItemPool.entry(BilletPowderItems.POWDER_LITHIUM.get(), 3, 3, 5),
                ItemPool.entry(BilletPowderItems.POWDER_IRON.get(), 3, 3, 5),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_IRON.get(), 1, 1, 1),
                ItemPool.entry(PlateCrystalWasteItems.CRYSTAL_LITHIUM.get(), 1, 1, 1)
        ));
    }
}
