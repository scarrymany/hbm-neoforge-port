package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Core "insert into any machine" upgrade chip. CE identity-checked {@code this ==
 * ModItems.upgrade_speed_1} (etc.) to pick a hardcoded speed value and a set of {@code desc.*}
 * translation keys per instance; both become constructor parameters here instead, avoiding an
 * identity dependency between this class and {@code ModItems} (which registers it).
 * <p>
 * CE's {@code IUpgradeInfoProvider}/{@code IUpgradeInfoProviderSource} GUI tooltip lookup is
 * dropped: per the porting plan it is a defensive, null-checked optional enhancement (falls back to
 * static text whenever no machine GUI is open), not a hard requirement, and both interfaces belong
 * to not-yet-ported Phase 2 GUI/machine content. The static fallback tooltip (the same {@code
 * desc.*} keys CE fell back to) is fully reproduced.
 */
public class ItemMachineUpgrade extends ItemBase {

    private final UpgradeType type;
    private final int tier;
    private final int speed;
    private final String[] descKeys;

    public ItemMachineUpgrade(UpgradeType type, int tier, int speed, Properties properties, String... descKeys) {
        super(properties);
        this.type = type;
        this.tier = tier;
        this.speed = speed;
        this.descKeys = descKeys;
    }

    public UpgradeType getType() {
        return this.type;
    }

    public int getTier() {
        return this.tier;
    }

    /** Machine speed multiplier this upgrade grants, or 0 if it grants none (e.g. non-speed upgrades). */
    public int getSpeed() {
        return this.speed;
    }

    /** Exact CE {@code ItemMachineUpgrade.getSpeed(ItemStack)} :60-71. */
    public static int getSpeed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return stack.getItem() instanceof ItemMachineUpgrade upgrade ? upgrade.getSpeed() : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Arrays.stream(this.descKeys)
                .map(key -> Component.literal(I18nUtil.resolveKey(key)).withStyle(ChatFormatting.GOLD))
                .forEach(tooltip::add);
    }

    /**
     * CE {@code ItemMachineUpgrade.scrapItems} ({@code ItemMachineUpgrade.java:172-185}).
     * {@code Blocks.GRASS} (1.12 grass block) → {@code GRASS_BLOCK}. Lazy so {@code stone_gneiss}
     * is resolved after registry freeze.
     */
    public static boolean isScrapItem(Item item) {
        return scrapItems().contains(item);
    }

    public static Set<Item> scrapItems() {
        if (SCRAP_ITEMS == null) {
            Set<Item> set = new HashSet<>();
            set.add(Blocks.GRASS_BLOCK.asItem());
            set.add(Blocks.DIRT.asItem());
            set.add(Blocks.STONE.asItem());
            set.add(Blocks.COBBLESTONE.asItem());
            set.add(Blocks.SAND.asItem());
            set.add(Blocks.SANDSTONE.asItem());
            set.add(Blocks.GRAVEL.asItem());
            set.add(Blocks.NETHERRACK.asItem());
            set.add(Blocks.END_STONE.asItem());
            set.add(Items.FLINT);
            set.add(Items.SNOWBALL);
            set.add(Items.WHEAT_SEEDS);
            set.add(Items.STICK);
            Item gneiss = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "stone_gneiss"));
            if (gneiss != Items.AIR) set.add(gneiss);
            SCRAP_ITEMS = set;
        }
        return SCRAP_ITEMS;
    }

    private static Set<Item> SCRAP_ITEMS;

    public enum UpgradeType {
        SPEED,
        EFFECT,
        POWER,
        FORTUNE,
        AFTERBURN,
        OVERDRIVE,
        NULLIFIER,
        SCREAM,
        SPECIAL
    }
}
