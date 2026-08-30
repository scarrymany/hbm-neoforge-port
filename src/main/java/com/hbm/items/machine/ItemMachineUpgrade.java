package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Arrays.stream(this.descKeys)
                .map(key -> Component.literal(I18nUtil.resolveKey(key)).withStyle(ChatFormatting.GOLD))
                .forEach(tooltip::add);
    }

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
