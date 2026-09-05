package com.hbm.items.tool;

import com.hbm.entity.item.DroneEntityTypes;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.entity.item.EntityRequestDrone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Direction face = context.getClickedFace();
        if (face != Direction.UP) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos().above();
        ItemStack stack = context.getItemInHand();

        if (type == DroneType.REQUEST) {
            EntityRequestDrone drone = new EntityRequestDrone(DroneEntityTypes.REQUEST_DRONE.get(), level);
            drone.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(drone);
        } else {
            EntityDeliveryDrone drone = new EntityDeliveryDrone(DroneEntityTypes.DELIVERY_DRONE.get(), level);
            drone.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            drone.setExpress(type == DroneType.PATROL_EXPRESS || type == DroneType.PATROL_EXPRESS_CHUNKLOADING);
            if (type == DroneType.PATROL_CHUNKLOADING || type == DroneType.PATROL_EXPRESS_CHUNKLOADING) {
                drone.setChunkLoading();
            }
            level.addFreshEntity(drone);
        }

        if (!context.getPlayer().isCreative()) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}
