package com.hbm.blocks;

import com.hbm.blocks.generic.BlockCluster;
import com.hbm.blocks.generic.BlockDepthOre;
import com.hbm.blocks.generic.BlockHazard;
import com.hbm.blocks.generic.BlockNTMOre;
import com.hbm.blocks.generic.BlockNetherCoal;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.ModItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * <b>{@link com.hbm.blocks.IOreType} drops.</b> CE {@code OreEnum} entries whose flatten
 * items exist are wired here ({@code sulfur}/{@code niter}/{@code fluorite}/{@code lignite}/
 * {@code ingot_asbestos}/{@code chunk_ore_rare}/{@code cinnabar}/{@code oil_tar_crude}/
 * {@code nugget_zirconium}/{@code ingot_phosphorus}+{@code powder_fire}, plus the already-wired
 * cobalt/coltan/neodymium/alexandrite/nitan/cluster/meteor/basalt-gem paths).
 * {@code ore_nether_cobalt} keeps CE's own null oreType. {@code ore_gneiss_rare} has no
 * {@code OreEnum} in CE ({@code ModBlocks.java:371}) — do not invent one.
 * Fortune on {@link BlockNTMOre#getDrops} stays {@code 0} like the existing cobalt/coltan
 * path (LootParams fortune not threaded).
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
 * shade rendering solution per the report's section 5, independent of this registration pass), and
 * {@code ore_oil_empty}/{@code ore_oil_sand} (companion decorative states of the oil ore that are
 * plain {@code BlockBase}/{@code BlockFallingBase}, not part of the {@code BlockNTMOre} family).
 * {@code block_meteor_molten} - CE's stateful tick-driven molten-to-cobble transition block, needed
 * as one of the meteor world-gen hull-shell blocks per
 * docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 2a - <b>is</b> registered here now
 * ({@code registerMeteorOres()} below) as a plain {@link BlockHazard} instance, matching this port's
 * already-established simplification for every other {@code BlockHazard}-family block - the
 * identity-gated {@code updateTick}/{@code onPlayerDestroy} molten-to-cobble/molten-to-lava
 * transitions CE's 1.12 {@code BlockHazard} performs for this one block are not reproduced (a
 * cosmetic tick concern unrelated to world-gen shape placement, out of this pass).
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
        registerSellafieldOres();
        registerImpactOres();
    }

    private static void registerOverworldOres() {
        ore("ore_thorium", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_titanium", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_sulfur", 1, STD_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("sulfur"), OreEnumUtil::base2Rand3Fortune));
        ore("ore_niter", 1, STD_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("niter"), OreEnumUtil::base1Rand2Fortune));
        ore("ore_copper", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_tungsten", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_aluminium", 1, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_fluorite", 1, STD_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("fluorite"), OreEnumUtil::base2Rand3Fortune));
        ore("ore_lead", 2, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_beryllium", 2, STD_HARDNESS, 15.0F, null);
        ore("ore_lignite", 0, STD_HARDNESS, 15.0F,
                oreType(hbmItem("lignite"), OreEnumUtil::vanillaFortune));
        ore("ore_asbestos", 1, 6, STD_HARDNESS, 15.0F,
                oreType(() -> IngotNuggetItems.INGOT_ASBESTOS.get(), OreEnumUtil::vanillaFortune));
        ore("ore_rare", 2, 12, STD_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("chunk_ore_rare"), OreEnumUtil::vanillaFortune));
        ore("ore_cobalt", 3, 15, STD_HARDNESS, STD_RESISTANCE,
                oreType(() -> PlateCrystalWasteItems.FRAGMENT_COBALT.get(), OreEnumUtil::cobaltAmount));
        ore("ore_cinnabar", 1, STD_HARDNESS, STD_RESISTANCE,
                oreType(() -> BilletPowderItems.CINNABAR.get(), OreEnumUtil::base1Rand2Fortune));
        ore("ore_coltan", 3, 20, 15.0F, STD_RESISTANCE,
                oreType(() -> PlateCrystalWasteItems.FRAGMENT_COLTAN.get(), OreEnumUtil::vanillaFortune));
        ore("ore_australium", 4, 100, STD_HARDNESS, STD_RESISTANCE, null);
        ore("ore_schrabidium", 3, 300, SCHRABIDIUM_HARDNESS, SCHRABIDIUM_RESISTANCE, null);
        ore("ore_oil", 1, STD_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("oil_tar_crude"), OreEnumUtil::const1));
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
        ore("ore_gneiss_asbestos", 2, GNEISS_HARDNESS, STD_RESISTANCE,
                oreType(() -> IngotNuggetItems.INGOT_ASBESTOS.get(), OreEnumUtil::vanillaFortune));
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
        ore("ore_nether_sulfur", 1, NETHER_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("sulfur"), OreEnumUtil::base2Rand3Fortune));
        ore("ore_nether_fire", 1, NETHER_HARDNESS, STD_RESISTANCE,
                oreType((state, rand) -> rand.nextInt(10) == 0
                        ? new ItemStack(IngotNuggetItems.INGOT_PHOSPHORUS.get())
                        : new ItemStack(BilletPowderItems.POWDER_FIRE.get()),
                        OreEnumUtil::vanillaFortune));
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

        // CE: new BlockHazard(Material.ROCK, "block_meteor_molten").setTickRandomly(true)
        // .setLightLevel(0.75F).setHardness(15.0F).setResistance(360.0F) - no setHarvestLevel call
        // unlike its siblings above, so harvestLevel 0 (breakable by hand) is CE-faithful, not an
        // omission. Needed by docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 2a's
        // MeteoriteGenerator hull-shell placement (hull type 0/3) - see this class's own javadoc for
        // what CE tick behavior is/isn't preserved.
        registerBlock("block_meteor_molten",
                () -> new BlockHazard(oreProps(METEOR_HARDNESS, METEOR_RESISTANCE, 0).lightLevel(state -> 11)),
                ModCreativeTabs.RESOURCE);
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
        depthOre("ore_depth_cinnabar",
                oreType(() -> BilletPowderItems.CINNABAR.get(), OreEnumUtil::base1Rand2Fortune));
        depthOre("ore_depth_zirconium",
                oreType(() -> IngotNuggetItems.NUGGET_ZIRCONIUM.get(), OreEnumUtil::base2Rand2Fortune));
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
        ore("basalt_ore_sulfur", 0, STD_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("sulfur"), OreBlocks::basaltQuantity));
        ore("basalt_ore_fluorite", 0, STD_HARDNESS, STD_RESISTANCE,
                oreType(hbmItem("fluorite"), OreBlocks::basaltQuantity));
        ore("basalt_ore_asbestos", 0, STD_HARDNESS, STD_RESISTANCE,
                oreType(() -> IngotNuggetItems.INGOT_ASBESTOS.get(), OreBlocks::basaltQuantity));
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

    /**
     * CE {@code ModBlocks.java}:309-313 — {@code BlockSellafieldOre} drops
     * ({@code BlockSellafieldOre.java}:50-55). Shade/variant rendering deferred (Phase 5).
     */
    private static void registerSellafieldOres() {
        ore("ore_sellafield_diamond", 2, 5, STD_HARDNESS, 6.0F,
                oreType(() -> Items.DIAMOND, OreBlocks::basaltQuantity));
        ore("ore_sellafield_emerald", 2, 5, STD_HARDNESS, 6.0F,
                oreType(() -> Items.EMERALD, OreBlocks::basaltQuantity));
        ore("ore_sellafield_radgem", 2, 5, STD_HARDNESS, 6.0F,
                oreType(() -> PlateCrystalWasteItems.GEM_RAD.get(), OreBlocks::basaltQuantity));
        ore("ore_sellafield_schrabidium", 3, STD_HARDNESS, 6.0F, null);
        ore("ore_sellafield_uranium_scorched", 1, STD_HARDNESS, 6.0F, null);
        registerBlock("sellafield_bedrock",
                () -> new BlockBase(BlockBehaviour.Properties.of().strength(-1.0F, 6_000_000.0F).sound(SoundType.STONE)),
                ModCreativeTabs.RESOURCE);
    }

    /**
     * CE {@code ModBlocks.java}:353 is {@code BlockFissure}. Casing only.
     * TODO(CE: BlockFissure.java:1-105): {@code TileEntityFissure} sends LAVA UP.
     * Blocked by {@code volcanic_lava_block}/{@code rad_lava_block} + CE {@code IFluidStandardSender}
     * ≠ port MK2. Do not invent lava ids. {@code ore_tektite_osmiridium} is a plain cube.
     */
    private static void registerImpactOres() {
        registerBlock("ore_volcano",
                () -> new BlockBase(BlockBehaviour.Properties.of()
                        .strength(-1.0F, 1_000_000.0F).sound(SoundType.STONE).lightLevel(state -> 15)),
                ModCreativeTabs.BLOCKS);
        registerBlock("ore_tektite_osmiridium",
                () -> new BlockBase(oreProps(2.5F, 20.0F, 0)),
                ModCreativeTabs.RESOURCE);
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

    private static Supplier<Item> hbmItem(String id) {
        return () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
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
