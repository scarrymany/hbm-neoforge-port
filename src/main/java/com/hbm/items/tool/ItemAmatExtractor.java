package com.hbm.items.tool;

import com.hbm.blocks.bomb.CrashedBombBlock;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.items.special.ScatteredMilitaryItems;
import com.hbm.items.special.SpecialItems;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemAmatExtractor} (58 lines, read in full; real CE
 * registry id {@code bismuth_tool}, confirmed against {@code ModItems.java} - the class name and the
 * in-game id have always differed). {@code extends} this port's already-real
 * {@link ItemCustomLore}. Right-clicking a {@link CrashedBombBlock} while holding an empty
 * {@link ItemCell} rolls a reward: 1% chance the bomb detonates in place, otherwise a weighted split
 * between a rare {@code cell_balefire} and a normal full AMAT cell, plus a flat 50 RAD contamination
 * side effect.
 * <p>
 * {@code BlockCrashedBomb}/{@code IBomb.explode} and {@code ContaminationUtil.contaminate} were both
 * flagged as forward-blocked by this package's own research report - both have since landed in this
 * port ({@link CrashedBombBlock} under {@code com.hbm.blocks.bomb}, {@link ContaminationUtil} as
 * already-committed Phase 3 foundation infrastructure), so this wires the full behavior directly,
 * with no stub.
 */
public class ItemAmatExtractor extends ItemCustomLore {

    public ItemAmatExtractor(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!(level.getBlockState(pos).getBlock() instanceof CrashedBombBlock bomb)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide() && ItemCell.hasEmptyCell(player)) {
            float chance = level.getRandom().nextFloat();

            if (chance < 0.01F) {
                bomb.explode(level, pos, player);
            } else if (chance <= 0.3F) {
                ItemCell.consumeEmptyCell(player);
                grant(player, new ItemStack(ScatteredMilitaryItems.CELL_BALEFIRE.get()));
            } else {
                ItemCell.consumeEmptyCell(player);
                grant(player, ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.AMAT));
            }

            ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.CREATIVE, 50.0F);
        }

        return InteractionResult.SUCCESS;
    }

    private static void grant(Player player, ItemStack reward) {
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }
}
