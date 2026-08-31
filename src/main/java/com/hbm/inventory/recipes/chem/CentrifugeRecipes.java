package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreOutput;
import com.hbm.items.special.BedrockOreType;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CentrifugeRecipes} (the item-only "ore washing"
 * centrifuge - see {@code docs/phase2/machines_chemical_isotope.md}'s table distinguishing it from
 * the real isotope-separation "gas centrifuge", {@link GasCentrifugeRecipes}). A flat
 * {@code HashMap<AStack, ItemStack[]>} keyed by input, up to 4 outputs, exactly like CE.
 * <p>
 * <b>Scope trim</b> (documented, same shape as {@code RefineryRecipes}'s own precedent): CE registers
 * ~154 real recipe entries (77 literal {@code recipes.put(} source lines, 16 of which sit inside a
 * {@code for(BedrockOreType type : VALUES)} loop and so expand to 96 runtime entries - see
 * {@code docs/phase7/mrec_11_centrifuge_misc.md} section 3.7). This class ports every entry whose
 * ingredients and outputs are confirmed registered in this port, using only items already present in
 * {@link BilletPowderItems}/{@link IngotNuggetItems}/{@link PlateCrystalWasteItems}/
 * {@link BedrockOreItems}, preserving CE's exact output quantities for every recipe it does carry.
 * Vanilla ore blocks are matched via NeoForge's common {@code c:ores/*} tags
 * ({@link OreDictStack#ofCommonTag}) rather than CE's 1.12 OreDictionary strings, per
 * {@code RecipesCommon}'s own documented tag-based replacement; CE's non-vanilla ore-dict tags
 * (schrabidium, plutonium, cobalt, fluorite, titanium, tungsten, thorium, beryllium, lignite) have no
 * port-side tag equivalent yet, so those are matched directly against this port's single primary
 * {@code OreBlocks} block instance instead (CE's own precedent for {@code ore_tikite}/
 * {@code block_euphemium_cluster}/{@code ore_nether_fire}, which use exact-block
 * {@link ComparableStack} rather than an oredict tag even in CE) - documented per-entry below; any
 * secondary ore variant of the same material (nether/gneiss counterparts) is not separately matched.
 * <p>
 * <b>{@code mrec-11-centrifuge-misc} pass</b> (see {@code docs/phase7/mrec_11_centrifuge_misc.md}):
 * added the full 96-entry bedrock-ore-chain loop ({@link #registerBedrockOreChain()}), 15 more
 * crystal-breakdown recipes, 9 more ore-washing recipes, 3 block-keyed misc recipes and 2 vanilla/
 * schraranium item-keyed recipes - see each section's own comment. {@code chunk_ore} (CE's
 * {@code ItemEnumMulti<EnumChunkType>}) is still not a registered item under any name in this port,
 * so the {@code chunk_ore} RARE recipe, {@code AL.ore()} (aluminium) and {@code crystal_aluminium}
 * stay unported; {@code "oreRareEarth"} has no confirmed modern tag/block equivalent; the AE2-
 * conditional certus-quartz entry is skipped (no AE2 compat plan). {@code powder_tektite}'s recipe,
 * {@code block_slag}'s recipe and the {@code powder_ash} COAL recipe stay unported (each needs a
 * generic {@code dust}/{@code dust_tiny} item, {@code block_slag} block or {@code powder_ash} item
 * family this port has not registered under any name).
 * <p>
 * <b>Substitution convention</b> (already established by {@code SILEXRecipes}/{@code RefineryRecipes}/
 * {@code MixerRecipes}/{@code GasCentrifugeRecipes} - see each class's own javadoc): CE's plain
 * {@code sulfur}/{@code niter}/{@code fluorite} items are not registered in this port under any name.
 * Every CE recipe that references one of them here substitutes the closest existing equivalent -
 * {@link PlateCrystalWasteItems#CRYSTAL_SULFUR}/{@link PlateCrystalWasteItems#CRYSTAL_NITER}/
 * {@link PlateCrystalWasteItems#CRYSTAL_FLUORITE} respectively (the {@code crystal_copper} recipe
 * below already carried this exact substitution, undocumented until this pass; {@code crystal_niter}
 * was previously truncated to its one non-niter output, now completed to all 4 CE outputs using the
 * same substitution). CE's {@code ModItems.ingot_mercury} field is a naming trap, not a missing item -
 * its real registry id is {@code nugget_mercury} (confirmed against CE source and this port's own
 * {@link IngotNuggetItems#NUGGET_MERCURY} javadoc), so every CE {@code ingot_mercury} reference below
 * maps to {@link IngotNuggetItems#NUGGET_MERCURY}, not a still-missing item. The previously-ported
 * {@code ores/redstone} recipe's 3rd output was an unexplained {@code POWDER_COBALT} substitution with
 * no CE basis; corrected here to CE's real {@code ingot_mercury} -&gt; {@link IngotNuggetItems#NUGGET_MERCURY}.
 * {@code crystal_gold}/{@code crystal_redstone} were each missing their real CE 4th
 * {@code ingot_mercury} output entirely (not substituted, just dropped); both restored here.
 */
public final class CentrifugeRecipes {

    public static final Map<AStack, ItemStack[]> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CentrifugeRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // ore washing: 1 ore -> 3x powder + 1 vanilla gravel byproduct, exactly CE's shape/quantities
        RECIPES.put(OreDictStack.ofCommonTag("ores/iron"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/gold"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/copper"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/lead"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/diamond"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/emerald"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/uranium"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_RA226.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/lapis"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LAPIS.get(), 6),
                new ItemStack(BilletPowderItems.POWDER_COBALT_TINY.get(), 1),
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        // CE: REDSTONE.ore() 3rd output is `ingot_mercury` (non-LBS amount 1), i.e. this port's
        // NUGGET_MERCURY (see class javadoc) - was previously an unexplained POWDER_COBALT swap.
        RECIPES.put(OreDictStack.ofCommonTag("ores/redstone"), new ItemStack[]{
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(OreDictStack.ofCommonTag("ores/coal"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 2),
                new ItemStack(Items.GRAVEL, 1)});

        // ---- mrec-11: 9 more ore-washing entries (CE non-vanilla OreDictStack tags, matched
        // directly against this port's single primary OreBlocks block per material - see class
        // javadoc "Substitution convention"/block-matching note) ----

        RECIPES.put(new ComparableStack(oreBlock("ore_lignite")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LIGNITE.get(), 2),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(new ComparableStack(oreBlock("ore_titanium")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        // vanilla nether quartz ore - real NeoForge common tag, unlike the other 8 entries here
        RECIPES.put(OreDictStack.ofCommonTag("ores/quartz"), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1),
                new ItemStack(Items.NETHERRACK, 1)});

        RECIPES.put(new ComparableStack(oreBlock("ore_tungsten")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(new ComparableStack(oreBlock("ore_thorium")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_THORIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_THORIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(new ComparableStack(oreBlock("ore_beryllium")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_EMERALD.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        // CE fluorite (F.ore()) output substituted per class javadoc's substitution convention
        RECIPES.put(new ComparableStack(oreBlock("ore_fluorite")), new ItemStack[]{
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 3),
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 3),
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(new ComparableStack(oreBlock("ore_schrabidium")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        // only overworld plutonium ore block this port registers is the nether variant
        RECIPES.put(new ComparableStack(oreBlock("ore_nether_plutonium")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_POLONIUM.get(), 3),
                new ItemStack(Items.GRAVEL, 1)});

        RECIPES.put(new ComparableStack(oreBlock("ore_cobalt")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 1),
                new ItemStack(Items.GRAVEL, 1)});

        // ---- mrec-11: 3 block-keyed misc entries (CE itself uses exact-block ComparableStack, not
        // an oredict tag, for all three) ----

        RECIPES.put(new ComparableStack(oreBlock("ore_tikite")), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get(), 2),
                new ItemStack(Items.END_STONE, 1)});

        RECIPES.put(new ComparableStack(ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()), new ItemStack[]{
                new ItemStack(IngotNuggetItems.NUGGET_EUPHEMIUM.get(), 7),
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 4),
                new ItemStack(IngotNuggetItems.INGOT_STARMETAL.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get(), 2)});

        RECIPES.put(new ComparableStack(oreBlock("ore_nether_fire")), new ItemStack[]{
                new ItemStack(Items.BLAZE_POWDER, 2),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 2),
                new ItemStack(IngotNuggetItems.INGOT_PHOSPHORUS.get(), 1),
                new ItemStack(Items.NETHERRACK, 1)});

        // ---- mrec-11: 2 vanilla/schraranium item-keyed misc entries ----

        RECIPES.put(new ComparableStack(Items.BLAZE_ROD), new ItemStack[]{
                new ItemStack(Items.BLAZE_POWDER, 1),
                new ItemStack(Items.BLAZE_POWDER, 1),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 1)});

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_SCHRARANIUM.get()), new ItemStack[]{
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get(), 3),
                new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), 2)});

        // crystal breakdown, exact CE quantities
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COAL.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COAL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_IRON.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // CE 4th output ingot_mercury x1 restored (see class javadoc) - was silently dropped before
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_GOLD.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // CE 4th output ingot_mercury x3 restored (see class javadoc) - was silently dropped before
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_REDSTONE.get()), new ItemStack[]{
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 3)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LAPIS.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LAPIS.get(), 4),
                new ItemStack(BilletPowderItems.POWDER_LAPIS.get(), 4),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 1),
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get(), 2)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_DIAMOND.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DIAMOND.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_URANIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_RA226.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // CE's ModItems.sulfur substituted with CRYSTAL_SULFUR - already-established convention
        // (see class javadoc), undocumented at this call site until this pass
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COPPER.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 2),
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get()),
                new ItemStack(BilletPowderItems.POWDER_COBALT_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LEAD.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // REVIEW FIX (r4-machine-recipes-batch2): CE's 3 plain-`niter` outputs were substituted with
        // CRYSTAL_NITER, but CRYSTAL_NITER is this same recipe's own input key - that made the recipe
        // self-referencing (feed 1 crystal_niter, get 9 crystal_niter + powder back), a duplication
        // exploit, not a faithful port. Plain `niter` still doesn't exist in this port (see class
        // javadoc), so those 3 outputs are dropped entirely instead of mis-substituted, matching this
        // file's own established convention elsewhere (recipes needing a missing item are simply not
        // ported/trimmed, not force-substituted with their own input item) - only the genuinely
        // portable 4th output remains.
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // ---- mrec-11: 15 more crystal-breakdown recipes ----

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_THORIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_THORIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_THORIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_URANIUM.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_RA226.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_PLUTONIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_POLONIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TITANIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // REVIEW FIX (r4-machine-recipes-batch2): same self-reference bug as crystal_niter above -
        // CE's 2 plain-`sulfur` outputs were substituted with CRYSTAL_SULFUR, this recipe's own input
        // key, making it a duplication exploit. Plain `sulfur` still doesn't exist in this port, so
        // those 2 outputs are dropped rather than mis-substituted; ingot_mercury -> NUGGET_MERCURY
        // stays (a real, non-self-referencing item).
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TUNGSTEN.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_TUNGSTEN.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // REVIEW FIX (r4-machine-recipes-batch2): same self-reference bug as crystal_niter/
        // crystal_sulfur above - CE's 2 plain-`fluorite` outputs were substituted with
        // CRYSTAL_FLUORITE, this recipe's own input key, making it a duplication exploit. Plain
        // `fluorite` still doesn't exist in this port, so those 2 outputs are dropped rather than
        // mis-substituted.
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get()), new ItemStack[]{
                new ItemStack(PlateCrystalWasteItems.GEM_SODALITE.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_BERYLLIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_BERYLLIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SCHRARANIUM.get()), new ItemStack[]{
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), 2)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SCHRABIDIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_RARE.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_DESH_MIX.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_DESH_MIX.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get(), 2)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_PHOSPHORUS.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 3),
                new ItemStack(IngotNuggetItems.INGOT_PHOSPHORUS.get(), 2),
                new ItemStack(Items.BLAZE_POWDER, 2)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TRIXITE.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_NITAN_MIX.get(), 1)});

        // CE's ModItems.fluorite substituted with CRYSTAL_FLUORITE (per class javadoc)
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_LITHIUM.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_LITHIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_QUARTZ.get(), 1),
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1)});

        // CE's ModItems.ingot_mercury x5 -> NUGGET_MERCURY x5 (see class javadoc)
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_STARMETAL.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_DURA_STEEL.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_ASTATINE.get(), 2),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get(), 5)});

        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_COBALT.get()), new ItemStack[]{
                new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 2),
                new ItemStack(BilletPowderItems.POWDER_IRON.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_COPPER.get(), 3),
                new ItemStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1)});

        // ---- mrec-11: full 96-entry bedrock-ore-chain loop (CE lines 220-241) ----
        registerBedrockOreChain();
    }

    /**
     * Ported from CE's {@code CentrifugeRecipes.registerDefaults()} lines 220-241 (the
     * {@code for(BedrockOreType type : BedrockOreType.VALUES)} loop) - 16 fixed per-type grade-to-
     * grade transitions x 6 {@link BedrockOreType} members = 96 real recipe entries at runtime, not
     * 16 source lines (see {@code docs/phase7/mrec_11_centrifuge_misc.md} section 3.7's loop-
     * multiplication note). CE's own {@code ItemBedrockOreNew.extract(BedrockOreOutput, double)}
     * resolves to a single {@code ItemEnumMulti}-style {@code ModItems.bedrock_ore_fragment}
     * metadata item (metadata = material id); this port's equivalent is one real {@link Item} per
     * (material, {@link MaterialShapes#FRAGMENT}) pair generated by
     * {@code com.hbm.items.MaterialItemGenerator}, resolved here by registry-id lookup
     * ({@link #shapeItem}) - the same convention {@code ArcWelderRecipes} already established for
     * this exact class of lookup. Every material referenced by any {@link BedrockOreType} is
     * confirmed to have {@link MaterialShapes#FRAGMENT} in its {@code setAutogen(...)} list (spot-
     * checked all 33 distinct materials against {@code com.hbm.inventory.material.Mats}), so none of
     * these 96 entries are blocked.
     */
    private static void registerBedrockOreChain() {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            // BASE / BASE_ROASTED -> PRIMARY x1 + gravel (CE lines 222-223)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.BASE).get()), new ItemStack[]{
                    bedrockStack(type, BedrockOreGrade.PRIMARY), new ItemStack(Items.GRAVEL)});
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.BASE_ROASTED).get()), new ItemStack[]{
                    bedrockStack(type, BedrockOreGrade.PRIMARY), new ItemStack(Items.GRAVEL)});
            // BASE_WASHED -> PRIMARY x2 + gravel (CE line 224)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.BASE_WASHED).get()), new ItemStack[]{
                    bedrockStack(type, BedrockOreGrade.PRIMARY), bedrockStack(type, BedrockOreGrade.PRIMARY), new ItemStack(Items.GRAVEL)});

            // PRIMARY_SULFURIC -> PRIMARY_NOSULFURIC x2 + SULFURIC_BYPRODUCT x2 (CE line 226)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_SULFURIC).get()), new ItemStack[]{
                    bedrockStack(type, BedrockOreGrade.PRIMARY_NOSULFURIC, 2), bedrockStack(type, BedrockOreGrade.SULFURIC_BYPRODUCT, 2)});
            // PRIMARY_SOLVENT -> PRIMARY_NOSOLVENT x2 + SULFURIC_BYPRODUCT x2 + SOLVENT_BYPRODUCT x2
            // (CE line 227 - sic, CE's own recipe mixes a sulfuric byproduct into the solvent step)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_SOLVENT).get()), new ItemStack[]{
                    bedrockStack(type, BedrockOreGrade.PRIMARY_NOSOLVENT, 2), bedrockStack(type, BedrockOreGrade.SULFURIC_BYPRODUCT, 2),
                    bedrockStack(type, BedrockOreGrade.SOLVENT_BYPRODUCT, 2)});
            // PRIMARY_RAD -> PRIMARY_NORAD x2 + SULFURIC_BYPRODUCT x2 + SOLVENT_BYPRODUCT x2 + RAD_BYPRODUCT x2 (CE line 228)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_RAD).get()), new ItemStack[]{
                    bedrockStack(type, BedrockOreGrade.PRIMARY_NORAD, 2), bedrockStack(type, BedrockOreGrade.SULFURIC_BYPRODUCT, 2),
                    bedrockStack(type, BedrockOreGrade.SOLVENT_BYPRODUCT, 2), bedrockStack(type, BedrockOreGrade.RAD_BYPRODUCT, 2)});

            // PRIMARY / PRIMARY_ROASTED -> extract(primary1) + extract(primary2) (CE lines 230-231)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY).get()), new ItemStack[]{
                    extract(type.primary1), extract(type.primary2)});
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_ROASTED).get()), new ItemStack[]{
                    extract(type.primary1), extract(type.primary2)});
            // PRIMARY_NOSULFURIC / PRIMARY_NOSOLVENT / PRIMARY_NORAD -> extract(p1) + extract(p2) + CRUMBS (CE lines 232-234)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_NOSULFURIC).get()), new ItemStack[]{
                    extract(type.primary1), extract(type.primary2), bedrockStack(type, BedrockOreGrade.CRUMBS)});
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_NOSOLVENT).get()), new ItemStack[]{
                    extract(type.primary1), extract(type.primary2), bedrockStack(type, BedrockOreGrade.CRUMBS)});
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_NORAD).get()), new ItemStack[]{
                    extract(type.primary1), extract(type.primary2), bedrockStack(type, BedrockOreGrade.CRUMBS)});
            // PRIMARY_FIRST -> extract(p1) x2 + extract(p2) + CRUMBS (CE line 235)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_FIRST).get()), new ItemStack[]{
                    extract(type.primary1), extract(type.primary1), extract(type.primary2), bedrockStack(type, BedrockOreGrade.CRUMBS)});
            // PRIMARY_SECOND -> extract(p1) + extract(p2) x2 + CRUMBS (CE line 236)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.PRIMARY_SECOND).get()), new ItemStack[]{
                    extract(type.primary1), extract(type.primary2), extract(type.primary2), bedrockStack(type, BedrockOreGrade.CRUMBS)});

            // SULFURIC_WASHED / SOLVENT_WASHED / RAD_WASHED -> extract(byproduct 1..3) + CRUMBS (CE lines 238-240)
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SULFURIC_WASHED).get()), new ItemStack[]{
                    extract(type.byproductAcid1), extract(type.byproductAcid2), extract(type.byproductAcid3), bedrockStack(type, BedrockOreGrade.CRUMBS)});
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.SOLVENT_WASHED).get()), new ItemStack[]{
                    extract(type.byproductSolvent1), extract(type.byproductSolvent2), extract(type.byproductSolvent3), bedrockStack(type, BedrockOreGrade.CRUMBS)});
            RECIPES.put(new ComparableStack(BedrockOreItems.get(type, BedrockOreGrade.RAD_WASHED).get()), new ItemStack[]{
                    extract(type.byproductRad1), extract(type.byproductRad2), extract(type.byproductRad3), bedrockStack(type, BedrockOreGrade.CRUMBS)});
        }
    }

    private static ItemStack bedrockStack(BedrockOreType type, BedrockOreGrade grade, int count) {
        return new ItemStack(BedrockOreItems.get(type, grade).get(), count);
    }

    private static ItemStack bedrockStack(BedrockOreType type, BedrockOreGrade grade) {
        return bedrockStack(type, grade, 1);
    }

    /** CE's {@code ItemBedrockOreNew.extract(o, 1)} - see {@link #registerBedrockOreChain()}'s javadoc. */
    private static ItemStack extract(BedrockOreOutput output) {
        return new ItemStack(shapeItem(output.material(), MaterialShapes.FRAGMENT), Math.min(output.amount(), 64));
    }

    /**
     * Resolve-by-id lookup against the already-populated {@link BuiltInRegistries#ITEM}, matching
     * the pattern already proven safe at runtime by {@code ArcWelderRecipes} (this method only ever
     * runs from {@link #register()}, itself only ever called from {@code CommonEvents.commonSetup}'s
     * {@code enqueueWork} - well after every item {@code RegisterEvent} has fired).
     */
    private static Item shapeItem(NTMMaterial mat, MaterialShapes shape) {
        String id = shape.buildRegistryName(mat);
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id))
                .orElseThrow(() -> new IllegalStateException(
                        "CentrifugeRecipes: item hbm:" + id + " is not registered - check com.hbm.items.MaterialItemGenerator"));
    }

    /** Same {@link BuiltInRegistries} resolve-by-id pattern as {@link #shapeItem}, for ore blocks. */
    private static Block oreBlock(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id))
                .orElseThrow(() -> new IllegalStateException(
                        "CentrifugeRecipes: block hbm:" + id + " is not registered - check com.hbm.blocks.OreBlocks"));
    }

    /**
     * Ported from CE's {@code CentrifugeRecipes.getOutput}: exact match first, then a linear scan
     * for the first applicable {@link AStack} (tag membership) - identical lookup order to CE.
     */
    public static ItemStack[] getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        for (Map.Entry<AStack, ItemStack[]> entry : RECIPES.entrySet()) {
            if (entry.getKey().matchesRecipe(stack, true)) {
                ItemStack[] out = entry.getValue();
                ItemStack[] copy = new ItemStack[out.length];
                for (int i = 0; i < out.length; i++) copy[i] = out[i].copy();
                return copy;
            }
        }
        return null;
    }
}
