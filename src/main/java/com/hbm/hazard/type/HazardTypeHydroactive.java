package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
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
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class HazardTypeHydroactive implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        if (RadiationConfig.disableHydro) return;

        final Level world = target.level();
        final boolean playerIsWet = target.isInWaterOrRain()
                || (isInRainBiome(target.blockPosition(), world) && world.isRaining() && world.canSeeSky(target.blockPosition()));

        if (playerIsWet && stack.getCount() > 0) {
            stack.setCount(0);
            world.explode(null, target.getX(), target.getY() + target.getEyeHeight(), target.getZ(), (float) level, false, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {

        if (RadiationConfig.disableHydro) return;

        if (item.isInWaterOrRain()) {
            item.discard();
            item.level().explode(null, item.getX(), item.getY() + item.getBbHeight() * 0.5, item.getZ(), (float) level, false, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[" + I18nUtil.resolveKey("trait.hydro") + "]").withStyle(ChatFormatting.RED));
    }

    private boolean isInRainBiome(final BlockPos pos, final Level world) {
        final Biome biome = world.getBiome(pos).value();
        return biome.hasPrecipitation();
    }
}
