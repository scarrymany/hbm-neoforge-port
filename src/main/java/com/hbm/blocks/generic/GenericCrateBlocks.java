package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Table-driven registration for CE's crate/barrel/loot-container family (upstream hbm-ce
 * {@code ModBlocks.java} {@code BaseBarrel}/{@code BlockCrate}/{@code BlockAmmoCrate}/
 * {@code BlockCanCrate}/{@code BlockJungleCrate}/{@code BlockLoot}/{@code BlockSupplyCrate}/
 * {@code BlockSkeletonHolder} instances) - see {@code docs/phase1/blocks_generic.md}'s "Crates,
 * barrels, loot containers" section. Mirrors {@link com.hbm.blocks.OreBlocks}'s
 * table-driven-{@code registerAll()} shape.
 * <p>
 * <b>{@code BaseBarrel} / {@link RedBarrel} / {@link YellowBarrel}.</b> CE never registers a plain
 * {@code BaseBarrel} — only Red/Pink/LOX/Taint ({@code RedBarrel}) and
 * {@code yellow_barrel}/{@code vitrified_barrel} ({@code YellowBarrel}).
 * <p>
 * <b>{@link BlockCrate}/{@link BlockAmmoCrate}/{@link BlockCanCrate}/{@link BlockJungleCrate}'s empty
 * loot pools.</b> Each of those classes' own javadoc already documents why their drop pools are wired
 * up empty (the real CE drops are owned by not-yet-ported items/weapons areas); this class only
 * supplies the block shells CE itself registers (ids, hardness, sound, tab), not loot content.
 * <p>
 * <b>Null creative tab.</b> CE registers {@code crate_red}, {@code deco_loot} and
 * {@code skeleton_holder} with {@code setCreativeTab(null)} (hidden from the creative inventory,
 * meant to be placed only via loot tables/world gen/commands); those three skip
 * {@link CreativeTabContents#add} here but still get a real {@link BlockItem} so they remain
 * placeable.
 */
public final class GenericCrateBlocks {

    private static final float CRATE_HARDNESS = 5.0F;
    private static final float CRATE_RESISTANCE = 10.0F;
    private static final float SMALL_CRATE_HARDNESS = 1.0F;
    private static final float SMALL_CRATE_RESISTANCE = 2.5F;

    public static Supplier<BlockEntityType<BlockLoot.LootBlockEntity>> LOOT_ENTITY_TYPE;
    public static Supplier<BlockEntityType<BlockPedestal.PedestalBlockEntity>> PEDESTAL_ENTITY_TYPE;
    public static Supplier<BlockEntityType<BlockSupplyCrate.SupplyCrateBlockEntity>> SUPPLY_CRATE_ENTITY_TYPE;
    public static Supplier<BlockEntityType<BlockSkeletonHolder.SkeletonHolderBlockEntity>> SKELETON_HOLDER_ENTITY_TYPE;

    /**
     * Public accessor for the registered {@code crate_supply} block, added by the Phase 4
     * (entities_vehicles_aircraft) package so {@code EntityParachuteCrate} can place it on landing -
     * previously only a local variable inside {@link #registerLootContainers()} (see
     * {@code com.hbm.items.tool.ItemCrateCaller}'s javadoc, which flagged this exact gap for the
     * crate family in general). A lazy accessor method, not a static field initializer, per this
     * port's established DeferredBlock-forward-reference rule.
     */
    public static DeferredBlock<BlockSupplyCrate> crateSupply() {
        return CRATE_SUPPLY;
    }

    /**
     * Public accessors for the plain {@code crate}/{@code crate_weapon}/{@code crate_metal}/
     * {@code crate_lead}/{@code crate_red} blocks registered by {@link #registerCrates()}, added
     * during the Phase 4 review pass so {@code com.hbm.items.tool.ItemCrateCaller} (previously
     * stubbed pending exactly this - see its own javadoc) can finally place them. Lazy accessor
     * methods, not static field initializers, per this port's established DeferredBlock-forward-
     * reference rule.
     */
    public static DeferredBlock<BlockCrate> crateStandard() {
        return CRATE_STANDARD;
    }

    public static DeferredBlock<BlockCrate> crateWeapon() {
        return CRATE_WEAPON;
    }

    public static DeferredBlock<BlockCrate> crateMetal() {
        return CRATE_METAL;
    }

    public static DeferredBlock<BlockCrate> crateLead() {
        return CRATE_LEAD;
    }

    public static DeferredBlock<BlockCrate> crateRed() {
        return CRATE_RED;
    }

    public static DeferredBlock<BlockPedestal> pedestal() {
        return PEDESTAL;
    }

    public static DeferredBlock<BlockLoot> decoLoot() {
        return DECO_LOOT;
    }

    public static DeferredBlock<RedBarrel> RED_BARREL;
    public static DeferredBlock<RedBarrel> PINK_BARREL;
    public static DeferredBlock<RedBarrel> LOX_BARREL;
    public static DeferredBlock<RedBarrel> TAINT_BARREL;
    public static DeferredBlock<YellowBarrel> YELLOW_BARREL;
    public static DeferredBlock<YellowBarrel> VITRIFIED_BARREL;

    private static DeferredBlock<BlockSupplyCrate> CRATE_SUPPLY;
    private static DeferredBlock<BlockCrate> CRATE_STANDARD;
    private static DeferredBlock<BlockCrate> CRATE_WEAPON;
    private static DeferredBlock<BlockCrate> CRATE_METAL;
    private static DeferredBlock<BlockCrate> CRATE_LEAD;
    private static DeferredBlock<BlockCrate> CRATE_RED;
    private static DeferredBlock<BlockPedestal> PEDESTAL;
    private static DeferredBlock<BlockLoot> DECO_LOOT;

    private GenericCrateBlocks() {
    }

    public static void registerAll() {
        registerBarrel();
        registerCrates();
        registerLootContainers();
    }

    /** CE ModBlocks.java:751-756 — RedBarrel/YellowBarrel, nukeTab, 0.5F/2.5F. */
    private static void registerBarrel() {
        // Fresh Properties per block — BlockBehaviour.Properties is single-use.
        registerBlock("barrel", () -> new BaseBarrel(barrelProps()), ModCreativeTabs.BLOCKS);
        RED_BARREL = registerBlock("red_barrel", () -> new RedBarrel(barrelProps(), RedBarrel.Kind.RED), ModCreativeTabs.NUKE);
        PINK_BARREL = registerBlock("pink_barrel", () -> new RedBarrel(barrelProps(), RedBarrel.Kind.PINK), ModCreativeTabs.NUKE);
        LOX_BARREL = registerBlock("lox_barrel", () -> new RedBarrel(barrelProps(), RedBarrel.Kind.LOX), ModCreativeTabs.NUKE);
        TAINT_BARREL = registerBlock("taint_barrel", () -> new RedBarrel(barrelProps(), RedBarrel.Kind.TAINT), ModCreativeTabs.NUKE);
        YELLOW_BARREL = registerBlock("yellow_barrel", () -> new YellowBarrel(barrelProps()), ModCreativeTabs.NUKE);
        // CE ModBlocks.java:752 — same YellowBarrel class, idle rad 0.5/5 (see YellowBarrel.tick).
        VITRIFIED_BARREL = registerBlock("vitrified_barrel", () -> new YellowBarrel(barrelProps()), ModCreativeTabs.NUKE);
    }

    private static void registerCrates() {
        CRATE_STANDARD = registerBlock("crate", () -> new BlockCrate(crateProps(SoundType.WOOD), BlockCrate.Type.STANDARD), ModCreativeTabs.CONSUMABLE);
        CRATE_WEAPON = registerBlock("crate_weapon", () -> new BlockCrate(crateProps(SoundType.WOOD), BlockCrate.Type.WEAPON), ModCreativeTabs.CONSUMABLE);
        CRATE_LEAD = registerBlock("crate_lead", () -> new BlockCrate(crateProps(SoundType.METAL), BlockCrate.Type.LEAD), ModCreativeTabs.CONSUMABLE);
        CRATE_METAL = registerBlock("crate_metal", () -> new BlockCrate(crateProps(SoundType.METAL), BlockCrate.Type.METAL), ModCreativeTabs.CONSUMABLE);
        // CE registers crate_red with setCreativeTab(null).
        CRATE_RED = registerBlock("crate_red", () -> new BlockCrate(crateProps(SoundType.METAL), BlockCrate.Type.RED), null);

        registerBlock("crate_can", () -> new BlockCanCrate(BlockBehaviour.Properties.of().strength(SMALL_CRATE_HARDNESS, SMALL_CRATE_RESISTANCE).sound(SoundType.WOOD)),
                ModCreativeTabs.CONSUMABLE);
        registerBlock("crate_jungle", () -> new BlockJungleCrate(BlockBehaviour.Properties.of().strength(SMALL_CRATE_HARDNESS, SMALL_CRATE_RESISTANCE).sound(SoundType.STONE)),
                ModCreativeTabs.CONSUMABLE);
        registerBlock("crate_ammo", () -> new BlockAmmoCrate(BlockBehaviour.Properties.of().strength(SMALL_CRATE_HARDNESS, SMALL_CRATE_RESISTANCE).sound(SoundType.METAL)),
                ModCreativeTabs.CONSUMABLE);
    }

    private static void registerLootContainers() {
        // CE: setCreativeTab(null).setHardness(0.0F).setResistance(0.0F)
        DeferredBlock<BlockLoot> lootBlock = registerBlock("deco_loot",
                () -> new BlockLoot(BlockBehaviour.Properties.of().strength(0.0F, 0.0F).noOcclusion()), null);
        DECO_LOOT = lootBlock;
        LOOT_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("deco_loot",
                () -> BlockEntityType.Builder.of(BlockLoot.LootBlockEntity::new, lootBlock.get()).build(null));

        // CE: pedestal for red-room loot display, tab=null (world-gen only)
        DeferredBlock<BlockPedestal> pedestalBlock = registerBlock("pedestal",
                () -> new BlockPedestal(BlockBehaviour.Properties.of().strength(5.0F, 10.0F).noOcclusion()), null);
        PEDESTAL = pedestalBlock;
        PEDESTAL_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("pedestal",
                () -> BlockEntityType.Builder.of(BlockPedestal.PedestalBlockEntity::new, pedestalBlock.get()).build(null));

        // CE: setCreativeTab(MainRegistry.missileTab).setHardness(1.0F).setResistance(2.5F)
        DeferredBlock<BlockSupplyCrate> supplyCrateBlock = registerBlock("crate_supply",
                () -> new BlockSupplyCrate(BlockBehaviour.Properties.of().strength(SMALL_CRATE_HARDNESS, SMALL_CRATE_RESISTANCE).sound(SoundType.WOOD)),
                ModCreativeTabs.MISSILE);
        CRATE_SUPPLY = supplyCrateBlock;
        SUPPLY_CRATE_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crate_supply",
                () -> BlockEntityType.Builder.of(BlockSupplyCrate.SupplyCrateBlockEntity::new, supplyCrateBlock.get()).build(null));

        // CE: setCreativeTab(null).setHardness(2.0F).setResistance(10.0F)
        DeferredBlock<BlockSkeletonHolder> skeletonHolderBlock = registerBlock("skeleton_holder",
                () -> new BlockSkeletonHolder(BlockBehaviour.Properties.of().strength(2.0F, CRATE_RESISTANCE)), null);
        SKELETON_HOLDER_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("skeleton_holder",
                () -> BlockEntityType.Builder.of(BlockSkeletonHolder.SkeletonHolderBlockEntity::new, skeletonHolderBlock.get()).build(null));
    }

    private static BlockBehaviour.Properties barrelProps() {
        return BlockBehaviour.Properties.of().strength(0.5F, 2.5F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties crateProps(SoundType sound) {
        return BlockBehaviour.Properties.of().strength(CRATE_HARDNESS, CRATE_RESISTANCE).sound(sound);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory, @Nullable ResourceKey<CreativeModeTab> tab) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        if (tab != null) {
            CreativeTabContents.add(tab, block);
        }
        return block;
    }
}
