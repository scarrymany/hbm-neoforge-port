package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.BombBlockEntities;
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
 * Block + {@code BlockItem} registration for {@code docs/phase3/bomb_blocks_and_detonators.md}
 * Section A (conventional explosive blocks + TE). Mirrors this port's established
 * {@code EnergyNetworkBlocks}/{@code PowerGenBlocks} table-driven {@link #registerAll()}/
 * {@link #registerBlock} shape. Registry names match CE's own {@code ModBlocks.java} declarations
 * exactly (confirmed by reading them) except the TNT variant, which CE itself already renamed to
 * {@code tnt_ntm} to avoid colliding with vanilla {@code minecraft:tnt}.
 * <p>
 * Block-entity-type registration lives in the sibling {@link BombBlockEntities} class. Wiring this
 * family into the game needs exactly one call from {@code ModBlocks.register()} (see this task's
 * wiring notes) - no other shared file needs a direct edit.
 * <p>
 * <b>Not registered here</b> (see this task's own scope and the research report's Deferred scope):
 * the 9 nuke casings + {@code NukeCustom} + {@code BlockCrashedBomb} (Section B, blocked on GUI/
 * recipe-plumbing work beyond this task), the detonator/defuser items themselves ({@code ItemDetonator}/
 * {@code ItemMultiDetonator}/{@code ItemLaserDetonator}/{@code ItemDefuser}, Section C - a separate
 * work package; {@link Landmine}'s defuser check resolves those two items by registry id rather than
 * a compile-time reference, see that class's javadoc), and the five non-{@code IBomb} world-hazard
 * block families explicitly named out of scope by the research report's Deferred scope section.
 * {@link LaunchPad}/{@link LaunchPadLarge}/{@link LaunchPadRusted} (the three launch-pad
 * multiblocks) were added by the later {@code missile_launch_infra} work package - see that
 * package's own {@code docs/phase3/missile_launch_infra.md} report, not this class's original
 * {@code bomb_blocks_and_detonators} scope.
 */
public final class BombBlocks {

    private static final BlockBehaviour.Properties TNT_PROPS =
            BlockBehaviour.Properties.of().strength(0F).sound(SoundType.GRASS);
    private static final BlockBehaviour.Properties CHARGE_PROPS =
            BlockBehaviour.Properties.of().strength(1.0F, 1.0F).sound(SoundType.METAL).noOcclusion().noCollission();
    private static final BlockBehaviour.Properties MINE_PROPS =
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion().noCollission();
    private static final BlockBehaviour.Properties BOMB_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 6000F).sound(SoundType.METAL);
    private static final BlockBehaviour.Properties DET_PROPS =
            BlockBehaviour.Properties.of().strength(0.1F, 0.0F).sound(SoundType.METAL).noOcclusion();

    public static DeferredBlock<BlockTNT> TNT;
    public static DeferredBlock<BlockDynamite> DYNAMITE;
    public static DeferredBlock<BlockSemtex> SEMTEX;
    public static DeferredBlock<BlockC4> C4;
    public static DeferredBlock<BlockFissureBomb> FISSURE_BOMB;

    public static DeferredBlock<BlockChargeC4> CHARGE_C4;
    public static DeferredBlock<BlockChargeSemtex> CHARGE_SEMTEX;
    public static DeferredBlock<BlockChargeDynamite> CHARGE_DYNAMITE;
    public static DeferredBlock<BlockChargeMiner> CHARGE_MINER;

    public static DeferredBlock<Landmine> MINE_AP;
    public static DeferredBlock<Landmine> MINE_HE;
    public static DeferredBlock<Landmine> MINE_SHRAP;
    public static DeferredBlock<Landmine> MINE_FAT;
    public static DeferredBlock<Landmine> MINE_NAVAL;

    public static DeferredBlock<DetCord> DET_CORD;
    public static DeferredBlock<DetCord> DET_CHARGE;
    public static DeferredBlock<DetCord> DET_N2;
    public static DeferredBlock<DetCord> DET_NUKE;
    public static DeferredBlock<DetCord> DET_BALE;
    public static DeferredBlock<DetMiner> DET_MINER;

    public static DeferredBlock<BombFlameWar> FLAME_WAR;
    public static DeferredBlock<BombFloat> FLOAT_BOMB;
    public static DeferredBlock<BombFloat> EMP_BOMB;
    public static DeferredBlock<BombThermo> THERM_ENDO;
    public static DeferredBlock<BombThermo> THERM_EXO;

    public static DeferredBlock<BombMultiBlock> BOMB_MULTI;

    /** Phase 3 ({@code missile_launch_infra}): the three launch-pad multiblocks. */
    public static DeferredBlock<LaunchPad> LAUNCH_PAD;
    public static DeferredBlock<LaunchPadLarge> LAUNCH_PAD_LARGE;
    public static DeferredBlock<LaunchPadRusted> LAUNCH_PAD_RUSTED;

    private static final BlockBehaviour.Properties LAUNCH_PAD_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 6000F).sound(SoundType.METAL);

    private BombBlocks() {
    }

    public static void registerAll() {
        TNT = registerBlock("tnt_ntm", () -> new BlockTNT(TNT_PROPS));
        DYNAMITE = registerBlock("dynamite", () -> new BlockDynamite(TNT_PROPS));
        SEMTEX = registerBlock("semtex", () -> new BlockSemtex(TNT_PROPS));
        C4 = registerBlock("c4", () -> new BlockC4(TNT_PROPS));
        FISSURE_BOMB = registerBlock("fissure_bomb", () -> new BlockFissureBomb(TNT_PROPS));

        CHARGE_C4 = registerBlock("charge_c4", () -> new BlockChargeC4(CHARGE_PROPS));
        CHARGE_SEMTEX = registerBlock("charge_semtex", () -> new BlockChargeSemtex(CHARGE_PROPS));
        CHARGE_DYNAMITE = registerBlock("charge_dynamite", () -> new BlockChargeDynamite(CHARGE_PROPS));
        CHARGE_MINER = registerBlock("charge_miner", () -> new BlockChargeMiner(CHARGE_PROPS));

        MINE_AP = registerBlock("mine_ap", () -> new Landmine(MINE_PROPS, 1.5D, 1D));
        MINE_HE = registerBlock("mine_he", () -> new Landmine(MINE_PROPS, 2D, 5D));
        MINE_SHRAP = registerBlock("mine_shrap", () -> new Landmine(MINE_PROPS, 1.5D, 1D));
        MINE_FAT = registerBlock("mine_fat", () -> new Landmine(MINE_PROPS, 2.5D, 1D));
        MINE_NAVAL = registerBlock("mine_naval", () -> new Landmine(MINE_PROPS, 2.5D, 1D));

        DET_CORD = registerBlock("det_cord", () -> new DetCord(DET_PROPS));
        DET_MINER = registerBlock("det_miner", () -> new DetMiner(DET_PROPS));
        DET_CHARGE = registerBlock("det_charge", () -> new DetCord(DET_PROPS));
        DET_N2 = registerBlock("det_n2", () -> new DetCord(DET_PROPS));
        DET_NUKE = registerBlock("det_nuke", () -> new DetCord(DET_PROPS));
        DET_BALE = registerBlock("det_bale", () -> new DetCord(DET_PROPS));

        FLAME_WAR = registerBlock("flame_war", () -> new BombFlameWar(BOMB_PROPS));
        FLOAT_BOMB = registerBlock("float_bomb", () -> new BombFloat(BOMB_PROPS));
        EMP_BOMB = registerBlock("emp_bomb", () -> new BombFloat(BOMB_PROPS));
        THERM_ENDO = registerBlock("therm_endo", () -> new BombThermo(BOMB_PROPS));
        THERM_EXO = registerBlock("therm_exo", () -> new BombThermo(BOMB_PROPS));

        BOMB_MULTI = registerBlock("bomb_multi", () -> new BombMultiBlock(BOMB_PROPS));

        LAUNCH_PAD = registerBlock("launch_pad", () -> new LaunchPad(LAUNCH_PAD_PROPS));
        LAUNCH_PAD_LARGE = registerBlock("launch_pad_large", () -> new LaunchPadLarge(LAUNCH_PAD_PROPS));
        LAUNCH_PAD_RUSTED = registerBlock("launch_pad_rusted", () -> new LaunchPadRusted(LAUNCH_PAD_PROPS));

        BombBlockEntities.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.WEAPON, block);
        return block;
    }
}
