package com.hbm.items.machine;

import com.hbm.blockentity.network.PipeBaseBlockEntity;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.IItemControlReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Port of CE {@code ItemFluidIDMulti} (upstream/hbm-ce/.../ItemFluidIDMulti.java, 247 lines) - a
 * player-configurable fluid-type identifier item holding two {@link FluidType}s (primary/secondary) in
 * NBT, cycling them via right-click, with a GUI for direct selection.
 * <p>
 * <b>Core behavior</b> (ported):
 * <ul>
 *   <li>Right-click (not sneaking): swap primary ↔ secondary, play sound, show toast (CE :79-90)</li>
 *   <li>Sneak + right-click: open {@code GUIScreenFluid} for direct fluid selection (CE :92-95, :239-247)</li>
 *   <li>Tooltip: shows primary + secondary fluid names (CE :111-117)</li>
 *   <li>{@link IItemFluidIdentifier#getType}: returns primary type (CE :129-131)</li>
 *   <li>NBT storage: {@code fluid1}/{@code fluid2} for primary/secondary (CE :189-203)</li>
 *   <li>Use on pipe: set type / sneak-spread (CE :206-218, :220-236)</li>
 * </ul>
 * <p>
 * <b>Deferred</b> (cite TODO(CE)):
 * <ul>
 *   <li>Creative subtypes for every fluid (CE :63-75) - NeoForge 1.21 uses different API</li>
 *   <li>Custom model with tinted overlay (CE :140-186) - simplified to basic item + ComponentPatch color</li>
 * </ul>
 */
public class ItemFluidIDMulti extends Item implements IItemFluidIdentifier, IItemControlReceiver {

    public ItemFluidIDMulti(Properties props) {
        super(props);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // CE :92-95: sneak + right-click opens GUIScreenFluid (client-only Screen, no Menu)
        if (level.isClientSide && player.isCrouching()) {
            com.hbm.client.ClientScreens.fluidIdentifier(player);
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // CE :79-90: right-click swaps primary ↔ secondary
        if (!level.isClientSide && !player.isCrouching()) {
            FluidType primary = getType(stack, true);
            FluidType secondary = getType(stack, false);
            setType(stack, secondary, true);
            setType(stack, primary, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(secondary.getLocalizedName(), true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        // CE :111-117: tooltip shows primary + secondary
        tooltip.add(Component.translatable(this.getDescriptionId() + ".info"));
        tooltip.add(Component.literal("   ").append(getType(stack, true).getLocalizedName()));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".info2"));
        tooltip.add(Component.literal("   ").append(getType(stack, false).getLocalizedName()));
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(@NotNull ItemStack stack) {
        // CE :119-125: hasContainerItem + getContainerItem (returns copy for crafting)
        return stack.copy();
    }

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public FluidType getType(Level level, BlockPos pos, ItemStack stack) {
        // CE :129-131: IItemFluidIdentifier.getType returns primary
        return getType(stack, true);
    }

    // CE :188-203: NBT storage for primary/secondary types (NeoForge 1.21: CustomData component)
    public static void setType(ItemStack stack, FluidType type, boolean primary) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        int id = type.getID();
        tag.putInt("fluid" + (primary ? 1 : 2), id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static FluidType getType(ItemStack stack, boolean primary) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains("fluid" + (primary ? 1 : 2))) {
            return Fluids.NONE;
        }
        int id = tag.getInt("fluid" + (primary ? 1 : 2));
        return Fluids.fromID(id);
    }

    /** CE {@code ItemFluidIDMulti.onItemUse} :206-218. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof PipeBaseBlockEntity duct)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            FluidType handType = getType(level, pos, context.getItemInHand());
            if (handType != duct.getFluidType()) {
                Player player = context.getPlayer();
                if (player != null && player.isCrouching()) {
                    spreadType(level, pos, handType, duct.getFluidType(), 256);
                } else {
                    duct.setType(handType);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** CE {@code ItemFluidIDMulti.spreadType} :220-236. */
    public static void spreadType(Level level, BlockPos pos, FluidType hand, FluidType pipe, int remaining) {
        if (remaining <= 0) return;
        if (!(level.getBlockEntity(pos) instanceof PipeBaseBlockEntity duct)) return;
        if (duct.getFluidType() != pipe) return;
        duct.setType(hand);
        spreadType(level, pos.offset(1, 0, 0), hand, pipe, remaining - 1);
        spreadType(level, pos.offset(0, 1, 0), hand, pipe, remaining - 1);
        spreadType(level, pos.offset(0, 0, 1), hand, pipe, remaining - 1);
        spreadType(level, pos.offset(-1, 0, 0), hand, pipe, remaining - 1);
        spreadType(level, pos.offset(0, -1, 0), hand, pipe, remaining - 1);
        spreadType(level, pos.offset(0, 0, -1), hand, pipe, remaining - 1);
    }

    /**
     * CE ItemFluidIDMulti.receiveControl (CE :101-109) — GUI writes {@code primary}/{@code secondary}
     * fluid IDs onto the held identifier via {@link com.hbm.packet.toserver.ItemControlPacket}.
     */
    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        if (data.contains("primary")) {
            setType(stack, Fluids.fromID(data.getInt("primary")), true);
        }
        if (data.contains("secondary")) {
            setType(stack, Fluids.fromID(data.getInt("secondary")), false);
        }
    }
}
