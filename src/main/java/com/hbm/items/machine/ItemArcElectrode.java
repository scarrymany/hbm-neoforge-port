package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.world.item.ItemStack;

/**
 * Arc furnace electrode. CE modeled the four grades (graphite/lanthanium/desh/saturnite) as
 * metadata variants of one registry entry; each grade is its own registered item here, carrying
 * its max durability directly instead of looking it up from an enum by stack damage.
 * <p>
 * The arc furnace TE that actually calls {@link #damage(ItemStack)} is Phase 2 content - this
 * class only needs to keep the durability counter and bar correct on its own.
 */
public class ItemArcElectrode extends ItemBase {

    private final EnumElectrodeType type;

    public ItemArcElectrode(EnumElectrodeType type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public EnumElectrodeType getType() {
        return this.type;
    }

    public static int getDurability(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.ARC_ELECTRODE_DURABILITY.get(), 0);
    }

    /** @return true once the electrode has reached its max durability and is spent. */
    public static boolean damage(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemArcElectrode electrode)) return true;
        int durability = getDurability(stack) + 1;
        stack.set(MachineDataComponents.ARC_ELECTRODE_DURABILITY.get(), durability);
        return durability >= electrode.type.durability;
    }

    public static int getMaxDurability(ItemStack stack) {
        return stack.getItem() instanceof ItemArcElectrode electrode ? electrode.type.durability : 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDurability(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getDurability(stack) / (float) this.type.durability);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF4A00;
    }

    public enum EnumElectrodeType {
        GRAPHITE(10),
        LANTHANIUM(100),
        DESH(500),
        SATURNITE(1500);

        public static final EnumElectrodeType[] VALUES = values();

        public final int durability;

        EnumElectrodeType(int durability) {
            this.durability = durability;
        }
    }
}
