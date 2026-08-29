package com.hbm.hazard.type;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockClean;
import com.hbm.config.RadiationConfig;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class HazardTypeContaminating implements IHazardType {

    private static final int MAX_RADIUS = 500;
    private static final int SET_BLOCK_FLAGS = 3;

    private static int computeRadius(final double level) {
        return (int) Math.min(Math.sqrt(level) + 0.5D, MAX_RADIUS);
    }

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
        if (!RadiationConfig.enableContaminationOnGround) return;
        if (item == null) return;
        final Level world = item.level();
        if (world == null || world.isClientSide()) return;

        if (item.onGround()) {
            final BlockPos pos = item.blockPosition();
            final BlockPos down = pos.below();
            if (world.getBlockState(down).getBlock() instanceof BlockClean clean) {
                getUsed(clean, down, world);
                return;
            }
            final int radius = computeRadius(level);
            if (radius > 1) {
                // With no biome change, a fallout rain would leave no radiation behind, so manually compensate
                ChunkRadiationManager.proxy.incrementRad(world, pos, level);
                // Replaces EntityFalloutRain with a direct waste explosion to make U -> Sa326 transform harder.
                ExplosionNukeGeneric.waste(world, pos.getX(), pos.getY(), pos.getZ(), radius);
            }
            item.discard();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        if (!RadiationConfig.enableContaminationOnGround) return;
        final int radius = computeRadius(level);
        if (radius > 1) {
            list.add(Component.literal("[" + I18nUtil.resolveKey("trait.contaminating") + "]").withStyle(ChatFormatting.DARK_GREEN));
            list.add(Component.literal(" " + I18nUtil.resolveKey("trait.contaminating.radius", radius)).withStyle(ChatFormatting.GREEN));
        }
    }

    protected static void getUsed(final Block b, final BlockPos pos, final Level level) {
        if (b == ModBlocks.tile_lab.get() && level.getRandom().nextInt(2000) == 0) {
            level.setBlock(pos, ModBlocks.tile_lab_cracked.get().defaultBlockState(), SET_BLOCK_FLAGS);
        } else if (b == ModBlocks.tile_lab_cracked.get() && level.getRandom().nextInt(10000) == 0) {
            level.setBlock(pos, ModBlocks.tile_lab_broken.get().defaultBlockState(), SET_BLOCK_FLAGS);
        }
    }
}
