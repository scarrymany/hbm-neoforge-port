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
 * <b>{@code BaseBarrel}.</b> CE never registers a plain {@code BaseBarrel} instance itself - only its
 * Red/Yellow (explosive/nuclear-waste content) subclasses, both deferred to Phase 2 per
 * {@link BaseBarrel}'s own class javadoc and this area's research report. Since the shell class is
 * documented there as Phase-1-safe on its own, and this pass is what makes the class compile into
 * the game, one plain generic barrel ("barrel") is registered here with the same hardness/resistance
 * CE gives every barrel subclass, so the block exists for the explosive/waste subclasses to extend
 * once Phase 2 lands, per {@link BaseBarrel}'s "left open for those future subclasses to extend" note.
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
    public static Supplier<BlockEntityType<BlockSupplyCrate.SupplyCrateBlockEntity>> SUPPLY_CRATE_ENTITY_TYPE;
    public static Supplier<BlockEntityType<BlockSkeletonHolder.SkeletonHolderBlockEntity>> SKELETON_HOLDER_ENTITY_TYPE;

    private GenericCrateBlocks() {
    }

    public static void registerAll() {
        registerBarrel();
        registerCrates();
        registerLootContainers();
    }

    /** See the class javadoc's {@code BaseBarrel} note: CE itself never registers this base shape. */
    private static void registerBarrel() {
        registerBlock("barrel", () -> new BaseBarrel(BlockBehaviour.Properties.of().strength(0.5F, 2.5F).sound(SoundType.METAL)),
                ModCreativeTabs.BLOCKS);
    }

    private static void registerCrates() {
        registerBlock("crate", () -> new BlockCrate(crateProps(SoundType.WOOD), BlockCrate.Type.STANDARD), ModCreativeTabs.CONSUMABLE);
        registerBlock("crate_weapon", () -> new BlockCrate(crateProps(SoundType.WOOD), BlockCrate.Type.WEAPON), ModCreativeTabs.CONSUMABLE);
        registerBlock("crate_lead", () -> new BlockCrate(crateProps(SoundType.METAL), BlockCrate.Type.LEAD), ModCreativeTabs.CONSUMABLE);
        registerBlock("crate_metal", () -> new BlockCrate(crateProps(SoundType.METAL), BlockCrate.Type.METAL), ModCreativeTabs.CONSUMABLE);
        // CE registers crate_red with setCreativeTab(null).
        registerBlock("crate_red", () -> new BlockCrate(crateProps(SoundType.METAL), BlockCrate.Type.RED), null);

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
        LOOT_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("deco_loot",
                () -> BlockEntityType.Builder.of(BlockLoot.LootBlockEntity::new, lootBlock.get()).build(null));

        // CE: setCreativeTab(MainRegistry.missileTab).setHardness(1.0F).setResistance(2.5F)
        DeferredBlock<BlockSupplyCrate> supplyCrateBlock = registerBlock("crate_supply",
                () -> new BlockSupplyCrate(BlockBehaviour.Properties.of().strength(SMALL_CRATE_HARDNESS, SMALL_CRATE_RESISTANCE).sound(SoundType.WOOD)),
                ModCreativeTabs.MISSILE);
        SUPPLY_CRATE_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crate_supply",
                () -> BlockEntityType.Builder.of(BlockSupplyCrate.SupplyCrateBlockEntity::new, supplyCrateBlock.get()).build(null));

        // CE: setCreativeTab(null).setHardness(2.0F).setResistance(10.0F)
        DeferredBlock<BlockSkeletonHolder> skeletonHolderBlock = registerBlock("skeleton_holder",
                () -> new BlockSkeletonHolder(BlockBehaviour.Properties.of().strength(2.0F, CRATE_RESISTANCE)), null);
        SKELETON_HOLDER_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("skeleton_holder",
                () -> BlockEntityType.Builder.of(BlockSkeletonHolder.SkeletonHolderBlockEntity::new, skeletonHolderBlock.get()).build(null));
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
