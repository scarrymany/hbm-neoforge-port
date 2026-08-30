package com.hbm.items.tool;

import com.hbm.items.ItemBase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ambient readout of local soot/poison/heavy-metal/fallout pollution. Ported from CE's
 * {@code com.hbm.items.tool.ItemPollutionDetector}.
 *
 * <p><b>Stubbed pending {@code PollutionHandler}.</b> CE's {@code onUpdate} reads
 * {@code com.hbm.handler.pollution.PollutionHandler.getPollutionData(world, pos)}, a standalone
 * world-pollution simulation. No such system exists anywhere in this port yet - it is a real,
 * cross-cutting world-simulation system (not a Phase 0/1 item concern) that this area's own survey
 * (docs/phase1/items_tool.md) already flagged as needing an owner. Per the port plan's explicit
 * instruction for this item, its readout is left an explicit TODO rather than faked; the item itself
 * is registered so it exists ready for that system once it lands.
 */
public class ItemPollutionDetector extends ItemBase {

    public ItemPollutionDetector(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // TODO(cross-area follow-up): once com.hbm.handler.pollution.PollutionHandler (or an
        // equivalent) exists, port CE's behavior here - every 10 ticks, for a server-side player,
        // read PollutionData at the entity's position and report soot/poison/heavy-metal/fallout
        // levels to the player (CE used a PlayerInformPacketLegacy toast; this port's sibling items
        // use Player#sendSystemMessage instead - see e.g. ItemDosimeter).
    }
}
