package com.hbm.blocks;

import com.hbm.blocks.generic.BlockClean;
import com.hbm.blocks.generic.BlockForgottenBrick;
import com.hbm.blocks.generic.BlockGrate;
import com.hbm.blocks.generic.BlockRotatablePillar;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registry, replacing CE's bespoke {@code ModBlocks.ALL_BLOCKS} list (populated by
 * base-class constructors) with a real NeoForge {@link DeferredRegister}.
 * <p>
 * Deliberately empty in Phase 0: every concrete block (~620 CE block files) is Phase 1/2 content
 * and lands here field by field, one {@code public static final DeferredBlock<...>} per block,
 * registered via {@code BLOCKS.register(name, supplier)} (see the confirmed pattern in the Neo
 * Edition reference's {@code NtmBlocks}). Registering a matching {@code BlockItem} for a block is
 * left to whichever phase adds that field, since it depends on the not-yet-existing
 * {@code com.hbm.items.ModItems} item registry and on which {@code BlockItem} subclass (plain,
 * lore, blast-info, ...) a given block needs.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MainRegistry.MODID);

    /**
     * No {@code BlockEntityType} registry existed anywhere in the port before this pass - added
     * here (rather than a new top-level class) since {@code ModBlocks} is already the shared,
     * every-area-appends-to-it home for block-adjacent NeoForge registries. Any area registering a
     * block entity should add its {@code BLOCK_ENTITY_TYPES.register(...)} calls the same way
     * {@code BLOCKS.register(...)} calls already work.
     */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MainRegistry.MODID);

    /**
     * Cross-block sibling references some {@code com.hbm.blocks.generic} classes need at runtime
     * (identity checks against a specific registered instance, mirroring CE's own
     * {@code ModBlocks.foo} field reads from inside another block's class body - see
     * {@code BlockGrate}/{@code BlockRotatablePillar}/{@code BlockForgottenLock}'s own javadoc for
     * each one's exact use). Populated by {@code GenericBlocks.registerAll()} during
     * {@link #register}, before any block's {@code DeferredBlock#get()} is actually called (that
     * only happens later, at gameplay time, once NeoForge's {@code RegisterEvent} has fired) - not
     * {@code final} for that reason, the same as any other {@code DeferredRegister.register(...)}
     * result stored for later cross-reference.
     */
    public static DeferredBlock<BlockForgottenBrick> BRICK_FORGOTTEN;
    public static DeferredBlock<BlockClean> TILE_LAB;
    public static DeferredBlock<BlockClean> TILE_LAB_CRACKED;
    public static DeferredBlock<BlockClean> TILE_LAB_BROKEN;
    public static DeferredBlock<BlockGrate> STEEL_GRATE_WIDE;
    public static DeferredBlock<BlockRotatablePillar> BLOCK_SCHRABIDIUM_CLUSTER;
    public static DeferredBlock<BlockRotatablePillar> BLOCK_EUPHEMIUM_CLUSTER;

    // Lighting machines
    public static DeferredBlock<com.hbm.blocks.machine.Floodlight> FLOODLIGHT;
    public static DeferredBlock<com.hbm.blocks.machine.FloodlightBeam> FLOODLIGHT_BEAM;
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<com.hbm.blockentity.machine.FloodlightBlockEntity>> FLOODLIGHT_ENTITY;
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<com.hbm.blockentity.machine.FloodlightBeamBlockEntity>> FLOODLIGHT_BEAM_ENTITY;

    // Satellite machines
    public static DeferredBlock<com.hbm.blocks.machine.MachineTapeDrive> MACHINE_TAPE_DRIVE;
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<com.hbm.blockentity.machine.TapeDriveBlockEntity>> TAPE_DRIVE_ENTITY;

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        OreBlocks.registerAll();
        com.hbm.blocks.generic.GenericBlocks.registerAll();
        com.hbm.blocks.generic.Phase8Blocks.registerAll();
        MaterialBlockGenerator.registerAll();
        com.hbm.blocks.gas.GasBlocks.registerAll();
        com.hbm.blocks.machine.PowerGenBlocks.registerAll();
        com.hbm.blocks.machine.StorageMachineBlocks.registerAll();
        com.hbm.blockentity.machine.StorageBlockEntities.registerAll();
        com.hbm.blocks.network.energy.EnergyNetworkBlocks.registerAll();
        com.hbm.blocks.network.RadioNetworkBlocks.registerAll();
        com.hbm.blocks.network.FluidDuctBlocks.registerAll();
        com.hbm.blockentity.network.FluidDuctBlockEntities.registerAll();
        com.hbm.blocks.network.ConveyorBlocks.registerAll();
        com.hbm.blocks.machine.OilChainBlocks.registerAll();
        com.hbm.blocks.machine.ProcessingBlocks.registerAll();
        com.hbm.blocks.machine.chem.ChemIsotopeBlocks.registerAll();
        com.hbm.blocks.machine.PWRBlocks.registerAll();
        com.hbm.blocks.machine.fusion.FusionBlocks.registerAll();
        com.hbm.blocks.machine.rbmk.RBMKBlocks.registerAll();
        com.hbm.blocks.bomb.BombBlocks.registerAll();
        com.hbm.blocks.bomb.NukeCasingBlocks.registerAll();
        com.hbm.blocks.turret.TurretBlocks.registerAll();
        com.hbm.blocks.machine.LaunchInfraBlocks.registerAll();
        com.hbm.blocks.machine.SensorBlocks.registerAll();
        com.hbm.blocks.machine.reprocess.ReprocessBlocks.registerAll();
        com.hbm.blocks.machine.accel.AccelBlocks.registerAll();
        com.hbm.blocks.machine.workshop.WorkshopBlocks.registerAll();
        com.hbm.blocks.machine.CrucibleBlocks.registerAll();
        com.hbm.blocks.generic.BallsSpawnerBlocks.registerAll();
        com.hbm.blocks.machine.SealBlocks.registerAll();
        com.hbm.blocks.machine.pile.PileBlocks.registerAll();
        com.hbm.blocks.machine.Phase11CasingBlocks.registerAll();
        com.hbm.blocks.machine.dummyable.DummyableProcessBlocks.registerAll();
        com.hbm.blocks.machine.LightingBlocks.registerAll();
        com.hbm.blocks.machine.SatelliteBlocks.registerAll();
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
