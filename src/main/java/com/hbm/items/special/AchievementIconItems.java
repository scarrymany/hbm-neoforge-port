package com.hbm.items.special;

import com.hbm.items.EffectItem;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Ported from CE's {@code ModItems.achievement_icon} (upstream/hbm-ce/src/main/java/com/hbm/items/
 * ModItems.java:2729: {@code new ItemEnumMulti<>("achievement_icon", EnumAchievementType.VALUES,
 * true, true).setCreativeTab(null)}) and {@code ModItems.nothing} (ModItems.java:2782: {@code new
 * EffectItem("nothing").setCreativeTab(null)}).
 * <p>
 * Both are purely decorative, never-obtainable-in-game GUI items whose sole real CE purpose is
 * serving as advancement-icon graphics. CE's {@code EnumAchievementType} (upstream/hbm-ce/src/main/
 * java/com/hbm/items/ItemEnums.java:76-89) has 10 ordinals - GOFISH(0), ACID(1), BALLS(2),
 * DIGAMMASEE(3), DIGAMMAFEEL(4), DIGAMMAKNOW(5), DIGAMMAKAUAIMOHO(6), DIGAMMAUPONTOP(7),
 * DIGAMMAFOROURRIGHT(8), QUESTIONMARK(9) - each formerly a 1.12.2 damage-value variant of the single
 * {@code achievement_icon} item. Per this port's established "1.12 metadata subtype -> separate
 * 1.21.1 item id" convention (see {@code ItemConserve}/{@code FoodItems}'s {@code canned_<name>}
 * flattening, cited by docs/phase5/advancement_and_recipe_datagen_assets.md section 1.5 as the
 * precedent to follow for this exact item), each ordinal becomes its own
 * {@code hbm:achievement_icon_<name>} registry entry rather than one item with 10 damage values
 * (1.21.1 has no metadata subtypes at all).
 * <p>
 * Added by task c15 (authoring {@code src/main/resources/data/hbm/advancement/*.json}) because 10 of
 * CE's 65 real advancement JSON files use one of these two items as their {@code display.icon}, and
 * neither {@code achievement_icon} nor {@code nothing} existed anywhere in this port before this pass
 * (confirmed by a repo-wide grep for {@code achievement_icon}/{@code EnumAchievementType}/the quoted
 * string {@code "nothing"} turning up zero registrations). Unlike the ~19 other CE items/blocks c15's
 * advancement port also found unregistered (real gameplay content - machine blocks, weapon parts,
 * materials - still owned by Phase 2/3/4, see c15's own task notes for the full list), these two are
 * pure advancement-system decoration with zero gameplay function: no recipe references them, no
 * machine consumes them, {@code setCreativeTab(null)} in CE means they were never even reachable from
 * the creative inventory. Registering them here stays inside c15's own subject matter (the
 * advancement system) rather than reaching into another phase's scope, and unblocks 10 files' icons
 * that would otherwise render as the missing-item placeholder.
 * <p>
 * Wiring: {@link #registerAll()} must be called once from {@code ModItems.register(IEventBus)} -
 * see this task's structured output {@code wiringSnippets} for the exact line (this file cannot
 * self-wire into the shared {@code ModItems} class per this wave's file-ownership rules).
 */
public final class AchievementIconItems {

    private AchievementIconItems() {
    }

    /** CE's {@code EffectItem("nothing")} - {@code achimpossible.json}'s icon, the only file using it. */
    public static DeferredItem<Item> NOTHING;

    public static void registerAll() {
        NOTHING = register("nothing", () -> new EffectItem(new Item.Properties()));

        // EnumAchievementType.VALUES order, verbatim (ordinal = CE's 1.12.2 "data" damage value).
        register("achievement_icon_gofish", basic());              // 0 - achgofish.json (icon omits "data", defaults to 0)
        register("achievement_icon_acid", basic());                 // 1 - achslimeball.json ("data": 1)
        register("achievement_icon_balls", basic());                // 2 - achsulfuric.json ("data": 2)
        register("achievement_icon_digammasee", basic());           // 3 - digammasee.json
        register("achievement_icon_digammafeel", basic());          // 4 - digammafeel.json
        register("achievement_icon_digammaknow", basic());          // 5 - digammaknow.json
        register("achievement_icon_digammakauaimoho", basic());     // 6 - digammakauaimoho.json
        register("achievement_icon_digammaupontop", basic());       // 7 - digammaupontop.json
        register("achievement_icon_digammaforourright", basic());   // 8 - unused by any of the 65 real files; registered anyway for full EnumAchievementType fidelity
        register("achievement_icon_questionmark", basic());         // 9 - achc20_5.json and bobhidden.json (both "data": 9)
    }

    private static Supplier<Item> basic() {
        return () -> new ItemBase(new Item.Properties());
    }

    private static DeferredItem<Item> register(String name, Supplier<Item> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
