package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.fluid.ToxicBlock;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Registers the wasteland-decontamination and virus block sets
 * {@code com.hbm.explosion.ExplosionChaos}'s {@code decontaminate}/{@code hardenVirus}/
 * {@code spreadVirus} methods need, per docs/phase4/entities_vortex_gravity_wells.md's Table B and
 * Deferred scope (confirmed not registered anywhere in this port before this pass). {@code explode}'s
 * own indestructible-block list ({@code reinforced_brick}/{@code reinforced_glass}/
 * {@code reinforced_lamp_on}/{@code reinforced_lamp_off}) already exists in {@link GenericBlocks}
 * ({@code registerRadResistant}/{@code registerReinforcedLamps}) with CE's real, modest
 * (25-300, <b>not</b> infinite) resistance values - only its missing fifth member,
 * {@code reinforced_sand}, is added there (not here) for consistency with its four already-registered
 * siblings. This file only registers what CE itself gave literally no other placement, drop, or
 * hazard-tick behavior worth carrying beyond identity + physical properties at this narrow
 * registration package's scope - see each field's own inline note for exactly what CE behavior is
 * and isn't preserved.
 * <p>
 * <b>Not ported</b> (see this report's Deferred scope and each note below): CE's self-propagating
 * {@code CrystalVirus}/{@code CheaterVirusSeed} tick/{@code neighborChanged}/{@code breakBlock} growth
 * mechanic (a real CE simulation, but tied to {@code GeneralConfig.enableVirus} and two further blocks
 * this list does not include, {@code ModBlocks.crystal_pulsar}/{@code cheater_virus} - out of this
 * narrow package's scope, flagged in the implementing session's own knownGaps rather than guessed at);
 * {@code BlockNuclearWaste}'s periodic {@code gas_radon_dense} spread (that gas block does not exist
 * in this port's gas-block family yet). {@code HazardRegistry} now binds {@code block_trinitite}
 * ({@code trn * block}) and {@code block_waste} ({@code wst * block}) — CE {@code HazardRegistry.java:222/:229}.
 */
public final class WastelandVirusBlocks {

    private static final float UNBREAKABLE_RESISTANCE = 3_600_000.0F;

    public static DeferredBlock<BlockBase> CRYSTAL_VIRUS;
    public static DeferredBlock<BlockBase> CRYSTAL_HARDENED;
    public static DeferredBlock<BlockBase> CHEATER_VIRUS_SEED;
    public static DeferredBlock<BlockHazard> BLOCK_TRINITITE;
    public static DeferredBlock<BlockNuclearWaste> BLOCK_WASTE;
    public static DeferredBlock<BlockSellafield> SELLAFIELD;
    public static DeferredBlock<BlockBase> SELLAFIELD_SLAKED;
    public static DeferredBlock<ToxicBlock> TOXIC_BLOCK;

    private WastelandVirusBlocks() {
    }

    public static void registerAll() {
        registerVirus();
        registerHazardDecoration();
        registerSellafield();
        registerToxicBlock();
    }

    /**
     * CE: {@code CrystalVirus}/{@code BlockBase("crystal_hardened")}/{@code CheaterVirusSeed}, all
     * {@code Material.IRON}, all {@code .setCreativeTab(null)} (never creative-menu-obtainable in CE
     * either - explosion/world-placed only). {@code crystal_virus}/{@code crystal_hardened} are
     * CE hardness 15.0F, resistance {@code Float.POSITIVE_INFINITY}; {@code cheater_virus_seed} is
     * hardness <i>and</i> resistance {@code Float.POSITIVE_INFINITY} (this port's {@code -1.0F}
     * "unbreakable" destroy-time convention, matching e.g. {@code ore_bedrock_block}). CE's own
     * self-spreading tick behavior is not ported - see this class's own javadoc.
     */
    private static void registerVirus() {
        BlockBehaviour.Properties virusProps = BlockBehaviour.Properties.of().strength(15.0F, UNBREAKABLE_RESISTANCE).sound(SoundType.METAL);
        CRYSTAL_VIRUS = registerBlock("crystal_virus", () -> new BlockBase(virusProps), null);
        CRYSTAL_HARDENED = registerBlock("crystal_hardened", () -> new BlockBase(virusProps), null);

        BlockBehaviour.Properties seedProps = BlockBehaviour.Properties.of().strength(-1.0F, UNBREAKABLE_RESISTANCE).sound(SoundType.METAL);
        CHEATER_VIRUS_SEED = registerBlock("cheater_virus_seed", () -> new BlockBase(seedProps), null);
    }

    /**
     * CE: {@code block_trinitite = new BlockHazard(Material.IRON, "block_trinitite")
     * .setHardness(5.0F).setResistance(10.0F)}; {@code block_waste = new BlockNuclearWaste("block_waste")
     * .makeBeaconable().setDisplayEffect(RADFOG).setHardness(5.0F).setResistance(10.0F)}.
     * {@code block_trinitite} reuses this port's already-real {@link BlockHazard} (contact-hazard
     * application via {@link com.hbm.hazard.HazardSystem#applyHazards}); {@code block_waste} reuses
     * this port's own already-committed (but until now unregistered) {@link BlockNuclearWaste}, CE's
     * real subclass for this exact block. Both blocks' beacon-base/particle-effect flags, and
     * {@code BlockNuclearWaste}'s own radon-gas-spread tick, are CE additions those two classes'
     * own javadoc already documents as deferred (beacon base is a modern block tag, particles are
     * Phase 5 rendering, radon spread needs a gas block this port's gas family doesn't have yet).
     */
    private static void registerHazardDecoration() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
        BLOCK_TRINITITE = registerBlock("block_trinitite", () -> new BlockHazard(props), ModCreativeTabs.BLOCKS);
        BLOCK_WASTE = registerBlock("block_waste", () -> new BlockNuclearWaste(props), ModCreativeTabs.BLOCKS);
    }

    /**
     * CE: {@code new BlockSellafield(Material.ROCK, SoundType.STONE, "sellafield").setHardness(5.0F)
     * .setResistance(6F)} and {@code new BlockSellafieldSlaked(Material.ROCK, SoundType.STONE,
     * "sellafield_slaked").setHardness(5.0F).setResistance(6F)}. {@code sellafield_slaked} is CE's
     * fully-decayed terminus block - a plain, stateless {@link BlockBase} here (its only CE-side
     * behavior beyond that is the custom baked-model shade rendering {@link BlockSellafield}'s own
     * javadoc already defers to Phase 5).
     */
    private static void registerSellafield() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(5.0F, 6.0F).sound(SoundType.STONE);
        SELLAFIELD = registerBlock("sellafield", () -> new BlockSellafield(props), ModCreativeTabs.RESOURCE);
        SELLAFIELD_SLAKED = registerBlock("sellafield_slaked", () -> new BlockBase(props), ModCreativeTabs.RESOURCE);
    }

    /**
     * CE {@code new ToxicBlock(toxic_fluid, Material.WATER, "toxic_block")}. Creative tab null.
     * Existing CE blockstate/model/lang. Fluid flow cited on the class.
     */
    private static void registerToxicBlock() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .noLootTable()
                .replaceable()
                .strength(100.0F)
                .sound(SoundType.SLIME_BLOCK);
        TOXIC_BLOCK = registerBlock("toxic_block", () -> new ToxicBlock(props), null);
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
