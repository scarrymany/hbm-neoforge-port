package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Logistics drone spawner, ported from CE's {@code com.hbm.items.tool.ItemDrone}. CE's single item
 * with 5 {@code EnumDroneType} metadata subtypes ({@code PATROL}, {@code PATROL_CHUNKLOADING},
 * {@code PATROL_EXPRESS}, {@code PATROL_EXPRESS_CHUNKLOADING}, {@code REQUEST}) is flattened into 5
 * separate registered items here, matching this port's post-flattening convention - see
 * {@link DroneType} and {@code CouplingToolItems} for the 5 registrations.
 * <p>
 * <b>Stubbed pending the drone logistics subsystem</b>: {@code EntityDroneBase}/
 * {@code EntityDeliveryDrone} (this port has no {@code com.hbm.entity} package at all - confirmed by
 * repo-wide grep, Phase 3+ scope per PORT_SPEC), {@code IDroneLinkable}, and the 4
 * {@code GUIDrone*}/{@code ContainerDrone*} pairs CE's drone dock/waypoint/requester/provider blocks
 * need none exist anywhere in this port. Per
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s Deferred scope: "a real
 * standalone subsystem...recommend a dedicated 'drone logistics' Phase 2 research package." Per the
 * port plan's "stub with a documented TODO rather than blocking" rule, every variant is registered
 * (tooltip included) with its use-behavior left a no-op {@link InteractionResult#PASS}.
 */
public class ItemDrone extends Item {

    public enum DroneType {
        PATROL,
        PATROL_CHUNKLOADING,
        PATROL_EXPRESS,
        PATROL_EXPRESS_CHUNKLOADING,
        REQUEST
    }

    private final DroneType type;

    public ItemDrone(DroneType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public DroneType getDroneType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Deploys a logistics drone (" + type.name().toLowerCase() + ")."));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): once com.hbm.entity.item.{EntityDroneBase,EntityDeliveryDrone}
        // and the drone dock/waypoint/requester/provider block entities exist, port CE's
        // right-click-up-face drone-spawn behavior here.
        return InteractionResult.PASS;
    }
}
