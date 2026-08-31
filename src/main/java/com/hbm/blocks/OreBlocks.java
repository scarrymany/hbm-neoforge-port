package com.hbm.blocks;

import com.hbm.blocks.generic.BlockCluster;
import com.hbm.blocks.generic.BlockDepthOre;
import com.hbm.blocks.generic.BlockNTMOre;
import com.hbm.blocks.generic.BlockNetherCoal;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.ModItems;
import com.hbm.items.PlateCrystalWasteItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Table-driven registration for CE's ore/cluster/depth-ore family (upstream hbm-ce
 * {@code ModBlocks.java}, {@code BlockNTMOre}/{@code BlockCluster}/{@code BlockDepthOre}/
 * {@code BlockOutgas} instances) plus the two genuine 1.12-metadata-flattening cases in this area,
 * {@code BlockOreBasalt} (5 variants) and {@code BlockBiomeStone} (2 variants) - see
 * {@code docs/phase1/modblocks_generative.md} section 1a.
 * <p>
 * <b>{@link com.hbm.blocks.IOreType} drop-item availability.</b> CE's real ore drops
 * ({@code sulfur}, {@code niter}, {@code fluorite}, {@code lignite}, {@code ingot_asbestos},
 * {@code chunk_ore}, {@code cinnabar}, {@code nugget_zirconium}, {@code oil_tar},
 * {@code ingot_phosphorus}, and the {@code ItemPool}-driven meteorite treasure table) are either not
 * yet ported by any Phase 1 items area, or exist only as package-private fields in
 * {@code com.hbm.items.IngotNuggetItems} not reachable from this package. Rather than invent a
 * public accessor on another area's file (out of this area's edit scope) or guess at a substitute
 * item, every such ore keeps CE's own harvest level/hardness/resistance/xp but registers with a
 * {@code null} {@link com.hbm.blocks.IOreType}, which {@link BlockNTMOre}/{@link BlockDepthOre}
 * already treat as "fall back to the ordinary self-drop" - exactly CE's own behavior for
 * {@code ore_australium}, {@code ore_schrabidium} and {@code ore_depth_borax}, which pass a null ore
 * type for the same reason (no distinct drop item). Every ore whose CE drop item already has a
 * public registry entry in {@link PlateCrystalWasteItems}/{@link BilletPowderItems} (cobalt, coltan,
 * neodymium, alexandrite, nitan, the five ore-cluster crystals, meteorite fragments, the meteor
 * block's plate_dalekanium jackpot, and the basalt gem/molysite variants) is wired to that real item.
 * {@code ore_nether_cobalt} keeps CE's own null oreType (a confirmed CE oversight documented in the
 * research report, preserved rather than "fixed").
 * <p>
 * <b>Harvest level.</b> Modern Minecraft expresses tool-tier requirements as block tags
 * ({@code minecraft:needs_iron_tool}, etc.), not a per-block integer - that tag assignment is a
 * datagen follow-up. What is preserved now is CE's coarse harvest-level-zero-vs-nonzero distinction
 * via {@link BlockBehaviour.Properties#requiresCorrectToolForDrops()}.
 * <p>
 * <b>Deliberately not ported here</b> (see the class-level exclusions in the research report and this
 * area's final summary): {@code BlockBedrockOre}/{@code BlockBedrockOreTE} (the whole bedrock-tier
 * family - the TE variant is explicitly out of Phase 1, and the plain variant's only real content is
 * an identity-checked drill drop not worth splitting from its TE sibling), {@code ore_meteor}
 * ({@code BlockMeteorOre}, a {@code BlockEnumMeta<EnumMeteorType>} 5-variant metadata-multi block the
 * research report did not flag as a flattening case and this pass did not have a confirmed drop
 * mapping for), {@code ore_sellafield_*}/{@code BlockSellafieldOre} (needs its own random-variant/
 * shade rendering solution per the report's section 5, independent of this registration pass),
 * {@code block_meteor_molten} (a stateful tick-driven molten-to-cobble transition block, not a
 * resource-drop block), and {@code ore_oil_empty}/{@code ore_oil_sand} (companion decorative states
 * of the oil ore that are plain {@code BlockBase}/{@code BlockFallingBase}, not part of the
 * {@code BlockNTMOre} family).
 */
public final class OreBlocks {

    private static final float STD_HARDNESS = 5.0F;
    private static final float STD_RESISTANCE = 10.0F;
    private static final float GNEISS_HARDNESS = 1.5F;
    private static final float NETHER_HARDNESS = 0.4F;
    private static final float SCHRABIDIUM_RESISTANCE = 600.0F;
    private static final float SCHRABIDIUM_HARDNESS = 15.0F;
    private static final float METEOR_HARDNESS = 15.0F;
    private static final float METEOR_RESISTANCE = 360.0F;
    private static final float DEPTH_RESISTANCE = 10.0F;
    private static final int METEOR_TREASURE_ONE_IN = 10;

    private static DeferredBlock<BlockNTMOre> blockMeteor;

    private OreBlocks() {
    }

    public static void registerAll() {
        registerOverworldOres();
        registerGneissOres();
        registerNetherOres();
        registerUraniumOres();
        registerMeteorOres();
        registerClusters();
        registerDepthOres();
        registerBasalt();
        registerBiomeStone();
    }

    private static void registerOverworldOres() {
        ore("ore_thorium", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_titanium", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_sulfur", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_niter", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_copper", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_tungsten", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_aluminium", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_fluorite", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_lead", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_beryllium", 2, STD_HARDNESS, 15.0F, null);
        ore("ore_lignite", 0, STD_HARDNESS, 15.0F, null);
        ore("ore_asbestos", 1, 6, STD_HARDNESS, 15.0F, null);
        ore("ore_rare", 2, 12, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_cobalt", 3, 15, STD_HARDNESS, STD_RESISTANCE,
                oreType(() -> PlateCrystalWasteItems.FRAGMENT_COBALT.get(), OreEnumUtil::cobaltAmount));
        ore("ore_cinnabar", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_coltan", 3, 20, 15.0F, STD_RESISTANCE,
                oreType(() -> PlateCrystalWasteItems.FRAGMENT_COLTAN.get(), OreEnumUtil::vanillaFortune));
        ore("ore_australium", 4, 100, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_schrabidium", 3, 300, SCHRABIDIUM_HARDNESS, SCHRABIDIUM_RESISTANCE, null);
        ore("ore_oil", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_tikite", 4, STD_HARDNESS, STD_RESISTANCE, null);

        registerBlock("waste_planks",
                () -> new BlockNTMOre(oreProps(0.5F, 2.5F, 2).sound(SoundType.WOOD), null),
                ModCreativeTabs.RESOURCE);
    }

    private static void registerGneissOres() {
        // Stratum substrate the (separate) ore-veins content package's SchistStratum-equivalent
        // world-gen paints in before any of the ore veins below get planted into it - see
        // docs/phase4/ore_veins_and_bedrock_ores.md Group C's "Blocking gap" note. CE:
        // new BlockBase(Material.ROCK, "stone_gneiss").setHardness(1.5F).setResistance(10.0F) - the
        // exact GNEISS_HARDNESS this class already anticipated (see the field's own declaration above).
        registerBlock("stone_gneiss", () -> new BlockBase(oreProps(GNEISS_HARDNESS, STD_RESISTANCE, 0)), ModCreativeTabs.RESOURCE);

        ore("ore_gneiss_iron", 1, GNEISS_HARDNESS, STD_RESISTANCE, null);
        ore("ore_gneiss_gold", 2, GNEISS_HARDNESS, STD_RESISTANCE, null);
        ore("ore_gneiss_copper", 1, GNEISS_HARDNESS, STD_RESISTANCE, null);
        ore("ore_gneiss_asbestos", 2, GNEISS_HARDNESS, STD_RESISTANCE, null);
        ore("ore_gneiss_lithium", 0, GNEISS_HARDNESS, STD_RESISTANCE, null);
        ore("ore_gneiss_schrabidium", 3, GNEISS_HARDNESS, STD_RESISTANCE, null);
        ore("ore_gneiss_rare", 3, GNEISS_HARDNESS, STD_RESISTANCE, null);
        ore("ore_gneiss_gas", 0, GNEISS_HARDNESS, STD_RESISTANCE, null);
        outgas("ore_gneiss_uranium", GNEISS_HARDNESS, STD_RESISTANCE);
        outgas("ore_gneiss_uranium_scorched", GNEISS_HARDNESS, STD_RESISTANCE);
    }

    private static void registerNetherOres() {
        // ore_nether_cobalt passes no IOreType in CE either - confirmed oversight (a richer
        // "cobaltNetherAmount" drop exists in OreEnumUtil but is never wired to this block),
        // preserved verbatim rather than silently improved.
        ore("ore_nether_cobalt", 3, NETHER_HARDNESS, STD_RESISTANCE, null);
        ore("ore_nether_tungsten", 2, NETHER_HARDNESS, STD_RESISTANCE, null);
        ore("ore_nether_sulfur", 1, NETHER_HARDNESS, STD_RESISTANCE, null);
        ore("ore_nether_fire", 1, NETHER_HARDNESS, STD_RESISTANCE, null);
        ore("ore_nether_plutonium", 3, NETHER_HARDNESS, STD_RESISTANCE, null);
        ore("ore_nether_schrabidium", 3, SCHRABIDIUM_HARDNESS, SCHRABIDIUM_RESISTANCE, null);
        outgas("ore_nether_uranium", NETHER_HARDNESS, STD_RESISTANCE);
        outgas("ore_nether_uranium_scorched", NETHER_HARDNESS, STD_RESISTANCE);

        // CE: new BlockNetherCoal(false, 5, true, "ore_nether_coal").setLightLevel(10F / 15F)
        // .setHardness(0.4F).setResistance(10.0F) - no harvest-level call, so no tool requirement
        // (harvestLevel 0). See BlockNetherCoal's own javadoc for what the 3 leading CE constructor
        // params actually configure and what is/isn't ported.
        registerBlock("ore_nether_coal",
                () -> new BlockNetherCoal(oreProps(NETHER_HARDNESS, STD_RESISTANCE, 0).lightLevel(state -> 10)),
                ModCreativeTabs.RESOURCE);
    }

    private static void registerUraniumOres() {
        outgas("ore_uranium", STD_HARDNESS, STD_RESISTANCE);
        outgas("ore_uranium_scorched", STD_HARDNESS, STD_RESISTANCE);
    }

    private static void registerMeteorOres() {
        IOreType meteorDrop = oreType(
                (state, rand) -> rand.nextInt(METEOR_TREASURE_ONE_IN) == 0
                        ? new ItemStack(PlateCrystalWasteItems.PLATE_DALEKANIUM.get())
                        : new ItemStack(blockMeteor.get()),
                OreEnumUtil::const1);
        blockMeteor = ore("block_meteor", 3, METEOR_HARDNESS, METEOR_RESISTANCE, meteorDrop);

        IOreType meteoriteFragment = oreType(() -> PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), OreEnumUtil::base1Rand3);
        ore("block_meteor_cobble", 2, 0, METEOR_HARDNESS, METEOR_RESISTANCE, meteoriteFragment);
        ore("block_meteor_broken", 1, 0, METEOR_HARDNESS, METEOR_RESISTANCE, meteoriteFragment);
        ore("block_meteor_treasure", 3, METEOR_HARDNESS, METEOR_RESISTANCE, null);
    }

    private static void registerClusters() {
        float hardness = STD_HARDNESS;
        float resistance = 35.0F;
        cluster("cluster_iron", hardness, resistance, oreType(() -> PlateCrystalWasteItems.CRYSTAL_IRON.get(), OreEnumUtil::vanillaFortune));
        cluster("cluster_titanium", hardness, resistance, oreType(() -> PlateCrystalWasteItems.CRYSTAL_TITANIUM.get(), OreEnumUtil::vanillaFortune));
        cluster("cluster_aluminium", hardness, resistance, oreType(() -> PlateCrystalWasteItems.CRYSTAL_ALUMINIUM.get(), OreEnumUtil::vanillaFortune));
        cluster("cluster_copper", hardness, resistance, oreType(() -> PlateCrystalWasteItems.CRYSTAL_COPPER.get(), OreEnumUtil::vanillaFortune));
    }

    private static void registerDepthOres() {
        depthOre("ore_depth_cinnabar", null);
        depthOre("ore_depth_zirconium", null);
        depthOre("ore_depth_borax", null);
        depthOre("ore_alexandrite", oreType(() -> PlateCrystalWasteItems.GEM_ALEXANDRITE.get(), OreEnumUtil::alexandriteAmount));
        depthOre("cluster_depth_iron", oreType(() -> PlateCrystalWasteItems.CRYSTAL_IRON.get(), OreEnumUtil::vanillaFortune));
        depthOre("cluster_depth_titanium", oreType(() -> PlateCrystalWasteItems.CRYSTAL_TITANIUM.get(), OreEnumUtil::vanillaFortune));
        depthOre("cluster_depth_tungsten", oreType(() -> PlateCrystalWasteItems.CRYSTAL_TUNGSTEN.get(), OreEnumUtil::vanillaFortune));
        depthOre("ore_depth_nether_neodymium", oreType(() -> PlateCrystalWasteItems.FRAGMENT_NEODYMIUM.get(), OreEnumUtil::base2Rand2Fortune));
        depthOre("ore_depth_nether_nitan", oreType(() -> BilletPowderItems.POWDER_NITAN_MIX.get(), OreEnumUtil::const1));
    }

    /** CE's own basalt drop-quantity formula: {@code rand.nextInt(fortune + 1)} + 1. */
    private static int basaltQuantity(BlockState state, int fortune, RandomSource rand) {
        return rand.nextInt(fortune + 1) + 1;
    }

    private static void registerBasalt() {
        ore("basalt_ore_sulfur", 0, STD_HARDNESS, STD_RESISTANCE, null);
        ore("basalt_ore_fluorite", 0, STD_HARDNESS, STD_RESISTANCE, null);
        ore("basalt_ore_asbestos", 0, STD_HARDNESS, STD_RESISTANCE, null);
        ore("basalt_ore_gem", 0, STD_HARDNESS, STD_RESISTANCE,
                oreType(() -> PlateCrystalWasteItems.GEM_VOLCANIC.get(), OreBlocks::basaltQuantity));
        ore("basalt_ore_molysite", 0, STD_HARDNESS, STD_RESISTANCE,
                oreType(() -> BilletPowderItems.POWDER_MOLYSITE.get(), OreBlocks::basaltQuantity));
    }

    private static void registerBiomeStone() {
        registerBlock("stone_biome_desert",
                () -> new BlockBase(oreProps(STD_HARDNESS, STD_RESISTANCE, 0)),
                ModCreativeTabs.BLOCKS);
        registerBlock("stone_biome_woodland",
                () -> new BlockBase(oreProps(STD_HARDNESS, STD_RESISTANCE, 0)),
                ModCreativeTabs.BLOCKS);
    }

    // ==================== construction helpers ====================

    private static DeferredBlock<BlockNTMOre> ore(String name, int harvestLevel, float hardness, float resistance, @Nullable IOreType oreType) {
        return registerBlock(name, () -> new BlockNTMOre(oreProps(hardness, resistance, harvestLevel), oreType), ModCreativeTabs.RESOURCE);
    }

    private static DeferredBlock<BlockNTMOre> ore(String name, int harvestLevel, int xp, float hardness, float resistance, @Nullable IOreType oreType) {
        return registerBlock(name, () -> new BlockNTMOre(oreProps(hardness, resistance, harvestLevel), oreType, xp), ModCreativeTabs.RESOURCE);
    }

    private static void outgas(String name, float hardness, float resistance) {
        registerBlock(name, () -> new BlockOutgas(oreProps(hardness, resistance, 1)), ModCreativeTabs.RESOURCE);
    }

    private static void cluster(String name, float hardness, float resistance, @Nullable IOreType oreType) {
        registerBlock(name, () -> new BlockCluster(oreProps(hardness, resistance, 1), oreType), ModCreativeTabs.RESOURCE);
    }

    /**
     * CE's {@code BlockDepth} base hardcodes {@code setBlockUnbreakable().setResistance(10.0F)} plus
     * pickaxe-tier-3 ({@code diamond}) as the harvest level for every depth-stratum block; the exact
     * tool tier is a block-tag (datagen) concern in modern Minecraft (see the class javadoc), so only
     * the coarse "needs the correct tool" requirement is expressed here.
     */
    private static void depthOre(String name, @Nullable IOreType oreType) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .strength(-1.0F, DEPTH_RESISTANCE)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
        registerBlock(name, () -> new BlockDepthOre(props, oreType), ModCreativeTabs.RESOURCE);
    }

    private static BlockBehaviour.Properties oreProps(float hardness, float resistance, int harvestLevel) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(hardness, resistance).sound(SoundType.STONE);
        return harvestLevel > 0 ? props.requiresCorrectToolForDrops() : props;
    }

    private static IOreType oreType(Supplier<Item> drop, IOreType.TriFunction<BlockState, Integer, RandomSource, Integer> quantity) {
        return oreType((state, rand) -> new ItemStack(drop.get()), quantity);
    }

    private static IOreType oreType(BiFunction<BlockState, RandomSource, ItemStack> drop, IOreType.TriFunction<BlockState, Integer, RandomSource, Integer> quantity) {
        return new IOreType() {
            @Override
            public BiFunction<BlockState, RandomSource, ItemStack> getDropFunction() {
                return drop;
            }

            @Override
            public TriFunction<BlockState, Integer, RandomSource, Integer> getQuantityFunction() {
                return quantity;
            }
        };
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory, ResourceKey<CreativeModeTab> tab) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(tab, block);
        return block;
    }
}
