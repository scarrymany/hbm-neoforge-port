package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.LaunchInfraBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for {@code docs/phase3/missile_launch_infra.md}'s silo
 * hatch and Soyuz launch complex, mirroring {@code BombBlocks}'s established table-driven
 * {@link #registerAll()} shape. Registry names match CE's real {@code ModBlocks.java} declarations
 * ({@code silo_hatch_drillgon}, {@code dummy_block_silo_hatch}, {@code launchpad_soyuz}) - see
 * {@code missile_launch_infra.md}'s headline finding #3 for why this class deliberately does
 * <b>not</b> register {@code silo_hatch}/{@code silo_hatch_large} (those are unrelated
 * {@code BlockDoorGeneric} instances, Phase 1 scope).
 * <p>
 * {@link DummyBlockSiloHatch} intentionally gets no {@link BlockItem} - it is placed
 * programmatically only ({@link com.hbm.blockentity.machine.SiloHatchBlockEntity#placeDummy}),
 * never by a player, and its own {@code getCloneItemStack} already redirects pick-block to the real
 * {@code silo_hatch_drillgon} item.
 */
public final class LaunchInfraBlocks {

    private static final BlockBehaviour.Properties HATCH_PROPS =
            BlockBehaviour.Properties.of().strength(10.0F, 1200F).sound(SoundType.METAL).noOcclusion();
    private static final BlockBehaviour.Properties DUMMY_PROPS =
            BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noOcclusion().noCollission().noLootTable();
    private static final BlockBehaviour.Properties SOYUZ_PROPS =
            BlockBehaviour.Properties.of().strength(10.0F, 1200F).sound(SoundType.METAL);

    public static DeferredBlock<BlockSiloHatch> SILO_HATCH;
    public static DeferredBlock<DummyBlockSiloHatch> DUMMY_BLOCK_SILO_HATCH;
    public static DeferredBlock<LaunchpadSoyuz> LAUNCHPAD_SOYUZ;

    private LaunchInfraBlocks() {
    }

    public static void registerAll() {
        SILO_HATCH = registerBlock("silo_hatch_drillgon", () -> new BlockSiloHatch(HATCH_PROPS));
        DUMMY_BLOCK_SILO_HATCH = ModBlocks.BLOCKS.register("dummy_block_silo_hatch", () -> new DummyBlockSiloHatch(DUMMY_PROPS));
        LAUNCHPAD_SOYUZ = registerBlock("launchpad_soyuz", () -> new LaunchpadSoyuz(SOYUZ_PROPS));

        LaunchInfraBlockEntities.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MISSILE, block);
        return block;
    }
}
