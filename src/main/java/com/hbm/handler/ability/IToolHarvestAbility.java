package com.hbm.handler.ability;

import com.hbm.config.ToolConfig;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.util.TagsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * What happens to a block once an {@link IToolAreaAbility} (or plain single-block mining)
 * confirms it should be harvested. Ported from CE's {@code com.hbm.handler.ability.IToolHarvestAbility}.
 *
 * <p>{@link #SILK} and {@link #LUCK} are full, self-contained ports (temporarily bump the held
 * tool's enchantment level around the harvest, exactly like CE). {@link #SMELTER} is a full port
 * using the vanilla {@link net.minecraft.world.item.crafting.RecipeManager} smelting lookup in
 * place of CE's {@code FurnaceRecipes.instance()} - a direct, self-contained equivalent.
 *
 * <p>{@link #SHREDDER}, {@link #CENTRIFUGE}, {@link #CRYSTALLIZER} and {@link #MERCURY} are
 * registered (name + sort order, so {@link AvailableAbilities}/{@link ToolPreset} and the tool
 * items that reference them by name still compile and their presets still round-trip through
 * NBT), but their actual conversion logic is not implemented: CE's originals read
 * {@code ShredderRecipes}/{@code CentrifugeRecipes} (Phase 2 machine-recipe tables),
 * ore-dict-tag-driven crystal lookup, and a hardcoded {@code ModItems.ingot_mercury} field, none
 * of which exist in this port yet - genuinely not-yet-ported systems, not something this area can
 * fabricate. Falling back to {@link #NONE}'s default harvest (plain break-and-drop) for these four
 * is a deliberate, explicit choice, not a silent stub: whichever phase ports the shredder/
 * centrifuge/crystallizer/mercury systems should fill in {@code onHarvestBlock} here.
 */
public interface IToolHarvestAbility extends IBaseAbility {

    default void preHarvestAll(int level, Level world, Player player, ItemStack tool) {
    }

    default void postHarvestAll(int level, Level world, Player player, ItemStack tool) {
    }

    /** You must call {@link #harvestBlock} to actually break the block, or visual glitches ensue. */
    default void onHarvestBlock(Level world, BlockPos pos, Player player, BlockPos refPos) {
        BlockState state = world.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) world, refPos, world.getBlockEntity(refPos), player, player.getMainHandItem());
        harvestBlock(world, pos, player, refPos, drops);
    }

    default void harvestBlock(Level world, BlockPos pos, Player player, BlockPos refPos, List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                Block.popResource(world, pos, stack);
            }
        }
        world.removeBlock(pos, false);

        ItemToolAbility.damageTool(player.getMainHandItem(), player, 1);
    }

    default ItemStack getSmeltingResult(Level world, ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input.copy());

        for (var holder : world.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
            var recipe = holder.value();
            if (!recipe.matches(recipeInput, world)) {
                continue;
            }

            ItemStack result = recipe.getResultItem(world.registryAccess()).copy();
            if (result.isEmpty()) {
                return ItemStack.EMPTY;
            }

            result.setCount(result.getCount() * input.getCount());
            return result;
        }

        return ItemStack.EMPTY;
    }

    int SORT_ORDER_BASE = 100;
    String LUCK_BASE_FORTUNE_KEY = "hbm_luck_base_fortune";

    IToolHarvestAbility NONE = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE;
        }
    };

    IToolHarvestAbility SILK = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.silktouch";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_SILK.get();
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 1;
        }

        @Override
        public void preHarvestAll(int level, Level world, Player player, ItemStack tool) {
            if (tool.isEmpty()) return;
            Holder<Enchantment> silkTouch = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH);
            EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(silkTouch, 1));
        }

        @Override
        public void postHarvestAll(int level, Level world, Player player, ItemStack tool) {
            if (tool.isEmpty()) return;
            Holder<Enchantment> silkTouch = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH);
            EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(silkTouch, 0));
        }
    };

    IToolHarvestAbility LUCK = new IToolHarvestAbility() {
        private final int[] powerAtLevel = { 1, 2, 3, 4, 5, 9 };

        @Override
        public String getName() {
            return "tool.ability.luck";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_LUCK.get();
        }

        @Override
        public int levels() {
            return powerAtLevel.length;
        }

        @Override
        public String getExtension(int level) {
            return " (" + powerAtLevel[level] + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 2;
        }

        @Override
        public void preHarvestAll(int level, Level world, Player player, ItemStack tool) {
            if (tool.isEmpty()) return;

            Holder<Enchantment> fortune = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE);
            int baseFortune = EnchantmentHelper.getItemEnchantmentLevel(fortune, tool);
            int abilityFortune = powerAtLevel[level];

            CompoundTag tag = TagsUtil.getCustomData(tool);
            tag.putInt(LUCK_BASE_FORTUNE_KEY, baseFortune);
            TagsUtil.putCustomData(tool, tag);

            EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(fortune, Math.max(baseFortune, abilityFortune)));
        }

        @Override
        public void postHarvestAll(int level, Level world, Player player, ItemStack tool) {
            if (tool.isEmpty()) return;

            Holder<Enchantment> fortune = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE);
            CompoundTag tag = TagsUtil.getCustomData(tool);
            int baseFortune = tag.contains(LUCK_BASE_FORTUNE_KEY) ? tag.getInt(LUCK_BASE_FORTUNE_KEY) : 0;
            tag.remove(LUCK_BASE_FORTUNE_KEY);
            TagsUtil.putCustomData(tool, tag);

            EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(fortune, baseFortune));
        }
    };

    IToolHarvestAbility SMELTER = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.smelter";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_FURNACE.get();
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 3;
        }

        @Override
        public void onHarvestBlock(Level world, BlockPos pos, Player player, BlockPos refPos) {
            BlockState state = world.getBlockState(pos);
            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) world, refPos, world.getBlockEntity(refPos), player, player.getMainHandItem());

            for (int i = 0; i < drops.size(); i++) {
                ItemStack stack = drops.get(i);
                ItemStack smelted = getSmeltingResult(world, stack);
                if (!smelted.isEmpty()) {
                    drops.set(i, smelted);
                }
            }

            harvestBlock(world, pos, player, refPos, drops);
        }
    };

    IToolHarvestAbility SHREDDER = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.shredder";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_SHREDDER.get();
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 4;
        }
    };

    IToolHarvestAbility CENTRIFUGE = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.centrifuge";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_CENTRIFUGE.get();
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 5;
        }
    };

    IToolHarvestAbility CRYSTALLIZER = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.crystallizer";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_CRYSTALLIZER.get();
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 6;
        }
    };

    IToolHarvestAbility MERCURY = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.mercury";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.ABILITY_MERCURY.get();
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 7;
        }
    };

    IToolHarvestAbility[] abilities = { NONE, SILK, LUCK, SMELTER, SHREDDER, CENTRIFUGE, CRYSTALLIZER, MERCURY };

    static IToolHarvestAbility getByName(String name) {
        for (IToolHarvestAbility ability : abilities) {
            if (ability.getName().equals(name)) {
                return ability;
            }
        }

        return NONE;
    }
}
