package com.hbm.handler.ability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * On-hit ability contract for melee weapons. Only the interface shape and the {@link #NONE}
 * sentinel are ported here: this area's scope (mining-tool ability framework) needs this type to
 * exist so {@link AvailableAbilities}/{@link ToolPreset} - shared by both tools and weapons in CE
 * - compile and behave correctly, but the concrete weapon abilities themselves (RADIATION,
 * VAMPIRE, STUN, PHOSPHORUS, FIRE, CHAINSAW, BEHEADER, BOBBLE) depend on Phase 3 systems
 * (ContaminationUtil, HbmPotion, EntityQuackos-style mob drops, ModBlocks.bobblehead, ...) that
 * are not part of this port yet. Whoever ports {@code ItemSwordAbility} and friends in Phase 3
 * should add those singletons here, following CE's {@code com.hbm.handler.ability.IWeaponAbility}
 * exactly.
 */
public interface IWeaponAbility extends IBaseAbility {

    void onHit(int level, Level level_, Player player, Entity victim, Item tool);

    int SORT_ORDER_BASE = 200;

    IWeaponAbility NONE = new IWeaponAbility() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE;
        }

        @Override
        public void onHit(int level, Level level_, Player player, Entity victim, Item tool) {
        }
    };

    IWeaponAbility[] abilities = { NONE };

    static IWeaponAbility getByName(String name) {
        for (IWeaponAbility ability : abilities) {
            if (ability.getName().equals(name)) {
                return ability;
            }
        }

        return NONE;
    }
}
