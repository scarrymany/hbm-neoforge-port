package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * PWR reactor vessel visual shell, ported from CE's {@code BlockGenericPWR}. CE's {@code
 * BlockBakeBase} superclass and {@code IDynamicModels} marker existed purely to auto-generate a
 * cube/column model from a texture at bake time; per the port's datagen ground rule that whole
 * mechanism is dropped in favor of a datagen-authored block model, leaving a plain decorative shell
 * with no tile entity - the real PWR multiblock wiring is Phase 2 content, matching the RBMK slab's
 * own "wire up later" note.
 */
public class BlockGenericPWR extends BlockBase implements ITooltipProvider {

    public BlockGenericPWR(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        this.addStandardInfo(tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
