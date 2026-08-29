package com.hbm.inventory.material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog of physical forms a material can come in (nugget, ingot, plate, dust, wire, block...),
 * all expressed as a multiple of a common "quantum" unit so quantities stay proportional across
 * shapes (1 ingot = 9 nuggets = 72 quanta). Ported from CE near-verbatim.
 *
 * <p>Two naming schemes are attached to every shape that corresponds to a real item:
 * <ul>
 *     <li>{@link #registryName} - singular snake_case token used to build a per-material item id,
 *     e.g. shape INGOT + material "iron" -> registry id {@code hbm:iron_ingot}.</li>
 *     <li>{@link #tagFolder} - plural folder used for NeoForge/vanilla common item tags,
 *     e.g. {@code c:ingots/iron}, following the {@code c:<plural>/<material>} convention.</li>
 * </ul>
 * Both are {@code null} for technical/compat shapes that never back a real registered item
 * (ANY, ORE variants, QUANTUM, QUART) - those exist purely to express ore-dict prefixes or
 * recipe-quantity units.
 *
 * <p>CE's GregTech-compatibility shapes ({@code registerCompatShapes}, gated behind
 * {@code Loader.isModLoaded("gregtech")}) are intentionally not ported - GTCEu compatibility is
 * out of scope for this port, not merely stubbed.
 */
public class MaterialShapes {

    public static final List<MaterialShapes> allShapes = new ArrayList<>();
    /** Legacy CE Forge ore-dict prefix (e.g. "ingot", "wireDense") -> shape. Kept for cross-referencing CE. */
    public static final Map<String, MaterialShapes> prefixByName = new HashMap<>();
    /** Canonical registry-name token (e.g. "ingot", "dense_wire") -> shape, for Phase 1 item generation. */
    public static final Map<String, MaterialShapes> shapeByRegistryName = new HashMap<>();

    /*
     * Ore shapes never back a real generated item - ore blocks/items are handled entirely
     * outside the autogen system, and their crucible yield ratios are curated per-ore (see
     * Mats.registerOre), not the uniform shape.q(1) relationship autogen shapes get. They keep
     * their legacy prefixes for cross-referencing CE but carry no registry name or tag folder.
     */
    public static final MaterialShapes ANY = new MaterialShapes(0, null, null).noAutogen();
    public static final MaterialShapes ONLY_ORE = new MaterialShapes(0, null, null, "ore").noAutogen();
    public static final MaterialShapes ORE = new MaterialShapes(0, null, null, "ore", "oreNether").noAutogen();
    public static final MaterialShapes ORENETHER = new MaterialShapes(0, null, null, "oreNether").noAutogen();

    /** 1/72 of an ingot, allows the ingot to be divisible through 2, 4, 6, 8, 9, 12, 24 and 36 */
    public static final MaterialShapes QUANTUM = new MaterialShapes(1, "quantum", "quanta");
    public static final MaterialShapes NUGGET = new MaterialShapes(8, "nugget", "nuggets", "nugget", "tiny");
    public static final MaterialShapes TINY = new MaterialShapes(NUGGET.quantity, null, null, "tiny").noAutogen();
    public static final MaterialShapes FRAGMENT = new MaterialShapes(8, "ore_fragment", "ore_fragments", "bedrockorefragment");
    public static final MaterialShapes DUSTTINY = new MaterialShapes(NUGGET.quantity, "tiny_dust", "tiny_dusts", "dustTiny");
    public static final MaterialShapes WIRE = new MaterialShapes(9, "wire", "wires", "wireFine");
    public static final MaterialShapes BOLT = new MaterialShapes(9, "bolt", "bolts", "bolt");
    public static final MaterialShapes BILLET = new MaterialShapes(NUGGET.quantity * 6, "billet", "billets", "billet");
    public static final MaterialShapes INGOT = new MaterialShapes(NUGGET.quantity * 9, "ingot", "ingots", "ingot");
    public static final MaterialShapes GEM = new MaterialShapes(INGOT.quantity, "gem", "gems", "gem");
    public static final MaterialShapes CRYSTAL = new MaterialShapes(INGOT.quantity, "crystal", "crystals", "crystal");
    public static final MaterialShapes DUST = new MaterialShapes(INGOT.quantity, "dust", "dusts", "dust");
    public static final MaterialShapes DENSEWIRE = new MaterialShapes(INGOT.quantity, "dense_wire", "dense_wires", "wireDense");
    public static final MaterialShapes PLATE = new MaterialShapes(INGOT.quantity, "plate", "plates", "plate");
    public static final MaterialShapes CASTPLATE = new MaterialShapes(INGOT.quantity * 3, "plate_triple", "plates_triple", "plateTriple");
    public static final MaterialShapes WELDEDPLATE = new MaterialShapes(INGOT.quantity * 6, "plate_sextuple", "plates_sextuple", "plateSextuple");
    public static final MaterialShapes SHELL = new MaterialShapes(INGOT.quantity * 4, "shell", "shells", "shell");
    public static final MaterialShapes PIPE = new MaterialShapes(INGOT.quantity * 3, "pipe", "pipes", "ntmpipe");
    /** Pure recipe-quantity unit (crucible "quartz debris" cost) - never backs a real item. */
    public static final MaterialShapes QUART = new MaterialShapes(162, null, null);
    public static final MaterialShapes BLOCK = new MaterialShapes(INGOT.quantity * 9, "block", "storage_blocks", "block");
    public static final MaterialShapes HEAVY_COMPONENT = new MaterialShapes(CASTPLATE.quantity * 256, "heavy_component", "heavy_components", "componentHeavy");
    public static final MaterialShapes PART = new MaterialShapes(3, "part", "parts", "part");

    public static final MaterialShapes LIGHTBARREL = new MaterialShapes(INGOT.quantity * 3, "light_barrel", "light_barrels", "barrelLight");
    public static final MaterialShapes HEAVYBARREL = new MaterialShapes(INGOT.quantity * 6, "heavy_barrel", "heavy_barrels", "barrelHeavy");
    public static final MaterialShapes LIGHTRECEIVER = new MaterialShapes(INGOT.quantity * 4, "light_receiver", "light_receivers", "receiverLight");
    public static final MaterialShapes HEAVYRECEIVER = new MaterialShapes(INGOT.quantity * 9, "heavy_receiver", "heavy_receivers", "receiverHeavy");
    public static final MaterialShapes MECHANISM = new MaterialShapes(INGOT.quantity * 4, "gun_mechanism", "gun_mechanisms", "gunMechanism");
    public static final MaterialShapes STOCK = new MaterialShapes(INGOT.quantity * 4, "stock", "stocks", "stock");
    public static final MaterialShapes GRIP = new MaterialShapes(INGOT.quantity * 2, "grip", "grips", "grip");

    public boolean noAutogen = false;
    private final int quantity;
    /** Historical Forge ore-dict prefixes from CE, retained only for cross-referencing CE's item names. */
    public final String[] prefixes;
    /** Singular snake_case registry-name token, e.g. "ingot". Null for technical/compat-only shapes. */
    public final String registryName;
    /** Plural common-tag folder, e.g. "ingots" (tag id becomes {@code c:<tagFolder>/<material>}). Null if this shape has no common tag. */
    public final String tagFolder;

    private MaterialShapes(int quantity, String registryName, String tagFolder, String... legacyPrefixes) {
        this.quantity = quantity;
        this.registryName = registryName;
        this.tagFolder = tagFolder;
        this.prefixes = legacyPrefixes;

        for (String prefix : legacyPrefixes) {
            prefixByName.put(prefix, this);
        }
        if (registryName != null) {
            shapeByRegistryName.put(registryName, this);
        }
        allShapes.add(this);
    }

    /** Disables recipe autogen for special cases like compatibility prefixes (TINY, ORENETHER), technical prefixes (ANY) or prefixes that have to be handled manually (ORE) */
    public MaterialShapes noAutogen() {
        this.noAutogen = true;
        return this;
    }

    public int q(int amount) {
        return this.quantity * amount;
    }

    /** eg rails: INGOT.q(6, 16) since the recipe uses 6 iron ingots producing 16 individual rail blocks */
    public int q(int unitsUsed, int itemsProduced) {
        return this.quantity * unitsUsed / itemsProduced;
    }

    /**
     * Builds the item registry name for this shape of the given material, e.g. shape INGOT of
     * material "iron" -> "iron_ingot". Only valid for shapes that back a real item.
     */
    public String buildRegistryName(NTMMaterial mat) {
        if (registryName == null) {
            throw new IllegalStateException("Shape has no registry name (technical/compat-only shape), cannot build an item id from it");
        }
        return mat.getRegistryName() + "_" + registryName;
    }

    /** The {@code c:<tagFolder>/<material>} common item tag for this shape of the given material. */
    public TagKey<Item> commonTag(NTMMaterial mat) {
        if (tagFolder == null) {
            throw new IllegalStateException("Shape has no common tag folder (technical/compat-only shape)");
        }
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", tagFolder + "/" + mat.getRegistryName()));
    }
}
