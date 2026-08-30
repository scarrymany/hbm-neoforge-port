package com.hbm.items.weapon.grenade;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.IEquipReceiver;
import com.hbm.items.ItemBase;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.weapon.grenade.ItemGrenadeUniversal} (289 lines) - the crafted,
 * thrown item. {@code setHasSubtypes(false)} in CE (one registry item, all variance in NBT) becomes,
 * in this port, one registry item with all variance in the {@link GrenadeLoadout} data component -
 * the direct structural translation, not a re-derivation.
 * <p>
 * <b>Charge-up/equip-draw timer</b> reuses this port's already-committed
 * {@link HbmLivingAttachment#getGrenadeDeployment()}/{@code setGrenadeDeployment(int)} directly (per
 * {@code docs/phase3/grenades.md}'s "Key design/API decisions" - no new attachment field needed).
 * <p>
 * <b>"Is this stack held" tracking</b> gets its own {@link GrenadeDataComponents#EQUIPPED} component
 * rather than reusing {@code com.hbm.items.weapon.sedna.ItemGunBaseNT.getIsEquipped/setIsEquipped}
 * the way CE's own code does - see {@link GrenadeDataComponents}'s javadoc for why that CE reuse was
 * incidental code sharing, not a real gun/grenade coupling this port needs to preserve.
 * <p>
 * <b>Not ported (Phase 5 client scope, see {@code docs/phase3/grenades.md}'s Deferred scope):</b>
 * {@code IAnimatedItem}/{@code getAnimation()}'s 4 per-shell {@code BusAnimation} equip-bob/ring-spin
 * sequences (depend on {@code com.hbm.render.anim.BusAnimation}, not ported anywhere in this tree)
 * and the {@code sendEquipAnimation}/{@code HbmEffectNT.Anim} equip-cue trigger packet. The shell-
 * specific reload-cue *sounds* (revolver-cock/bolt-open/etc. tick thresholds) are real gameplay-
 * adjacent audio cues, not rendering, and are kept.
 */
public class ItemGrenadeUniversal extends ItemBase implements IEquipReceiver {

    /**
     * {@code stacksTo(4)} matches {@link GrenadeLoadout#DEFAULT}'s FRAG shell limit - used only as
     * the item's own default-component fallback for a stack built without going through
     * {@link #make}. Every stack {@link #make} actually produces overrides this per-stack via the
     * real 1.20.5+ stack-size mechanism, {@code DataComponents.MAX_STACK_SIZE} (count became a data
     * component in that update; there is no {@code Item#getMaxStackSize(ItemStack)} instance-level
     * override point to hook into instead) - see {@link #make} for where each shell's actual limit
     * (1/2/4) is applied.
     */
    public ItemGrenadeUniversal(Properties properties) {
        super(properties.stacksTo(4));
    }

    public static GrenadeLoadout getLoadout(ItemStack stack) {
        return stack.getOrDefault(GrenadeDataComponents.LOADOUT.get(), GrenadeLoadout.DEFAULT);
    }

    public static EnumGrenadeShell getShell(ItemStack stack) {
        return getLoadout(stack).shell();
    }

    public static EnumGrenadeFilling getFilling(ItemStack stack) {
        return getLoadout(stack).filling();
    }

    public static EnumGrenadeFuze getFuze(ItemStack stack) {
        return getLoadout(stack).fuze();
    }

    @Nullable
    public static EnumGrenadeExtra getExtra(ItemStack stack) {
        return getLoadout(stack).extra();
    }

    @Override
    public void onEquip(Player player, InteractionHand hand) {
        resetDeployment(player);
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        resetDeployment(player);
    }

    private static void resetDeployment(Player player) {
        HbmLivingAttachment.getData(player).setGrenadeDeployment(0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GrenadeLoadout loadout = getLoadout(stack);

        if (HbmLivingAttachment.getData(player).getGrenadeDeployment() >= loadout.shell().getDrawDuration()) {
            if (!level.isClientSide()) {
                EntityGrenadeUniversal grenade = new EntityGrenadeUniversal(level, player, stack, hand);
                level.addFreshEntity(grenade);
            }
            if (!player.isCreative()) stack.shrink(1);
            if (!stack.isEmpty()) this.onEquip(player, hand);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        InteractionHand heldHand = getHeldHand(player, stack);
        boolean actuallyHeld = heldHand != null;
        boolean wasHeld = getIsEquipped(stack);

        if (!wasHeld && actuallyHeld) {
            this.onEquip(player, heldHand);
        } else if (isDeploymentOwner(player, heldHand)) {
            HbmLivingAttachment props = HbmLivingAttachment.getData(player);
            int deployment = props.getGrenadeDeployment() + 1;
            props.setGrenadeDeployment(deployment);

            EnumGrenadeShell shell = getLoadout(stack).shell();
            if (shell == EnumGrenadeShell.FRAG && deployment == 18) {
                playCue(level, player, HBMSoundHandler.revolverCock, 1F);
            }
            if (shell == EnumGrenadeShell.STICK && (deployment == 16 || deployment == 25)) {
                playCue(level, player, HBMSoundHandler.boltOpen, 1.25F);
            }
            if (shell == EnumGrenadeShell.TECH && deployment == 18) {
                playCue(level, player, HBMSoundHandler.grenadeTech, 1F);
            }
            if (shell == EnumGrenadeShell.NUKE && deployment == 26) {
                playCue(level, player, HBMSoundHandler.grenadeNuka, 1F);
            }
        }

        setIsEquipped(stack, actuallyHeld);
    }

    @Nullable
    private static InteractionHand getHeldHand(Player player, ItemStack stack) {
        if (player.getMainHandItem() == stack) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem() == stack) return InteractionHand.OFF_HAND;
        return null;
    }

    /** CE: only one hand advances the shared deployment counter per tick - main hand always wins if both hands somehow hold a grenade. */
    private static boolean isDeploymentOwner(Player player, @Nullable InteractionHand heldHand) {
        if (heldHand == null) return false;
        if (heldHand == InteractionHand.MAIN_HAND) return true;
        return !(player.getMainHandItem().getItem() instanceof ItemGrenadeUniversal);
    }

    private static void playCue(Level level, Player player, Holder<SoundEvent> event, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), event, SoundSource.PLAYERS, 1F, pitch);
    }

    public static boolean getIsEquipped(ItemStack stack) {
        return stack.getOrDefault(GrenadeDataComponents.EQUIPPED.get(), false);
    }

    public static void setIsEquipped(ItemStack stack, boolean value) {
        stack.set(GrenadeDataComponents.EQUIPPED.get(), value);
    }

    public static ItemStack make(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze) {
        return make(shell, filling, fuze, null, 1);
    }

    public static ItemStack make(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, @Nullable EnumGrenadeExtra extra) {
        return make(shell, filling, fuze, extra, 1);
    }

    public static ItemStack make(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, @Nullable EnumGrenadeExtra extra, int amount) {
        ItemStack stack = new ItemStack(GrenadeItems.GRENADE_UNIVERSAL.get(), amount);
        stack.set(GrenadeDataComponents.LOADOUT.get(), new GrenadeLoadout(shell, filling, fuze, extra));
        // CE: getItemStackLimit(stack) returns shell.getStackLimit() dynamically. 1.21.1's stack size
        // is a real per-stack Data Component (DataComponents.MAX_STACK_SIZE, part of the 1.20.5 "count
        // is a component" refactor) rather than an Item-level override point - set directly here so
        // ItemStack#getMaxStackSize() reflects this specific shell.
        stack.set(DataComponents.MAX_STACK_SIZE, shell.getStackLimit());
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GrenadeLoadout loadout = getLoadout(stack);
        tooltip.add(Component.translatable(GrenadeItems.shellItem(loadout.shell()).getDescriptionId()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(GrenadeItems.fillingItem(loadout.filling()).getDescriptionId()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(GrenadeItems.fuzeItem(loadout.fuze()).getDescriptionId()).withStyle(ChatFormatting.YELLOW));
        if (loadout.extra() != null) {
            tooltip.add(Component.translatable(GrenadeItems.extraItem(loadout.extra()).getDescriptionId()).withStyle(ChatFormatting.RED));
        }
    }
}
