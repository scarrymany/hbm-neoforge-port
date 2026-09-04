package com.hbm.items.tool;

import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.items.IngotNuggetItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Exact CE {@code ItemMS} ({@code mysteryshovel}) {@code :36-65}: server hit on
 * {@code ntm_dirt} {@code destroyBlock(false)}, then drop CE meta 1/2/3
 * ({@code ingot_u238m2_elements}/{@code _arsenic}/{@code _vault}) with CE scatter.
 */
public class ItemMS extends Item {

    public ItemMS(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Lost but not forgotten"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        // CE ItemMS.java:36-65
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            if (level.getBlockState(pos).getBlock() == PlantBlocks.NTM_DIRT.get()) {
                level.destroyBlock(pos, false);

                Random rand = new Random();
                List<ItemStack> list = new ArrayList<>();
                list.add(new ItemStack(IngotNuggetItems.INGOT_U238M2_ELEMENTS.get()));
                list.add(new ItemStack(IngotNuggetItems.INGOT_U238M2_ARSENIC.get()));
                list.add(new ItemStack(IngotNuggetItems.INGOT_U238M2_VAULT.get()));

                for (ItemStack sta : list) {
                    float f = rand.nextFloat() * 0.8F + 0.1F;
                    float f1 = rand.nextFloat() * 0.8F + 0.1F;
                    float f2 = rand.nextFloat() * 0.8F + 0.1F;
                    ItemEntity entityitem = new ItemEntity(level, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, sta);

                    float f3 = 0.05F;
                    entityitem.setDeltaMovement(
                            rand.nextGaussian() * f3,
                            rand.nextGaussian() * f3 + 0.2F,
                            rand.nextGaussian() * f3);
                    level.addFreshEntity(entityitem);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
