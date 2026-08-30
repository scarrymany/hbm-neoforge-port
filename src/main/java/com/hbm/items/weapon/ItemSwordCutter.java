package com.hbm.items.weapon;

import com.hbm.items.IEquipReceiver;
import com.hbm.items.tool.ItemSwordAbility;
import com.hbm.packet.toclient.GunAnimationPayload;
import com.hbm.weapon.anim.ToolAnimationType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.items.weapon.ItemSwordCutter}.
 * <p>
 * CE's real point of this class is a first-person-only "mob slicer" special attack: the client
 * tracks a click-drag screen-space plane ({@code startPos}/{@code planeNormal}/mouse yaw-pitch,
 * all {@code public static} fields on the item class itself - CE's own "this whole system is a
 * mess" comment), then sends a {@code PacketMobSlicer} carrying that plane to the server, which
 * geometrically cuts entities caught in it. None of that is ported here (documented, not silently
 * dropped): it needs a brand-new C2S packet ({@code PacketMobSlicer} has no equivalent anywhere in
 * this port or its network registry), a new client-side click/plane-tracking input state (Phase
 * 5-shaped, since it only matters to first-person rendering), and a geometry-cutting resolution
 * step this port's entity/damage systems have no hook for at all - a genuinely new feature, not an
 * adaptation of an existing one. What <b>is</b> ported: the plain-attack ability-sword behavior
 * (inherited from {@link ItemSwordAbility}) and the swing/equip animation triggers, using this
 * port's {@link ToolAnimationType} + {@link GunAnimationPayload} substitute for CE's
 * {@code HbmEffectNT.Anim} dispatch (see {@code docs/phase3/weapon_animation_hooks.md}) - so the
 * cutter still swings and equips like a real weapon, just without the plane-cut special attack.
 */
public class ItemSwordCutter extends ItemSwordAbility implements IEquipReceiver {

    public ItemSwordCutter(float damage, double movement, Tier tier, Properties properties) {
        super(damage, movement, tier, properties);
    }

    /** CE: {@code getTexId()} - selects which cutter texture/mode the (unported) mob-slicer packet uses. Kept for subclass parity ({@code ItemCrucible} overrides it), unused otherwise. */
    public byte getTexId() {
        return 0;
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            GunAnimationPayload.triggerGunAnimation(serverPlayer, stack, InteractionHand.MAIN_HAND, ToolAnimationType.EQUIP, t -> true);
        }
    }

    /**
     * CE: {@code onEntitySwing} sends the {@code "generic"}/{@code "cSwing"} animation trigger on
     * every swing (server-side only, guarded by {@code EntityPlayerMP}). Assumed real 1.21.1
     * {@code IItemExtension} hook shape ({@code onEntitySwing(ItemStack, LivingEntity)}, stack
     * first, matching every other {@code IItemExtension} method in this port's already-confirmed
     * usages) - not verified against a real compiled NeoForge jar in this sandbox (no cached
     * dependencies to check against, network access to Maven Central is blocked here); if the real
     * hook has a different name or parameter order, this override becomes dead code needing a
     * rename rather than a silent behavior change, since {@code @Override} would fail to compile.
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            GunAnimationPayload.triggerGunAnimation(serverPlayer, stack, InteractionHand.MAIN_HAND, ToolAnimationType.SWING, t -> true);
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        boolean mainhand = player.getMainHandItem() == stack;
        boolean wasEquipped = stack.getOrDefault(WeaponDataComponents.EQUIPPED.get(), false);
        boolean nowEquipped = isSelected && mainhand;

        if (nowEquipped && !wasEquipped) {
            onEquip(player, stack);
        }

        stack.set(WeaponDataComponents.EQUIPPED.get(), nowEquipped);
    }
}
