package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.util.Compat;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Applied Energistics 2 compat: intended to sum the radiation of an AE2 storage cell's virtual contents when AE2 is
 * loaded, mirroring CE's use of AE2's {@code IStorageCell}/{@code IAEItemStack} API.
 * <p>
 * <b>Deferred:</b> AE2's storage API was rewritten for 1.21 (generic {@code AEKey}/{@code MEStorage} storage instead
 * of CE's {@code IAEItemStack}/{@code IItemStorageChannel}), and no source for that API was available to this task.
 * Per the hard rule against inventing APIs, the actual cell-scraping logic is left out rather than guessed; this
 * class only detects whether AE2 is present so the integration step has a confirmed, safe place to add the real
 * scraping call once the AE2-for-1.21 API is verified.
 */
public class HazardTransformerRadiationME implements IHazardTransformer {

    private static final boolean AE2_LOADED = ModList.get().isLoaded(Compat.ModIds.AE2);

    @Override
    public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
        if (!AE2_LOADED) return;
        // AE2 storage-cell radiation scraping deferred, see class javadoc.
    }
}
