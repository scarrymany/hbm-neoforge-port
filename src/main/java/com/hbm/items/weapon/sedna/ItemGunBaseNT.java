package com.hbm.items.weapon.sedna;

import com.hbm.handler.HbmKeybinds;
import com.hbm.interfaces.IItemHUD;
import com.hbm.inventory.RecipesCommon;
import com.hbm.items.IEquipReceiver;
import com.hbm.items.IKeybindReceiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineInfinite;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.GunAnimationPayload;
import com.hbm.sound.AudioWrapper;
import com.hbm.util.BobMathUtil;
import com.hbm.weapon.anim.GunAnimationType;
import com.hbm.weapon.anim.HbmAnimationType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.ItemGunBaseNT} (496 lines) - the held-weapon item
 * class every CE gun (revolvers, rifles, shotguns, SMGs, miniguns, launchers, laser weapons, ...) is
 * a direct instance of. See {@code docs/phase3/gun_framework.md}'s Package B for the full research
 * this class implements; read in full before editing.
 * <p>
 * <b>Entity-agnostic by design - a real, load-bearing constraint, not an oversight.</b>
 * {@link #handleKeybind(LivingEntity, Container, ItemStack, HbmKeybinds.EnumKeybind, boolean)}
 * takes {@link LivingEntity} + {@link Container}, not {@link Player}, because CE's own
 * {@code EntityAIFireGun} drives the exact same state machine for mobs with a {@code null}
 * inventory (confirmed by the research report's grep of that AI class). The {@link Player}-typed
 * overload required by {@link IKeybindReceiver} is a thin wrapper around this one, supplying the
 * player's own inventory - it is not the "real" implementation.
 * <p>
 * <b>{@code GunConfig}/{@code Receiver} getters route through {@link XWeaponModManager#eval}</b> (see
 * {@link #getConfig} - {@code getConfig} itself is still a raw array index, but every value read off
 * the returned {@code GunConfig}/its {@code Receiver}s is mod-overridable). Package C (the weapon-mod
 * eval chain + concrete {@code WeaponMod*} effect classes + the attachment items themselves) has
 * landed - {@link XWeaponModManager#getUpgradeItems} now backs this class's own tooltip loop below.
 * <p>
 * <b>Deliberately not ported from CE</b> (see the research report's Package C/D "Not part of this
 * package" lists for why each belongs elsewhere): {@code defaultAmmo}/{@code setDefaultAmmo} (needs
 * {@code GunFactory.EnumAmmo} + {@code ModItems.ammo_standard}, Package D content); the
 * {@code GUIWeaponTable}-aware "this gun accepts:" tooltip branch (a dedicated weapon-table block/menu
 * this port doesn't have yet - see {@code WeaponModItems}'s own javadoc); {@code onBlockStartBreak}/
 * {@code onLeftClickEntity} (CE's own overrides both unconditionally
 * return {@code true}, i.e. "don't change vanilla's default behavior" - 1.21.1 already behaves that
 * way with no override present, so omitting them changes nothing observable); the smoke-node/
 * orchestra *default* lambda bodies (client-rendering feed data with zero gameplay effect - the
 * {@code GunConfig} slots those lambdas plug into are still ported, only CE's own
 * {@code Lego.LAMBDA_STANDARD_SMOKE} default implementation is deferred); {@code IHUDComponent}/
 * {@code BusAnimationSedna}-typed config slots (both classes are unported Phase 5 rendering
 * infrastructure, see {@code GunConfig}'s own javadoc).
 */
public class ItemGunBaseNT extends Item implements IKeybindReceiver, IEquipReceiver, IItemHUD {

    public static final List<ItemGunBaseNT> INSTANCES = new ArrayList<>();
    public static final List<Item> secrets = new ArrayList<>();
    private static final DecimalFormatSymbols SYMBOLS_US = new DecimalFormatSymbols(Locale.US);
    /** Tooltip damage-number formatter (CE: {@code #.##}, US decimal point regardless of client locale). */
    public static final DecimalFormat FORMAT_DMG = new DecimalFormat("#.##", SYMBOLS_US);

    /** Timestamp (ms) of each config index's most recent shot - used by rendering (muzzle flash/smoke), not by the state machine itself. */
    public long[] lastShot;
    /** [0;1] randomized every shot for various rendering applications. */
    public double shotRand = 0D;

    /** Weapon-mod items this gun recognizes, for the (not yet ported) weapon-mod-table tooltip; harmless empty list until Package C populates it. */
    public List<RecipesCommon.ComparableStack> recognizedMods = new ArrayList<>();

    public static final ConcurrentHashMap<LivingEntity, AudioWrapper> loopedSounds = new ConcurrentHashMap<>();

    /** Client-side camera-kick effect only (see the research report's headline finding #3) - has zero effect on where a bullet actually goes; {@code Lego.calcSpread} already resolved that before any {@code fire()} lambda calls this. Shared mutable static state, matching CE exactly. */
    public static float recoilVertical = 0;
    public static float recoilHorizontal = 0;
    public static float recoilDecay = 0.75F;
    public static float recoilRebound = 0.25F;
    public static float offsetVertical = 0;
    public static float offsetHorizontal = 0;

    public static void setupRecoil(float vertical, float horizontal, float decay, float rebound) {
        recoilVertical += vertical;
        recoilHorizontal += horizontal;
        recoilDecay = decay;
        recoilRebound = rebound;
    }

    public static void setupRecoil(float vertical, float horizontal) {
        setupRecoil(vertical, horizontal, 0.75F, 0.25F);
    }

    public static float prevAimingProgress;
    public static float aimingProgress;

    /** NEVER ACCESS DIRECTLY - USE {@link #getConfig}. */
    protected final GunConfig[] configs_DNA;
    public Function<ItemStack, String> nameMutator;
    public final WeaponQuality quality;

    /**
     * A raw array index - {@code GunConfig} instances themselves are never swapped out by a weapon
     * mod (only the values read off one, via {@link XWeaponModManager#eval}, are - see this class's
     * javadoc).
     */
    public GunConfig getConfig(ItemStack stack, int index) {
        return configs_DNA[index];
    }

    public int getConfigCount() {
        return configs_DNA.length;
    }

    public ItemGunBaseNT(Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties.stacksTo(1));
        this.configs_DNA = cfg;
        this.quality = quality;
        this.lastShot = new long[cfg.length];
        for (int i = 0; i < cfg.length; i++) cfg[i].index = i;
        if (quality == WeaponQuality.LEGENDARY || quality == WeaponQuality.SECRET) secrets.add(this);
        INSTANCES.add(this);
    }

    public enum WeaponQuality {
        A_SIDE,
        B_SIDE,
        LEGENDARY,
        UTILITY,
        SPECIAL,
        SECRET,
        DEBUG
    }

    public enum GunState {
        /** Forced delay where nothing can be done. */
        DRAWING,
        /** The gun is ready to fire or reload. */
        IDLE,
        /** Forced delay, but with option for refire. */
        COOLDOWN,
        /** Forced delay after which a reload action happens, may be canceled. */
        RELOADING,
        /** Forced delay due to jamming. */
        JAMMED;

        public static final GunState[] VALUES = values();
    }

    public ItemGunBaseNT setNameMutator(Function<ItemStack, String> lambda) {
        this.nameMutator = lambda;
        return this;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (this.nameMutator != null) {
            String key = this.nameMutator.apply(stack);
            if (key != null) return Component.translatable(key + ".name");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        int configs = this.configs_DNA.length;
        for (int i = 0; i < configs; i++) {
            GunConfig config = getConfig(stack, i);
            for (Receiver rec : config.getReceivers(stack)) {
                IMagazine<?> mag = rec.getMagazine(stack);
                if (mag == null) continue;

                float dmg = rec.getBaseDamage(stack);
                tooltip.add(Component.translatable("desc.gun.base_damage", FORMAT_DMG.format(dmg)));

                if (!(mag instanceof MagazineInfinite)) {
                    // Live "N / capacity" HUD text needs a real player Container (see IMagazine's
                    // reportAmmoStateForHUD), which appendHoverText has no generic access to (it is
                    // called for tooltips rendered with no specific viewer in context on some paths) -
                    // deferred to whichever Phase 5 package renders the item's actual HUD ammo counter.
                    Object type = mag.getType(stack, null);
                    if (type instanceof BulletConfig bullet) {
                        int min = (int) (bullet.projectilesMin * rec.getSplitProjectiles(stack));
                        int max = (int) (bullet.projectilesMax * rec.getSplitProjectiles(stack));
                        String countSuffix = min > 1 ? (min != max ? (" x" + min + "-" + max) : (" x" + min)) : "";
                        tooltip.add(Component.translatable("desc.gun.damage_with_ammo",
                                FORMAT_DMG.format(dmg * bullet.damageMult) + countSuffix));
                    }
                }
            }

            float maxDura = config.getDurability(stack);
            if (maxDura > 0) {
                int dura = Mth.clamp((int) ((maxDura - getWear(stack, i)) * 100 / maxDura), 0, 100);
                tooltip.add(Component.translatable("desc.gun.condition", dura));
            }

            for (ItemStack upgrade : XWeaponModManager.getUpgradeItems(stack, i)) {
                tooltip.add(upgrade.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
            }
        }

        switch (this.quality) {
            case A_SIDE -> tooltip.add(Component.translatable("desc.gun.quality.a_side").withStyle(ChatFormatting.YELLOW));
            case B_SIDE -> tooltip.add(Component.translatable("desc.gun.quality.b_side").withStyle(ChatFormatting.GOLD));
            case LEGENDARY -> tooltip.add(Component.translatable("desc.gun.quality.legendary").withStyle(ChatFormatting.RED));
            case SPECIAL -> tooltip.add(Component.translatable("desc.gun.quality.special").withStyle(ChatFormatting.AQUA));
            case UTILITY -> tooltip.add(Component.translatable("desc.gun.quality.utility").withStyle(ChatFormatting.GREEN));
            case SECRET -> tooltip.add(Component.translatable("desc.gun.quality.secret").withStyle(BobMathUtil.getBlink() ? ChatFormatting.DARK_RED : ChatFormatting.RED));
            case DEBUG -> tooltip.add(Component.translatable("desc.gun.quality.debug").withStyle(BobMathUtil.getBlink() ? ChatFormatting.YELLOW : ChatFormatting.GOLD));
        }

        // CE also shows a "this gun accepts:" list of recognizedMods while a GUIWeaponTable screen is
        // open (Minecraft.getMinecraft().currentScreen instanceof GUIWeaponTable) - that screen (a
        // dedicated weapon-table block/menu) is not ported yet, see WeaponModItems's own javadoc;
        // recognizedMods itself is populated correctly by XWeaponModManager.init() already.
    }

    @Override
    public boolean canHandleKeybind(Player player, ItemStack stack, HbmKeybinds.EnumKeybind keybind) {
        return keybind == HbmKeybinds.EnumKeybind.GUN_PRIMARY
                || keybind == HbmKeybinds.EnumKeybind.GUN_SECONDARY
                || keybind == HbmKeybinds.EnumKeybind.GUN_TERTIARY
                || keybind == HbmKeybinds.EnumKeybind.RELOAD;
    }

    @Override
    public void handleKeybind(Player player, ItemStack stack, HbmKeybinds.EnumKeybind keybind, boolean newState) {
        handleKeybind(player, player.getInventory(), stack, keybind, newState);
    }

    /**
     * The real implementation - see this class's javadoc for why it is {@link LivingEntity} +
     * {@link Container}, not {@link Player}-only. {@code inventory} may be {@code null} for a
     * mob-held gun, exactly as CE's {@code EntityAIFireGun} calls it.
     */
    public void handleKeybind(LivingEntity entity, @Nullable Container inventory, ItemStack stack, HbmKeybinds.EnumKeybind keybind, boolean newState) {
        int configs = this.configs_DNA.length;

        for (int i = 0; i < configs; i++) {
            GunConfig config = getConfig(stack, i);
            LambdaContext ctx = new LambdaContext(config, entity, inventory, i);

            if (keybind == HbmKeybinds.EnumKeybind.GUN_PRIMARY && newState && !getPrimary(stack, i)) {
                if (config.getPressPrimary(stack) != null) config.getPressPrimary(stack).accept(stack, ctx);
                setPrimary(stack, i, true);
                continue;
            }
            if (keybind == HbmKeybinds.EnumKeybind.GUN_PRIMARY && !newState && getPrimary(stack, i)) {
                if (config.getReleasePrimary(stack) != null) config.getReleasePrimary(stack).accept(stack, ctx);
                setPrimary(stack, i, false);
                continue;
            }
            if (keybind == HbmKeybinds.EnumKeybind.GUN_SECONDARY && newState && !getSecondary(stack, i)) {
                if (config.getPressSecondary(stack) != null) config.getPressSecondary(stack).accept(stack, ctx);
                setSecondary(stack, i, true);
                continue;
            }
            if (keybind == HbmKeybinds.EnumKeybind.GUN_SECONDARY && !newState && getSecondary(stack, i)) {
                if (config.getReleaseSecondary(stack) != null) config.getReleaseSecondary(stack).accept(stack, ctx);
                setSecondary(stack, i, false);
                continue;
            }
            if (keybind == HbmKeybinds.EnumKeybind.GUN_TERTIARY && newState && !getTertiary(stack, i)) {
                if (config.getPressTertiary(stack) != null) config.getPressTertiary(stack).accept(stack, ctx);
                setTertiary(stack, i, true);
                continue;
            }
            if (keybind == HbmKeybinds.EnumKeybind.GUN_TERTIARY && !newState && getTertiary(stack, i)) {
                if (config.getReleaseTertiary(stack) != null) config.getReleaseTertiary(stack).accept(stack, ctx);
                setTertiary(stack, i, false);
                continue;
            }
            if (keybind == HbmKeybinds.EnumKeybind.RELOAD && newState && !getReloadKey(stack, i)) {
                if (config.getPressReload(stack) != null) config.getPressReload(stack).accept(stack, ctx);
                setReloadKey(stack, i, true);
                continue;
            }
            if (keybind == HbmKeybinds.EnumKeybind.RELOAD && !newState && getReloadKey(stack, i)) {
                if (config.getReleaseReload(stack) != null) config.getReleaseReload(stack).accept(stack, ctx);
                setReloadKey(stack, i, false);
            }
        }
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        for (int i = 0; i < this.configs_DNA.length; i++) {
            playAnimation(player, stack, GunAnimationType.EQUIP, i);
            setPrimary(stack, i, false);
            setSecondary(stack, i, false);
            setTertiary(stack, i, false);
            setReloadKey(stack, i, false);
        }
    }

    /**
     * Triggers a {@link GunAnimationPayload} for a gun animation transition, following
     * {@code docs/phase3/weapon_animation_hooks.md}'s confirmed hook design. Matches CE's
     * {@code playAnimation} exactly, including the quirk that the whole thing (packet send AND the
     * {@code lastAnim}/{@code animTimer} bookkeeping) is a complete no-op unless {@code player} is a
     * real {@link ServerPlayer} - a mob-held gun (a null {@code player}, since {@code LambdaContext
     * .getPlayer()} returns null for non-{@link Player} shooters) never gets animation bookkeeping at
     * all in CE either.
     */
    public static void playAnimation(@Nullable Player player, ItemStack stack, HbmAnimationType type, int index) {
        if (player instanceof ServerPlayer serverPlayer) {
            GunAnimationPayload.triggerGunAnimation(serverPlayer, stack, InteractionHand.MAIN_HAND, type, t -> true);
            setLastAnim(stack, index, type);
            setAnimTimer(stack, index, 0);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {

        if (!(entity instanceof LivingEntity living)) return;
        Player player = entity instanceof Player p ? p : null;
        int confNo = this.configs_DNA.length;
        GunConfig[] configs = new GunConfig[confNo];
        LambdaContext[] ctx = new LambdaContext[confNo];
        for (int i = 0; i < confNo; i++) {
            configs[i] = this.getConfig(stack, i);
            ctx[i] = new LambdaContext(configs[i], living, player != null ? player.getInventory() : null, i);
        }

        if (level.isClientSide()) {

            if (isSelected && player == MainRegistry.proxy.me()) {

                /// AIMING ///
                prevAimingProgress = aimingProgress;
                boolean aiming = getIsAiming(stack);
                float aimSpeed = 0.25F;
                if (aiming && aimingProgress < 1F) aimingProgress += aimSpeed;
                if (!aiming && aimingProgress > 0F) aimingProgress -= aimSpeed;
                aimingProgress = Mth.clamp(aimingProgress, 0F, 1F);

                // CE also drives smoke-node particle bookkeeping (Lego.LAMBDA_STANDARD_SMOKE) here;
                // deferred, see this class's javadoc. The orchestra callback (per-config reload sound
                // cue timing) has no default implementation ported either, but the config slot itself
                // is still called for any Package D content that plugs its own lambda in.
                for (int i = 0; i < confNo; i++) {
                    if (configs[i].getOrchestra(stack) != null) configs[i].getOrchestra(stack).accept(stack, ctx[i]);
                }
            }
            return;
        }

        if (player != null) {
            boolean wasHeld = getIsEquipped(stack);
            if (!wasHeld && isSelected) {
                this.onEquip(player, stack);
            }
        }
        setIsEquipped(stack, isSelected);

        /// RESET WHEN NOT EQUIPPED ///
        if (!isSelected) {
            for (int i = 0; i < confNo; i++) {
                GunState current = getState(stack, i);
                if (current != GunState.JAMMED) {
                    setState(stack, i, GunState.DRAWING);
                    setTimer(stack, i, configs[i].getDrawDuration(stack));
                }
                setLastAnim(stack, i, GunAnimationType.CYCLE); // prevents new guns from initializing with DRAWING, 0
            }
            setIsAiming(stack, false);
            setReloadCancel(stack, false);
            return;
        }

        for (int i = 0; i < confNo; i++) {
            if (configs[i].getOrchestra(stack) != null) configs[i].getOrchestra(stack).accept(stack, ctx[i]);

            setAnimTimer(stack, i, getAnimTimer(stack, i) + 1);

            /// STATE MACHINE ///
            int timer = getTimer(stack, i);
            if (timer > 0) setTimer(stack, i, timer - 1);
            if (timer <= 1) configs[i].getDecider(stack).accept(stack, ctx[i]);
        }
    }

    // GUN DRAWN // (defined for parity with CE; CE itself never reads this flag back either)
    public static boolean getIsDrawn(ItemStack stack) { return stack.getOrDefault(GunDataComponents.DRAWN.get(), false); }
    public static void setIsDrawn(ItemStack stack, boolean value) { stack.set(GunDataComponents.DRAWN.get(), value); }
    // GUN STATE TIMER //
    public static int getTimer(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).timer(); }
    public static void setTimer(ItemStack stack, int index, int value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withTimer(value)); }
    // GUN STATE //
    public static GunState getState(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).state(); }
    public static void setState(ItemStack stack, int index, GunState value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withState(value)); }
    // GUN MODE //
    public static int getMode(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).mode(); }
    public static void setMode(ItemStack stack, int index, int value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withMode(value)); }
    // GUN AIMING //
    public static boolean getIsAiming(ItemStack stack) { return stack.getOrDefault(GunDataComponents.AIMING.get(), false); }
    public static void setIsAiming(ItemStack stack, boolean value) { stack.set(GunDataComponents.AIMING.get(), value); }
    // GUN WEAR //
    public static float getWear(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).wear(); }
    public static void setWear(ItemStack stack, int index, float value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withWear(value)); }
    // LOCKON //
    public static int getLockonTarget(ItemStack stack) { return stack.getOrDefault(GunDataComponents.LOCKON_TARGET.get(), 0); }
    public static void setLockonTarget(ItemStack stack, int value) { stack.set(GunDataComponents.LOCKON_TARGET.get(), value); }
    public static boolean getIsLockedOn(ItemStack stack) { return stack.getOrDefault(GunDataComponents.LOCKED_ON.get(), false); }
    public static void setIsLockedOn(ItemStack stack, boolean value) { stack.set(GunDataComponents.LOCKED_ON.get(), value); }
    // ANIM TRACKING // - raw ordinal into whichever HbmAnimationType enum the caller knows it is (see GunStateComponent's javadoc)
    public static int getLastAnim(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).lastAnim(); }
    public static void setLastAnim(ItemStack stack, int index, HbmAnimationType value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withAnim(((Enum<?>) value).ordinal())); }
    public static int getAnimTimer(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).animTimer(); }
    public static void setAnimTimer(ItemStack stack, int index, int value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withAnimTimer(value)); }

    // BUTTON STATES //
    public static boolean getPrimary(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).button(GunStateComponent.BUTTON_PRIMARY); }
    public static void setPrimary(ItemStack stack, int index, boolean value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withButton(GunStateComponent.BUTTON_PRIMARY, value)); }
    public static boolean getSecondary(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).button(GunStateComponent.BUTTON_SECONDARY); }
    public static void setSecondary(ItemStack stack, int index, boolean value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withButton(GunStateComponent.BUTTON_SECONDARY, value)); }
    public static boolean getTertiary(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).button(GunStateComponent.BUTTON_TERTIARY); }
    public static void setTertiary(ItemStack stack, int index, boolean value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withButton(GunStateComponent.BUTTON_TERTIARY, value)); }
    public static boolean getReloadKey(ItemStack stack, int index) { return GunDataComponents.getIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT).button(GunStateComponent.BUTTON_RELOAD); }
    public static void setReloadKey(ItemStack stack, int index, boolean value) { GunDataComponents.updateIndexed(stack, GunDataComponents.GUN_STATES, index, GunStateComponent.DEFAULT, s -> s.withButton(GunStateComponent.BUTTON_RELOAD, value)); }
    // RELOAD CANCEL //
    public static boolean getReloadCancel(ItemStack stack) { return stack.getOrDefault(GunDataComponents.RELOAD_CANCEL.get(), false); }
    public static void setReloadCancel(ItemStack stack, boolean value) { stack.set(GunDataComponents.RELOAD_CANCEL.get(), value); }
    // EQUIPPED //
    public static boolean getIsEquipped(ItemStack stack) { return stack.getOrDefault(GunDataComponents.EQUIPPED.get(), false); }
    public static void setIsEquipped(ItemStack stack, boolean value) { stack.set(GunDataComponents.EQUIPPED.get(), value); }

    /** Wrapper for extra context used in most Consumer lambdas which are part of the guncfg. */
    public static class LambdaContext {
        public final GunConfig config;
        public final LivingEntity entity;
        @Nullable
        public final Container inventory;
        public final int configIndex;

        public LambdaContext(GunConfig config, LivingEntity entity, @Nullable Container inventory, int configIndex) {
            this.config = config;
            this.entity = entity;
            this.inventory = inventory;
            this.configIndex = configIndex;
        }

        @Nullable
        public Player getPlayer() {
            if (!(entity instanceof Player)) return null;
            return (Player) entity;
        }
    }

    /**
     * Stub - see {@code docs/phase3/weapon_animation_hooks.md}'s Deferred scope: the crosshair/HUD
     * component render loop this replaces (CE's {@code renderHUD}) needs the not-yet-ported
     * {@code hud} package ({@code IHUDComponent}) and this port's own client-side crosshair renderer,
     * neither of which exist yet - Phase 5 scope. The hook point itself is wired (this class does
     * implement {@link IItemHUD}) so Phase 5 has a real, already-correct call site to fill in rather
     * than needing to add the {@code implements} clause itself.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderHUD(RenderGuiLayerEvent.Pre event, ResourceLocation layer, Player player, ItemStack stack, InteractionHand hand) {
        // Intentionally empty - see method javadoc.
    }
}
