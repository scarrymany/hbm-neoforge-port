package com.hbm.items.special;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * One flattened {@code (BedrockOreType, BedrockOreGrade)} combination of CE's
 * {@code ItemBedrockOreNew}. CE packed type and grade into a single item's metadata
 * ({@code grade.ordinal() << 4 | type.ordinal()}); post-metadata, every one of the 156 combinations
 * is its own registered {@code Item} instance instead, with type/grade fixed at construction
 * (see {@link BedrockOreItems}).
 * <p>
 * Not reproduced here: CE's per-grade tint (recoloring one shared grayscale texture per type,
 * {@code BedrockOreColorHandler}) and per-grade {@link ProcessingTrait} overlay-layer baking
 * ({@code registerModels}/{@code bakeModels}, {@code IModel.retexture}) both belong to the runtime
 * dynamic-model system items_special.md's finding 6 flags as needing a full model/datagen redesign,
 * not a straight port - that is asset/rendering pipeline work, not item registration. The trait
 * tooltip (finding 6's other half, {@code addInformation}) is plain data and is ported below.
 */
public class ItemBedrockOre extends Item {

    private static final String NAME_KEY_PREFIX = "item.hbm.bedrock_ore_new.";
    private static final String TRAIT_KEY_PREFIX = "item.hbm.bedrock_ore_new.trait.";

    public final BedrockOreType type;
    public final BedrockOreGrade grade;

    public ItemBedrockOre(Properties properties, BedrockOreType type, BedrockOreGrade grade) {
        super(properties);
        this.type = type;
        this.grade = grade;
    }

    /**
     * Ported from CE's {@code ItemBedrockOreNew#getItemStackDisplayName}: the display name is
     * composed at read time from a type-name key and a grade-name key that takes the type name as
     * its {0} parameter, rather than baked into a per-registry-entry lang key. Kept independent of
     * {@link #getDescriptionId()} (which differs per flattened variant, e.g.
     * {@code bedrock_ore_new_base_light}) so the 156 variants share the same 6 type + 26 grade lang
     * entries CE ships, instead of needing 156 distinct ones.
     */
    @Override
    public Component getName(ItemStack stack) {
        Component typeName = Component.translatable(NAME_KEY_PREFIX + "type." + type.suffix + ".name");
        return Component.translatable(NAME_KEY_PREFIX + "grade." + grade.name().toLowerCase(Locale.ROOT) + ".name", typeName);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (ProcessingTrait trait : grade.traits) {
            tooltip.add(Component.translatable(TRAIT_KEY_PREFIX + trait.name().toLowerCase(Locale.ROOT)));
        }
    }
}
