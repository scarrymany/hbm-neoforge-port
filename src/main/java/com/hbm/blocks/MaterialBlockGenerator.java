package com.hbm.blocks;

import com.hbm.blocks.generic.BlockBeaconable;
import com.hbm.blocks.generic.BlockHazard;
import com.hbm.blocks.generic.BlockHydroreactive;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.blocks.generic.BlockRadResistant;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Drives CE's "Material Blocks" section (upstream {@code ModBlocks.java} ~lines 437-490/616-651) -
 * one full-cube storage block per material tagged {@code .setAutogen(..., MaterialShapes.BLOCK, ...)}
 * in {@link Mats} - directly off {@link Mats#orderedList}, per
 * {@code docs/phase1/modblocks_generative.md} section 1b/6.3. 57 materials are currently tagged.
 * <p>
 * <b>Naming.</b> {@link MaterialShapes#buildRegistryName(NTMMaterial)} produces suffix-first ids
 * ({@code titanium_block}, {@code uranium_block}), a deliberate departure from CE's prefix-first
 * legacy ids ({@code block_titanium}, {@code block_uranium}) - the same tradeoff already accepted for
 * every other {@code Mats x MaterialShapes} item family in this port (e.g. {@code iron_ingot}, not
 * {@code ingot_iron}; see {@code com.hbm.items.MaterialItemGenerator}). This is a save-id change from
 * CE, called out in the research doc, not a bug.
 * <p>
 * <b>Behavior class per material.</b> CE varies the concrete {@code Block} subclass per material even
 * though every instance is shape-identical (a plain full cube). Classification below is per-material,
 * cross-checked against CE's actual {@code ModBlocks.java} field declarations at the line ranges named
 * above:
 * <ul>
 *     <li>{@link BlockHazard} - the thorium/plutonium/uranium/schrabidium/radium fuel-tier family
 *     (plutonium and its RG/238/239/240/241 isotopes, uranium and its 233/235/238 isotopes, thorium,
 *     neptunium, polonium, schraranium, schrabidium, schrabidate, solinium, radium, magnetized
 *     tungsten, and the americium isotopes).</li>
 *     <li>{@link BlockRadResistant} - lead, boron (CE's shielding-marker blocks).</li>
 *     <li>{@link BlockBeaconable} - bismuth, tantalum, niobium, lanthanum, zirconium.</li>
 *     <li>{@link BlockOutgas} - asbestos.</li>
 *     <li>{@link BlockHydroreactive} - lithium.</li>
 *     <li>{@link BlockFallingBase} - red phosphorus (CE's real {@code block_red_phosphorus} is a
 *     {@code Material.SAND} falling block, not a plain cube - see the hardness/resistance note below
 *     for the full CE comparison).</li>
 *     <li>{@link BlockBase} - every other material (titanium, copper, tungsten, aluminium, steel,
 *     cobalt, niter, sulfur, fluorite, beryllium, dura-steel, desh, starmetal, combine steel,
 *     dineutronium, saturnite, carbon, iron, gold, emerald, neodymium, mingrade, technetium,
 *     ghiorsium, ...). <b>Known simplification:</b> slag is grouped here too, but CE's real
 *     {@code block_slag} (upstream {@code ModBlocks.java} line 624) is
 *     {@code new BlockMeta(Material.ROCK, SoundType.STONE, "block_slag", "block_slag",
 *     "block_slag_broken")} - a multi-variant meta-block with a distinct "broken" sub-state, not a
 *     single-state full cube. This port has no {@code BlockMeta}-equivalent class yet, so slag is
 *     collapsed to a plain {@link BlockBase} pending one; its 5.0F/10.0F hardness/resistance survives
 *     the simplification by coincidence.</li>
 * </ul>
 * <p>
 * <b>Hardness/resistance.</b> Taken from CE's real {@code .setHardness(...).setResistance(...)} calls
 * where a directly-named CE block exists for the material (verified by grep against upstream
 * {@code ModBlocks.java}: {@code block_bismuth}/{@code _tantalium}/{@code _niobium} = 5.0F/30.0F,
 * {@code block_schrabidium}/{@code _schraranium}/{@code _schrabidate}/{@code _solinium} = 5.0F/300.0F,
 * {@code block_magnetized_tungsten} = 5.0F/35.0F, {@code block_dura_steel}/{@code _desh}/
 * {@code _starmetal}/{@code _combine_steel} = 5.0F/300.0F, {@code block_dineutronium} = 5.0F/60000.0F,
 * {@code block_saturnite} = 6.0F/400.0F, {@code block_asbestos} = 10.0F/10.0F). CE has no directly
 * named storage block at all for carbon, iron, gold, the plutonium-RG/americium isotopes, neodymium,
 * technetium, ghiorsium, mingrade, or slag (slag's real CE analogue, {@code block_slag}, is likewise
 * 5.0F/10.0F, so the fallback default happens to match anyway) - these fall back to CE's overwhelming
 * default of 5.0F/10.0F for this whole section, same as every other material not called out with a
 * specific override. Red phosphorus is a related but distinct case: its real CE analogue is
 * {@code block_red_phosphorus} ({@link BlockFallingBase}, {@code Material.SAND},
 * {@code SoundType.SAND}, still 5.0F/10.0F by coincidence) - not {@code block_white_phosphorus}
 * (a {@code BlockHazard}), which has no corresponding material in {@link Mats} at all. Unlike slag,
 * this port does carry a ready-made {@link BlockFallingBase} and registers red phosphorus with it
 * (see the classification list above), so the behavior-class gap is closed rather than simplified
 * away.
 * <p>
 * <b>Sound/tool.</b> CE's own material blocks split between an explicit {@code SoundType.METAL} call
 * and (for niter/sulfur/fluorite/lead/ra226, which never call {@code setSoundType}) Block's 1.12
 * default of stone footsteps - a purely cosmetic difference not worth branching 57 ways for. Every
 * block here uses {@code SoundType.METAL} uniformly. Likewise, every CE instance here is built on
 * {@code Material.IRON}, which in 1.12 gates harvesting behind a pickaxe via {@code Material}-keyed
 * harvest logic that no longer exists in 1.21 (tool-tier requirements are now a datagen block-tag
 * concern - see {@link OreBlocks}'s class javadoc for the identical tradeoff already accepted for the
 * ore family); the coarse {@link BlockBehaviour.Properties#requiresCorrectToolForDrops()} flag is set
 * for every block here as the nearest available equivalent.
 * <p>
 * <b>Overlap with {@code GenericDecoBlocks} - resolved.</b> {@code GenericDecoBlocks} originally
 * also hand-registered {@code block_bismuth}/{@code block_tantalium}/{@code block_niobium}/
 * {@code block_lanthanium}/{@code block_zirconium} under CE's legacy prefix-first ids. Since all five
 * materials carry {@code MaterialShapes.BLOCK} autogen in {@code Mats.java}, this class is their
 * authoritative source (per {@code docs/phase1/modblocks_generative.md} section 1b); the duplicate
 * hand-authored copies were removed from {@code GenericDecoBlocks}, which now only keeps
 * {@code block_cadmium}/{@code block_coltan}/{@code block_actinium} under their legacy ids, since
 * those three materials are not {@code Mats}-tagged for {@code BLOCK} autogen and have no equivalent
 * here.
 */
public final class MaterialBlockGenerator {

    private static final float STD_HARDNESS = 5.0F;
    private static final float STD_RESISTANCE = 10.0F;

    /** Per-material hardness/resistance overrides; anything absent from this map uses the CE-default 5.0F/10.0F pair. */
    private static final Map<NTMMaterial, float[]> HARDNESS_RESISTANCE_OVERRIDES = new HashMap<>();

    static {
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_BISMUTH, new float[]{5.0F, 30.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_TANTALIUM, new float[]{5.0F, 30.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_NIOBIUM, new float[]{5.0F, 30.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_SCHRABIDIUM, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_SCHRARANIUM, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_SCHRABIDATE, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_SOLINIUM, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_MAGTUNG, new float[]{5.0F, 35.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_ASBESTOS, new float[]{10.0F, 10.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_DURA, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_DESH, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_STAR, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_CMB, new float[]{5.0F, 300.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_DNT, new float[]{5.0F, 60000.0F});
        HARDNESS_RESISTANCE_OVERRIDES.put(Mats.MAT_SATURN, new float[]{6.0F, 400.0F});
    }

    /**
     * CE's contact-radiation-hazard family (see class javadoc). Known gap: CE's real
     * {@code block_ra226} additionally chains {@code .makeBeaconable()}, layering beacon-base
     * eligibility on top of the hazard behavior - the one CE material with both traits at once. This
     * port's single-behavior-class-per-material design has no way to compose {@link BlockHazard} with
     * {@link BlockBeaconable}'s beacon-base check, so {@code radium_block} loses the beacon-base trait
     * (contact radiation is preserved). Follow-up for whoever designs a trait/interface-composable
     * block-behavior system; not fixed here.
     */
    private static final Set<NTMMaterial> HAZARD_MATERIALS = new HashSet<>(List.of(
            Mats.MAT_PLUTONIUM, Mats.MAT_RGP, Mats.MAT_PU238, Mats.MAT_PU239, Mats.MAT_PU240, Mats.MAT_PU241,
            Mats.MAT_URANIUM, Mats.MAT_U233, Mats.MAT_U235, Mats.MAT_U238,
            Mats.MAT_THORIUM, Mats.MAT_NEPTUNIUM, Mats.MAT_POLONIUM,
            Mats.MAT_SCHRARANIUM, Mats.MAT_SCHRABIDIUM, Mats.MAT_SCHRABIDATE, Mats.MAT_SOLINIUM,
            Mats.MAT_RADIUM, Mats.MAT_MAGTUNG,
            Mats.MAT_RGA, Mats.MAT_AM241, Mats.MAT_AM242
    ));

    /** CE's shielding-marker family. */
    private static final Set<NTMMaterial> RAD_RESISTANT_MATERIALS = Set.of(Mats.MAT_LEAD, Mats.MAT_BORON);

    /** CE's beacon-base-eligible family. */
    private static final Set<NTMMaterial> BEACONABLE_MATERIALS = Set.of(
            Mats.MAT_BISMUTH, Mats.MAT_TANTALIUM, Mats.MAT_NIOBIUM, Mats.MAT_LANTHANIUM, Mats.MAT_ZIRCONIUM
    );

    /** Every registered material storage block, keyed by material - consumed by {@code HazardRegistry}. */
    private static final Map<NTMMaterial, DeferredBlock<? extends Block>> BLOCKS_BY_MATERIAL = new HashMap<>();

    private MaterialBlockGenerator() {
    }

    /**
     * Registers one full-cube storage block for every material in {@link Mats#orderedList} whose
     * {@link NTMMaterial#getAutogen()} contains {@link MaterialShapes#BLOCK} (57 materials as of this
     * writing), with a behavior class looked up per the classification in this class's javadoc.
     */
    public static void registerAll() {
        for (NTMMaterial mat : Mats.orderedList) {
            if (!mat.getAutogen().contains(MaterialShapes.BLOCK)) {
                continue;
            }
            registerMaterial(mat);
        }
    }

    /**
     * Returns the storage block registered for {@code mat} by {@link #registerAll()}, or {@code null}
     * if {@code mat} was not tagged for {@link MaterialShapes#BLOCK} autogen. Used by
     * {@code HazardRegistry} to bind contact-radiation levels to the right block.
     */
    public static DeferredBlock<? extends Block> get(NTMMaterial mat) {
        return BLOCKS_BY_MATERIAL.get(mat);
    }

    private static void registerMaterial(NTMMaterial mat) {
        BlockBehaviour.Properties props = propsFor(mat);

        if (HAZARD_MATERIALS.contains(mat)) {
            register(mat, () -> new BlockHazard(props));
        } else if (RAD_RESISTANT_MATERIALS.contains(mat)) {
            register(mat, () -> new BlockRadResistant(props));
        } else if (BEACONABLE_MATERIALS.contains(mat)) {
            register(mat, () -> new BlockBeaconable(props));
        } else if (mat == Mats.MAT_ASBESTOS) {
            register(mat, () -> new BlockOutgas(props));
        } else if (mat == Mats.MAT_LITHIUM) {
            register(mat, () -> new BlockHydroreactive(props));
        } else if (mat == Mats.MAT_PHOSPHORUS) {
            register(mat, () -> new BlockFallingBase(props));
        } else {
            register(mat, () -> new BlockBase(props));
        }
    }

    private static <T extends Block> void register(NTMMaterial mat, Supplier<T> factory) {
        String name = MaterialShapes.BLOCK.buildRegistryName(mat);
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.BLOCKS, block);
        BLOCKS_BY_MATERIAL.put(mat, block);
    }

    private static BlockBehaviour.Properties propsFor(NTMMaterial mat) {
        float[] hr = HARDNESS_RESISTANCE_OVERRIDES.getOrDefault(mat, new float[]{STD_HARDNESS, STD_RESISTANCE});
        return BlockBehaviour.Properties.of().strength(hr[0], hr[1]).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }
}
