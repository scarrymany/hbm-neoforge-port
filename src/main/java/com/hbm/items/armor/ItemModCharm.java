package com.hbm.items.armor;

import com.hbm.damage.ModDamageTypes;
import com.hbm.handler.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ItemModCharm} (53 lines, read in full) - see
 * {@code docs/phase4/meteor_events.md}. Backs both {@code protection_charm} ("repel" branch of
 * {@link com.hbm.handler.MeteorStrikeHandler}) and {@code meteor_charm} ("no-strike" branch), one
 * class instantiated twice (see {@link ModCharmItems}) exactly as CE's real {@code ModItems.java}
 * does. CE tells the two apart with {@code this == ModItems.protection_charm}/
 * {@code this == ModItems.meteor_charm} identity checks against static fields on the shared
 * {@code ModItems} god-class; this port instead gives each instance its own
 * {@link #disablesStrikes} flag set at construction, avoiding any cross-reference back into the
 * registrar class from inside the item class itself.
 * <p>
 * {@link #modDamage} also carries CE's real, meteor-<i>unrelated</i>
 * {@code ModDamageSource.broadcast} mitigation (halve for {@code protection_charm}, negate for
 * {@code meteor_charm}) - a genuine 2-line CE behavior ported verbatim per this report's own
 * Deferred-scope note (the broadcaster tower itself, {@code TileEntityBroadcaster}, is out of this
 * package's scope; only the damage-mitigation branch is ported here).
 */
public class ItemModCharm extends ItemArmorMod {

    /** {@code false} for {@code protection_charm} ("repel" + halve broadcaster damage),
     *  {@code true} for {@code meteor_charm} ("no strike" + negate broadcaster damage). */
    public final boolean disablesStrikes;

    public ItemModCharm(Properties properties, boolean disablesStrikes) {
        // CE: ItemModCharm(String s) -> super(ArmorModHandler.helmet_only, false, true, false, false, s)
        // - occupies the "helmet_only" mod-slot INDEX but is applicable to CHESTPLATE armor pieces
        // (helmet=false, chestplate=true, legs=false, boots=false). Preserved verbatim, not a typo.
        super(properties, ArmorModHandler.helmet_only, false, true, false, false);
        this.disablesStrikes = disablesStrikes;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("You feel blessed.").withStyle(ChatFormatting.AQUA));

        if (!disablesStrikes) {
            components.add(Component.literal("Diverts meteors away from the player.").withStyle(ChatFormatting.AQUA));
            components.add(Component.literal("Meteors no longer destroy blocks.").withStyle(ChatFormatting.AQUA));
            components.add(Component.literal("Halves broadcaster damage").withStyle(ChatFormatting.AQUA));
        } else {
            components.add(Component.literal("Disables meteorite spawning.").withStyle(ChatFormatting.AQUA));
            components.add(Component.literal("Negates broadcaster damage").withStyle(ChatFormatting.AQUA));
        }

        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(Component.literal("  ").append(stack.getHoverName()).withStyle(ChatFormatting.GOLD));
    }

    /** CE: {@code ItemModCharm#modDamage} - broadcaster-damage mitigation, unrelated to meteors
     *  (see class javadoc). Not gated on {@code disablesStrikes} being the meteor mechanic - both
     *  charms react to {@code ModDamageTypes.BROADCAST} regardless of their meteor behavior. */
    @Override
    public void modDamage(LivingDamageEvent.Pre event, ItemStack armor) {
        if (event.getSource().is(ModDamageTypes.BROADCAST)) {
            if (!disablesStrikes) {
                event.setNewDamage(event.getNewDamage() * 0.5F);
            } else {
                event.setNewDamage(0F);
            }
        }
    }
}
