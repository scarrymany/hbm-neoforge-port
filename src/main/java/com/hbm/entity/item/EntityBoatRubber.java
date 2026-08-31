package com.hbm.entity.item;

import com.hbm.items.tool.ToolItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * CE: {@code com.hbm.entity.item.EntityBoatRubber} (26 lines) — vanilla boat that drops
 * {@code boat_rubber}.
 */
public class EntityBoatRubber extends Boat {

    public float prevRenderYaw;

    public EntityBoatRubber(EntityType<? extends EntityBoatRubber> type, Level level) {
        super(type, level);
    }

    public EntityBoatRubber(Level level, double x, double y, double z) {
        this(BoatEntityTypes.BOAT_RUBBER.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return ToolItems.BOAT_RUBBER.get();
    }
}
