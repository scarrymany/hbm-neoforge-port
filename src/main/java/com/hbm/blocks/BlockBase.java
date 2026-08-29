package com.hbm.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Generic concrete block base, replacing CE's {@code BlockBase}. Registry name, translation key
 * and creative tab are no longer block-instance concerns in modern Minecraft: registry name comes
 * from the {@link net.neoforged.neoforge.registries.DeferredRegister} id, the translation key is
 * derived automatically from the registry name, and the creative tab is assigned to the
 * {@code BlockItem} at registration time. What is left to port is the no-spawn behavior and the
 * blast-resistance / no-spawn tooltip lines CE attached to every block built on this base.
 */
public class BlockBase extends Block {

    private static final float BLAST_RESISTANCE_TOOLTIP_THRESHOLD = 50.0F;

    protected final boolean noSpawn;

    public BlockBase(Properties properties) {
        this(properties, false);
    }

    public BlockBase(Properties properties, boolean noSpawn) {
        super(noSpawn ? properties.isValidSpawn(BlockBase::never) : properties);
        this.noSpawn = noSpawn;
    }

    public static boolean never(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        float resistance = this.getExplosionResistance();
        if (resistance > BLAST_RESISTANCE_TOOLTIP_THRESHOLD) {
            tooltip.add(Component.translatable("trait.blastres", resistance).withStyle(ChatFormatting.GOLD));
        }

        if (this.noSpawn) {
            tooltip.add(Component.translatable("tile.nospawn").withStyle(ChatFormatting.RED));
        }
    }

    // CE hardcoded a Tesla-coil hint tooltip here by identity-checking `stack.getItem()` against
    // `ModBlocks.meteor_battery`. That content-specific tooltip logic does not belong on a shared
    // base class and is deferred to the meteor_battery block class once it is ported in a later
    // phase (see the analogous deferral note in BlockFallingBase for gravel_diamond/sand_boron).
}
