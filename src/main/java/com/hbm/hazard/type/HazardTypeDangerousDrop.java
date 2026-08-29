package com.hbm.hazard.type;

import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.ObjDoubleConsumer;

/**
 * Parametric hazard type that runs a caller-supplied consumer on ground contact. Used in CE's bulk registration for
 * the demon core open-&gt;closed transition.
 */
public class HazardTypeDangerousDrop implements IHazardType {

    private final ObjDoubleConsumer<ItemEntity> onDroppedItemUpdate;

    public HazardTypeDangerousDrop(final ObjDoubleConsumer<ItemEntity> onDrop) {
        this.onDroppedItemUpdate = onDrop;
    }

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
        // Nothing
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
        onDroppedItemUpdate.accept(item, level);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[" + I18nUtil.resolveKey("trait.drop") + "]").withStyle(ChatFormatting.RED));
    }
}
