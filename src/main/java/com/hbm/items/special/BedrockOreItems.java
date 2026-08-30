package com.hbm.items.special;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Flattened registration for CE's {@code ItemBedrockOreNew}: one metadata-multi item
 * ({@code meta = grade.ordinal() << 4 | type.ordinal()}) covering the full
 * {@link BedrockOreType} (6) x {@link BedrockOreGrade} (26) cross product. Confirmed dense by
 * reading CE's {@code getSubItems}/{@code registerModels}/{@code make()} (all iterate the complete
 * nested loop unconditionally, no combination is ever skipped) - 156 distinct registry entries
 * here, one per (type, grade) pair, plus CE's sibling {@code ItemBedrockOreBase}.
 * <p>
 * Registry ids follow {@code bedrock_ore_new_<grade>_<type>} to preserve CE's {@code
 * bedrock_ore_new} id lineage while guaranteeing uniqueness: {@link BedrockOreGrade#prefix} alone
 * is not unique (e.g. {@code BASE} and {@code BASE_ROASTED} both have prefix {@code "base"}), so
 * the full lowercase enum name is used instead - CE avoided the same collision for its own
 * texture-placeholder names by appending a numeric index (see {@code registerModels}); the full
 * grade name is the more legible register-id-safe substitute.
 * <p>
 * Both CE items were declared {@code setCreativeTab(MainRegistry.partsTab)} - confirmed directly
 * from CE source, not inferred from the shape-to-tab table in docs/phase1/creative_tabs_plan.md
 * (these are not {@code MaterialShapes}-generated items).
 */
public final class BedrockOreItems {

    private static final String REGISTRY_PREFIX = "bedrock_ore_new_";

    private static final Map<BedrockOreType, Map<BedrockOreGrade, DeferredItem<ItemBedrockOre>>> BY_TYPE_AND_GRADE =
            new EnumMap<>(BedrockOreType.class);

    public static final DeferredItem<ItemBedrockOreBase> BEDROCK_ORE_BASE =
            ModItems.ITEMS.registerItem("bedrock_ore_base", ItemBedrockOreBase::new);

    static {
        for (BedrockOreGrade grade : BedrockOreGrade.VALUES) {
            for (BedrockOreType type : BedrockOreType.VALUES) {
                String id = REGISTRY_PREFIX + grade.name().toLowerCase(Locale.ROOT) + "_" + type.suffix;
                DeferredItem<ItemBedrockOre> item = ModItems.ITEMS.registerItem(id,
                        properties -> new ItemBedrockOre(properties, type, grade));
                BY_TYPE_AND_GRADE.computeIfAbsent(type, t -> new EnumMap<>(BedrockOreGrade.class)).put(grade, item);
                CreativeTabContents.add(ModCreativeTabs.PARTS, item);
            }
        }
        CreativeTabContents.add(ModCreativeTabs.PARTS, BEDROCK_ORE_BASE);
    }

    /** Looks up the registered flattened item for a given (type, grade) pair. */
    public static DeferredItem<ItemBedrockOre> get(BedrockOreType type, BedrockOreGrade grade) {
        return BY_TYPE_AND_GRADE.get(type).get(grade);
    }

    /** No-op call target; referencing this class forces the static block above to run. */
    public static void bootstrap() {
    }

    private BedrockOreItems() {
    }
}
