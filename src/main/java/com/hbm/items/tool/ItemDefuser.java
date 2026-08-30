package com.hbm.items.tool;

import com.hbm.api.block.IToolable.ToolType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemDefuser} (read in full, 55 lines). Per
 * {@code docs/phase3/bomb_blocks_and_detonators.md}'s headline finding #2, this class contributes
 * <b>zero</b> lines to the actual "safely remove a placed bomb" mechanic - that whole path is
 * {@link com.hbm.api.block.IToolable}/{@code onScrew} dispatch, already inherited for free from
 * {@link ItemTooling#useOn}. The only code CE's real {@code ItemDefuser} writes is
 * {@code itemInteractionForEntity} (right-click-on-a-living-entity), ported below as
 * {@link #interactLivingEntity}.
 *
 * <p>Registered twice by {@link DetonatorItems} (CE: {@code defuser} at 100 durability,
 * {@code defuser_desh} at CE's {@code -1} "infinite" sentinel, mapped onto this port's own
 * established 1024-durability infinite-tool convention - see {@code CouplingToolItems}'
 * {@code screwdriver_desh}/{@code hand_drill_desh} for the precedent this follows).
 */
public class ItemDefuser extends ItemTooling {

    public ItemDefuser(ToolType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Creeper) {
            // TODO(bomb-blocks-and-detonators): CE's ItemDefuser.itemInteractionForEntity delegates
            // this branch to com.hbm.items.armor.ItemModDefuser.defuse(creeper, player, true) - pacify
            // the creeper (clear its ignited state, remove its swell AI task, drop a safety_fuse item,
            // deal 1 damage, apply 200-tick Weakness). ItemModDefuser (com.hbm.items.armor package,
            // the defuser_gold armor-mod trinket) has not been ported yet - out of scope for this
            // detonator/defuser-items pass per docs/phase3/bomb_blocks_and_detonators.md's explicit
            // recommendation. Wire this branch to ItemModDefuser.defuse(...) once that class lands.
            return InteractionResult.PASS;
        }

        // TODO(bomb-blocks-and-detonators): CE's ItemDefuser also handles a right-click-on-
        // EntityGlyphidNuclear branch (only while the mob is already dying: kill it, detonate a
        // small piercing ExplosionVNT, gib it via ConfettiUtil, drop NUKE_DEMO ammo). Deferred per
        // the report's explicit recommendation - it pulls in com.hbm.entity.mob.glyphid.
        // EntityGlyphidNuclear (glyphid-mob package, not yet ported) and
        // com.hbm.items.weapon.sedna.factory.{GunFactory.EnumAmmo, ConfettiUtil} plus
        // com.hbm.inventory.OreDictManager.DictFrame (Sedna gun-factory package, not yet ported).
        // Port this branch once both the glyphid-mob and Sedna gun-factory packages exist.

        return InteractionResult.PASS;
    }
}
