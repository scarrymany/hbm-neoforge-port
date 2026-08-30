package com.hbm.items.tool;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.packet.toclient.GunAnimationPayload;
import com.hbm.util.EntityDamageUtil;
import com.hbm.weapon.anim.ToolAnimationType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemBoltgun} - a "nail gun" whose real
 * {@code onLeftClickEntity} behavior is a melee weapon: consume one bolt from the player's
 * inventory, deal 10 armor-piercing damage, play a hit sound/VFX cue. Per this package's task
 * (docs/phase1/items_special.md bucket b), only that behavior is ported here.
 * <p>
 * <b>Not ported</b> (documented, not silently dropped):
 * <ul>
 *     <li>{@code onItemUse}'s right-click-on-block "screw a bolt into an {@code IToolable} machine
 *     part, or convert a block via {@code NTMToolHandler}'s oredict conversion table" branch - pure
 *     Phase 2 machine-coupling content (per docs/phase1/items_tool.md's own bucket (c)
 *     classification of every other {@code ItemTooling}-family item), unrelated to this melee-scope
 *     package. Not overriding {@code useOn}/{@code onItemUseFirst} leaves the item a no-op on block
 *     right-click, which is the correct default until whoever ports the {@code IToolable}/
 *     {@code NTMToolHandler} bolt-conversion system wires it back in.</li>
 *     <li>CE's {@code EntityPlayer} "achievement" grant on beheading a player
 *     ({@code AdvancementManager.grantAchievement(..., achGoFish)}) - {@code AdvancementManager}
 *     does not exist in this port yet (same not-yet-ported system {@code ArmorFSB}'s javadoc already
 *     flags); the kill itself still happens, only the advancement grant is skipped.</li>
 *     <li>The {@code HbmEffectNT.VanillaExt_LargeExplode} hit-spark particle burst and the
 *     client-side "instant animation, no packet delay" optimization - {@code HbmEffectNT} is not
 *     part of this port (see {@code docs/phase3/weapon_animation_hooks.md}); this port's
 *     {@link ToolAnimationType}/{@link GunAnimationPayload} substitute is triggered instead (see
 *     {@link #onLeftClickEntity}), same substitution this package already made for
 *     {@code ItemChainsaw}/{@code ItemSwordCutter}.</li>
 *     <li>{@code DamageSource.causePlayerDamage(player).setDamageBypassesArmor()} - 1.21's
 *     bypasses-armor flag is a data-driven {@code DamageType} tag, not a per-instance setter (no
 *     equivalent exists without registering a new tagged {@code DamageType}, a
 *     {@code ModDamageTypes.java}/tag-datagen change out of this class's scope to make alone); this
 *     port uses {@code level.damageSources().playerAttack(player)} instead, matching the same
 *     documented "armor now mitigates this hit" deviation {@code ArmorFSB}'s hard-landing knockback
 *     javadoc already accepts for the identical 1.12-vs-1.21 API gap.</li>
 * </ul>
 */
public class ItemBoltgun extends Item {

    /** CE's actual live bolt pool (a {@code bolt_spike} entry is commented out in CE itself with a {@code //FIXME}). */
    private static final List<TagKey<Item>> BOLT_TAGS = List.of(
            MaterialShapes.BOLT.commonTag(Mats.MAT_STEEL),
            MaterialShapes.BOLT.commonTag(Mats.MAT_TUNGSTEN),
            MaterialShapes.BOLT.commonTag(Mats.MAT_DURA)
    );

    public ItemBoltgun(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        Level level = player.level();
        Inventory inventory = player.getInventory();

        for (var boltTag : BOLT_TAGS) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                if (slot.isEmpty() || !slot.is(boltTag)) {
                    continue;
                }

                if (!level.isClientSide()) {
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HBMSoundHandler.boltgun.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    inventory.removeItem(i, 1);

                    // CE: EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, DamageSource
                    // .causePlayerDamage(player).setDamageBypassesArmor(), 10F) - the bypasses-armor
                    // flag deviation is documented in the class javadoc; the ignore-invulnerability-
                    // window helper itself is used verbatim (confirmed real in this port already).
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, level.damageSources().playerAttack(player), 10F);

                    if (player instanceof ServerPlayer serverPlayer) {
                        GunAnimationPayload.triggerGunAnimation(serverPlayer, stack, InteractionHand.MAIN_HAND, ToolAnimationType.SWING, t -> true);
                    }
                }
                return true;
            }
        }

        return false;
    }
}
