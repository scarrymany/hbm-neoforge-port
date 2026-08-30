package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The two item companions {@link com.hbm.items.machine.ItemICFPellet}'s Phase 2 consumer
 * ({@code TileEntityICFPress}-equivalent, {@code docs/phase2/machine_fusion_watz.md}) needs but
 * Phase 1 did not register: CE's {@code ModItems.icf_pellet_empty} (the press's raw pellet-shell
 * input) and {@code ModItems.particle_muon} (the press's muon-catalyst input, consumed 1:1 to grant
 * a pressed pellet's muon-catalyzed bonus - see {@link com.hbm.items.machine.ItemICFPellet#getFusingDifficulty}).
 * <p>
 * Kept as its own registration class (registering directly into the shared
 * {@code ModItems.ITEMS}/{@code CreativeTabContents} statics, exactly like every other
 * {@code <Family>Blocks}/{@code <Family>Items} class in this Phase 2 wave) rather than folding into
 * {@code MachineItems} - avoids two parallel Phase 2 packages racing on that one shared file, per
 * this task's own package-isolation instruction. Both items are plain data-less {@link ItemBase}
 * instances (CE's own {@code ModItems.icf_pellet_empty}/{@code particle_muon} carry no NBT/behavior
 * beyond stack size), so no dedicated item subclass is needed.
 */
public final class IcfPressItems {

    public static DeferredItem<Item> ICF_PELLET_EMPTY;
    public static DeferredItem<Item> PARTICLE_MUON;

    private IcfPressItems() {
    }

    public static void registerAll() {
        ICF_PELLET_EMPTY = ModItems.ITEMS.register("icf_pellet_empty", () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, ICF_PELLET_EMPTY);

        PARTICLE_MUON = ModItems.ITEMS.register("particle_muon", () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, PARTICLE_MUON);
    }
}
