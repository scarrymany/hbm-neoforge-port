package com.hbm.blocks.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.util.TagsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Crate {@link BlockItem} with the slot-count/percentage-used tooltip, ported from CE's
 * {@code BlockStorageCrate.addInformation} (read in full - the slot-count/percentage half of it;
 * see below for what's dropped).
 *
 * <h2>Narrowed vs. CE</h2>
 * CE's tooltip also lists up to 10 of the crate's actual contained items by display name (reading
 * each {@code slotN} compound back into a real {@link ItemStack} via {@code new ItemStack(NBTTagCompound)}).
 * Reconstructing an {@link ItemStack} from raw NBT in modern Minecraft needs a
 * {@link net.minecraft.core.HolderLookup.Provider} (registry access) that an {@code Item}'s static
 * tooltip method has no confirmed, safe way to obtain in this sandbox (no existing call site in this
 * port's item catalog was found using {@code TooltipContext}'s registry accessor to cross-check the
 * exact method against). Rather than risk an unconfirmed-API compile failure for a cosmetic nice-to-have,
 * this class ships the slot-count/percentage line only (self-contained - just counts populated
 * {@code slotN} keys, no stack reconstruction needed) and drops the per-item content listing; CE's
 * "spiders inside" mob-vault Easter egg branch is also dropped, since it belongs to a loot-table/
 * mob-vault feature this port has no equivalent of yet (see {@code CrateBlockEntity}'s own javadoc).
 */
public class CrateBlockItem extends BlockItem {

    private final int totalSlots;

    public CrateBlockItem(CrateBlock block, Properties properties) {
        super(block, properties);
        this.totalSlots = block.getCrateType().slots;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        CompoundTag root = TagsUtil.hasCustomData(stack) ? TagsUtil.getCustomData(stack) : null;
        CompoundTag crateData = root != null && root.contains(IPersistentNBT.NBT_PERSISTENT_KEY)
                ? root.getCompound(IPersistentNBT.NBT_PERSISTENT_KEY) : root;

        int slotCount = 0;
        if (crateData != null) {
            for (int i = 0; i < totalSlots; i++) {
                if (crateData.contains("slot" + i)) slotCount++;
            }
        }

        float percent = totalSlots > 0 ? Math.round(slotCount * 1000F / totalSlots) / 10F : 0F;
        ChatFormatting color = percent >= 75 ? ChatFormatting.RED : percent < 25 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        tooltip.add(Component.literal(slotCount + "/" + totalSlots + " slots used (" + percent + "%)").withStyle(color));

        // CE BlockStorageCrate.java:239-240
        if (crateData != null && crateData.contains("lock")) {
            tooltip.add(Component.literal("This container is locked.").withStyle(ChatFormatting.RED));
        }
    }
}
