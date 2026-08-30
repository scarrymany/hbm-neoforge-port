package com.hbm.main;

import com.hbm.hazard.HazardSystem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Game-bus per-entity tick dispatch, split out of {@link CommonEvents} - see that class's javadoc for
 * why {@link EntityTickEvent.Pre} (a game-bus event) cannot share one {@code @EventBusSubscriber}
 * class with {@code FMLCommonSetupEvent} (a mod-bus event). This class uses the annotation's default
 * {@code bus = Bus.GAME}, which is correct here (unlike {@link CommonEvents}).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public class CommonTickEvents {

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        if(entity instanceof Player player) {
            HazardSystem.updatePlayerInventory(player);
        }
        if(entity instanceof ItemEntity itemEntity) {
            HazardSystem.updateDroppedItem(itemEntity);
        }
        if(entity instanceof LivingEntity livingEntity) {
            HazardSystem.updateLivingInventory(livingEntity);
        }
    }
}
