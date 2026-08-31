package com.hbm.hazard.transformer;

import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.tool.ToolItems;
import com.hbm.util.ItemStackUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Sums contained-item radiation for storage crates, containment boxes and plastic bags.
 * <p>
 * <b>Deferred contract:</b> {@link BlockStorageCrate#CRATE_RAD_KEY} is expected to be a persistent
 * {@code DataComponentType<Double>} holder (the block area owns the exact storage format decision); this class
 * reads it via {@code ItemStack#getOrDefault}, mirroring how {@link com.hbm.hazard.HazardComponents} exposes its own
 * components.
 */
public class HazardTransformerRadiationContainer implements IHazardTransformer {

    @Override
    public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {

        final boolean isCrate = stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlockStorageCrate;
        final boolean isBox = stack.getItem() == ToolItems.CONTAINMENT_BOX.get();
        final boolean isBag = stack.getItem() == ToolItems.PLASTIC_BAG.get();

        if (!isCrate && !isBox && !isBag) return;
        // Mirrors a CE quirk: this second guard makes the isBag branch below unreachable, so plastic
        // bags never actually get contained-item radiation applied. Preserved intentionally for parity.
        if (!isCrate && !isBox) return;

        double radiation = 0D;

        if (isCrate) {
            radiation = stack.getOrDefault(BlockStorageCrate.CRATE_RAD_KEY.get(), 0D);
        }

        if (isBox) {
            final ItemStack[] contained = ItemStackUtil.readStacksFromNBT(stack);
            if (contained == null) return;

            for (final ItemStack held : contained) {
                if (held != null) {
                    radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
                }
            }

            radiation = Math.sqrt(radiation);
        }

        if (isBag) {
            final ItemStack[] contained = ItemStackUtil.readStacksFromNBT(stack);
            if (contained == null) return;

            for (final ItemStack held : contained) {
                if (held != null) {
                    radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
                }
            }

            radiation *= 2D;
        }

        if (radiation > 0) {
            entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
        }
    }
}
