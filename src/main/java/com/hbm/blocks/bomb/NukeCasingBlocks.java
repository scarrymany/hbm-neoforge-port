package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.CrashedBombBlock.EnumDudType;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.bomb.NukeCasingMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for the 9 concrete nuke casings + {@code NukeCustom} +
 * the 4 {@code CrashedBomb} dud variants, per {@code docs/phase3/bomb_blocks_and_detonators.md}
 * Section B. Mirrors {@code PowerGenBlocks}' shape exactly - block-entity-type registration lives in
 * the sibling {@link NukeCasingBlockEntities} class, {@link NukeCasingMenus}' {@code MenuType}s are
 * triggered from this class's {@link #registerAll()} too, so wiring this whole family into the game
 * needs exactly one call from {@code ModBlocks.register()} (see this task's wiring notes) - no other
 * shared file needs a direct edit.
 * <p>
 * CE's own hardness/resistance is never set on any of the 9 casing block constructors (confirmed by
 * reading every one in full) - real vanilla {@code Block} defaults (hardness/resistance 0) apply
 * there, reproduced here via a shared {@code strength(0.0F)}; a decorative detail, not load-bearing
 * for detonation behavior.
 */
public final class NukeCasingBlocks {

    private static final BlockBehaviour.Properties CASING_PROPS =
            BlockBehaviour.Properties.of().strength(0.0F).noOcclusion();
    private static final BlockBehaviour.Properties CRASHED_PROPS =
            BlockBehaviour.Properties.of().strength(0.0F).sound(SoundType.METAL).noOcclusion();

    public static DeferredBlock<NukeBoyBlock> NUKE_BOY;
    public static DeferredBlock<NukeGadgetBlock> NUKE_GADGET;
    public static DeferredBlock<NukeManBlock> NUKE_MAN;
    public static DeferredBlock<NukeMikeBlock> NUKE_MIKE;
    public static DeferredBlock<NukeTsarBlock> NUKE_TSAR;
    public static DeferredBlock<NukeN2Block> NUKE_N2;
    public static DeferredBlock<NukePrototypeBlock> NUKE_PROTOTYPE;
    public static DeferredBlock<NukeFleijaBlock> NUKE_FLEIJA;
    public static DeferredBlock<NukeBalefireBlock> NUKE_BALEFIRE;
    public static DeferredBlock<NukeBalefireBlock> NUKE_FSTBMB;
    public static DeferredBlock<NukeSoliniumBlock> NUKE_SOLINIUM;
    public static DeferredBlock<NukeCustomBlock> NUKE_CUSTOM;

    public static DeferredBlock<CrashedBombBlock> CRASHED_BOMB_BALEFIRE;
    public static DeferredBlock<CrashedBombBlock> CRASHED_BOMB_CONVENTIONAL;
    public static DeferredBlock<CrashedBombBlock> CRASHED_BOMB_NUKE;
    public static DeferredBlock<CrashedBombBlock> CRASHED_BOMB_SALTED;

    private NukeCasingBlocks() {
    }

    public static void registerAll() {
        NUKE_BOY = registerBlock("nuke_boy", () -> new NukeBoyBlock(CASING_PROPS));
        NUKE_GADGET = registerBlock("nuke_gadget", () -> new NukeGadgetBlock(CASING_PROPS));
        NUKE_MAN = registerBlock("nuke_man", () -> new NukeManBlock(CASING_PROPS));
        NUKE_MIKE = registerBlock("nuke_mike", () -> new NukeMikeBlock(CASING_PROPS));
        NUKE_TSAR = registerBlock("nuke_tsar", () -> new NukeTsarBlock(CASING_PROPS));
        NUKE_N2 = registerBlock("nuke_n2", () -> new NukeN2Block(CASING_PROPS));
        NUKE_PROTOTYPE = registerBlock("nuke_prototype", () -> new NukePrototypeBlock(CASING_PROPS));
        NUKE_FLEIJA = registerBlock("nuke_fleija", () -> new NukeFleijaBlock(CASING_PROPS));
        NUKE_BALEFIRE = registerBlock("nuke_balefire", () -> new NukeBalefireBlock(CASING_PROPS));
        // CE ModBlocks.java:711 — same NukeBalefire class, CE id is nuke_fstbmb.
        NUKE_FSTBMB = registerBlock("nuke_fstbmb", () -> new NukeBalefireBlock(CASING_PROPS));
        NUKE_SOLINIUM = registerBlock("nuke_solinium", () -> new NukeSoliniumBlock(CASING_PROPS));
        NUKE_CUSTOM = registerBlock("nuke_custom", () -> new NukeCustomBlock(CASING_PROPS));

        CRASHED_BOMB_BALEFIRE = registerBlock("crashed_bomb_balefire", () -> new CrashedBombBlock(CRASHED_PROPS, EnumDudType.BALEFIRE));
        CRASHED_BOMB_CONVENTIONAL = registerBlock("crashed_bomb_conventional", () -> new CrashedBombBlock(CRASHED_PROPS, EnumDudType.CONVENTIONAL));
        CRASHED_BOMB_NUKE = registerBlock("crashed_bomb_nuke", () -> new CrashedBombBlock(CRASHED_PROPS, EnumDudType.NUKE));
        CRASHED_BOMB_SALTED = registerBlock("crashed_bomb_salted", () -> new CrashedBombBlock(CRASHED_PROPS, EnumDudType.SALTED));

        NukeCasingBlockEntities.registerAll();
        NukeCasingMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.NUKE, block);
        return block;
    }
}
