package com.hbm.items.tool;

import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registration for {@link ItemMultitoolPassive}'s 8 rungs (CE: {@code multitool_ext} through
 * {@code multitool_decon}) - rungs 3-10 of the {@code multitool_dig}/{@code multitool_silk}
 * sneak-click upgrade ladder. {@link ItemMultitoolPassive} itself was already fully ported (see its
 * own javadoc) but - per {@code docs/phase3/melee_weapons.md}'s own finding #3 and
 * {@code com.hbm.items.weapon.WeaponMeleeItems}'s javadoc - was never actually wired into
 * registration, since doing so needs a {@link DeferredItem} reference into {@code ToolItems}
 * (concurrently owned by the mining-tool-ability area) to close the ladder's loop back to
 * {@code multitool_dig}. New, separate registration class, same one-file-per-package-slice
 * convention as {@code MilitaryC2Items}/{@code DetonatorItems}.
 * <p>
 * <b>Creative tab placement matches CE exactly: none.</b> CE's real {@code ModItems.java} calls
 * {@code .setCreativeTab(null)} on all 8 of these (only {@code multitool_dig} itself, already
 * registered by {@code ToolItems}, has a real tab) - they are upgrade-chain-only items, reached by
 * sneak-right-clicking the previous rung, not creative-menu items. Do not add these to
 * {@code CreativeTabContents} - that would be a real behavioral deviation from CE, not a fix.
 * <p>
 * Per-rung right-click bodies, durability (5000, CE {@code setMaxDamage(5000)}), and attack-damage
 * values are already correct inside {@link ItemMultitoolPassive} itself - this class only supplies
 * the missing {@code (Rung, nextRung)} construction arguments and the registry ids.
 */
public final class MultitoolPassiveItems {

    private MultitoolPassiveItems() {
    }

    // Declared last-rung-first so each next-rung lambda reads a field that is already initialized
    // (javac treats the capture as an illegal forward reference even inside a lambda).
    /** Closes the loop back to Phase 1's already-registered {@code multitool_dig}. */
    public static final DeferredItem<Item> MULTITOOL_DECON =
            reg("multitool_decon", ItemMultitoolPassive.Rung.DECON, () -> ToolItems.MULTITOOL_DIG.get());
    public static final DeferredItem<Item> MULTITOOL_JOULE =
            reg("multitool_joule", ItemMultitoolPassive.Rung.JOULE, () -> MULTITOOL_DECON.get());
    public static final DeferredItem<Item> MULTITOOL_MEGA =
            reg("multitool_mega", ItemMultitoolPassive.Rung.MEGA, () -> MULTITOOL_JOULE.get());
    public static final DeferredItem<Item> MULTITOOL_SKY =
            reg("multitool_sky", ItemMultitoolPassive.Rung.SKY, () -> MULTITOOL_MEGA.get());
    public static final DeferredItem<Item> MULTITOOL_BEAM =
            reg("multitool_beam", ItemMultitoolPassive.Rung.BEAM, () -> MULTITOOL_SKY.get());
    public static final DeferredItem<Item> MULTITOOL_HIT =
            reg("multitool_hit", ItemMultitoolPassive.Rung.HIT, () -> MULTITOOL_BEAM.get());
    public static final DeferredItem<Item> MULTITOOL_MINER =
            reg("multitool_miner", ItemMultitoolPassive.Rung.MINER, () -> MULTITOOL_HIT.get());
    public static final DeferredItem<Item> MULTITOOL_EXT =
            reg("multitool_ext", ItemMultitoolPassive.Rung.EXT, () -> MULTITOOL_MINER.get());

    /** No-op body; referencing this class forces the static initializers above to run. */
    public static void registerAll() {
    }

    private static DeferredItem<Item> reg(String name, ItemMultitoolPassive.Rung rung, Supplier<? extends Item> nextRung) {
        return ModItems.ITEMS.register(name, () -> new ItemMultitoolPassive(new Item.Properties().durability(5000), rung, nextRung));
    }
}
