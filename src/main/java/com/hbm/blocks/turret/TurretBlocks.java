package com.hbm.blocks.turret;

import com.hbm.blocks.ModBlocks;
import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.turret.TurretMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for this turret package's 11 in-scope concrete turrets
 * (13 in CE minus the out-of-scope Arty/HIMARS artillery/missile pair - see
 * {@code docs/phase3/turret_system.md}). Mirrors {@code PowerGenBlocks}' shape: block-entity-type
 * registration lives in the sibling {@link TurretBlockEntities} class, and
 * {@link TurretMenus}' {@code MenuType} is triggered from this class's {@link #registerAll()} too,
 * so wiring this whole family into the game needs exactly one call from {@code ModBlocks.register()}
 * (see this task's wiring notes) - no other shared file needs a direct edit.
 * <p>
 * The two "damaged" ruins/loot variants ({@code TurretSentryDamaged}/{@code TurretHowardDamaged})
 * get no {@code BlockItem} - CE's own {@code TurretSentryDamaged} explicitly drops
 * {@code Items.AIR} instead of itself ({@code getItemDropped} override), and
 * {@code TurretHowardDamaged} is the same category of world-gen-ruin-only content; neither is
 * intended to be player-obtainable/placeable.
 */
public final class TurretBlocks {

    private static final BlockBehaviour.Properties TURRET_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<TurretSentryBlock> TURRET_SENTRY;
    public static DeferredBlock<TurretSentryDamagedBlock> TURRET_SENTRY_DAMAGED;
    public static DeferredBlock<TurretChekhovBlock> TURRET_CHEKHOV;
    public static DeferredBlock<TurretFriendlyBlock> TURRET_FRIENDLY;
    public static DeferredBlock<TurretRichardBlock> TURRET_RICHARD;
    public static DeferredBlock<TurretJeremyBlock> TURRET_JEREMY;
    public static DeferredBlock<TurretHowardBlock> TURRET_HOWARD;
    public static DeferredBlock<TurretHowardDamagedBlock> TURRET_HOWARD_DAMAGED;
    public static DeferredBlock<TurretFritzBlock> TURRET_FRITZ;
    public static DeferredBlock<TurretMaxwellBlock> TURRET_MAXWELL;
    public static DeferredBlock<TurretTauonBlock> TURRET_TAUON;

    private TurretBlocks() {
    }

    public static void registerAll() {
        TURRET_SENTRY = registerBlock("turret_sentry", () -> new TurretSentryBlock(TURRET_PROPS));
        TURRET_SENTRY_DAMAGED = registerBlockNoItem("turret_sentry_damaged", () -> new TurretSentryDamagedBlock(TURRET_PROPS));
        TURRET_CHEKHOV = registerBlock("turret_chekhov", () -> new TurretChekhovBlock(TURRET_PROPS));
        TURRET_FRIENDLY = registerBlock("turret_friendly", () -> new TurretFriendlyBlock(TURRET_PROPS));
        TURRET_RICHARD = registerBlock("turret_richard", () -> new TurretRichardBlock(TURRET_PROPS));
        TURRET_JEREMY = registerBlock("turret_jeremy", () -> new TurretJeremyBlock(TURRET_PROPS));
        TURRET_HOWARD = registerBlock("turret_howard", () -> new TurretHowardBlock(TURRET_PROPS));
        TURRET_HOWARD_DAMAGED = registerBlockNoItem("turret_howard_damaged", () -> new TurretHowardDamagedBlock(TURRET_PROPS));
        TURRET_FRITZ = registerBlock("turret_fritz", () -> new TurretFritzBlock(TURRET_PROPS));
        TURRET_MAXWELL = registerBlock("turret_maxwell", () -> new TurretMaxwellBlock(TURRET_PROPS));
        TURRET_TAUON = registerBlock("turret_tauon", () -> new TurretTauonBlock(TURRET_PROPS));

        TurretBlockEntities.registerAll();
        TurretMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.WEAPON, block);
        return block;
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlockNoItem(String name, Supplier<T> factory) {
        return ModBlocks.BLOCKS.register(name, factory);
    }
}
