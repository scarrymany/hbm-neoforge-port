package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.render.misc.RenderScreenOverlay;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.GunConfig} (168 lines) - per-gun-mode "DNA": the
 * receivers array, durability, draw/inspect durations, crosshair choice, and the click/decider lambda
 * slots. See {@code docs/phase3/gun_framework.md}'s Package B table, read in full.
 * <p>
 * <b>Every getter routes through {@link XWeaponModManager#eval}</b> (Package C, now landed) before
 * returning the raw {@code _DNA} field - i.e. every single config value on every gun is
 * mod-overridable, matching CE's own {@code GunConfig} 1:1. {@code XWeaponModManager.eval} is a pure
 * pass-through when the stack carries no installed-mod list for this config index, so an unmodified
 * gun behaves exactly as it did before this wiring landed.
 * <p>
 * Not ported from CE's own field set (see {@code docs/phase3/gun_framework.md}'s Deferred scope):
 * {@code animations_DNA}/{@code getAnims}/{@code anim(...)} (needs
 * {@code com.hbm.render.anim.sedna.BusAnimationSedna}, unported Phase 5 keyframe-animation data - see
 * {@code docs/phase3/weapon_animation_hooks.md}) and {@code hudComponents_DNA}/
 * {@code getHUDComponents}/{@code hud(...)} (needs {@code com.hbm.items.weapon.sedna.hud.IHUDComponent},
 * unported Phase 5 HUD-widget package). Both were purely client-rendering config slots in CE (their
 * own field comments in CE say as much - {@code smokeHandler_DNA} is commented "Handles smoke
 * clientside" right next to them) with no bearing on the fire/reload state machine itself. Because
 * neither field/getter exists here, any weapon mod whose CE {@code eval()} body branches on
 * {@code GunConfig.FUN_ANIMNATIONS}/{@code O_HUDCOMPONENTS} (bayonet/sawed-off/speedloader mods'
 * custom animation overrides) simply never has that branch reached in this port - documented per-mod
 * where it matters, not a silent gap.
 */
public class GunConfig {

    /* MOD-EVAL KEYS - string identity, not value, is what {@link XWeaponModManager#eval} matches on;
     * every key here mirrors CE's own {@code GunConfig} constant name 1:1 so a mod's {@code eval()}
     * body ported from CE needs no key renaming. */
    public static final String O_RECEIVERS = "O_RECEIVERS";
    public static final String F_DURABILITY = "F_DURABILITY";
    public static final String I_DRAWDURATION = "I_DRAWDURATION";
    public static final String I_INSPECTDURATION = "I_INSPECTDURATION";
    public static final String I_INSPECTCANCEL = "I_INSPECTCANCEL";
    public static final String O_CROSSHAIR = "O_CROSSHAIR";
    public static final String B_HIDECROSSHAIR = "B_HIDECROSSHAIR";
    public static final String B_THERMALSIGHTS = "B_THERMALSIGHTS";
    public static final String B_RELOADREQUIRESTYPECHANGE = "B_RELOADREQUIRESTYPECHANGE";
    public static final String B_RELOADANIMATIONSEQUENTIAL = "B_RELOADANIMATIONSEQUENTIAL";
    /** CE also has {@code O_SCOPETEXTURE} here (a {@code ResourceLocation} field/getter) - not ported: this class has no {@code scopeTexture_DNA} field (Phase 5 rendering, see class javadoc), but the key string is still exposed so a scope mod's {@code eval()} can match on it (the branch is simply never read by anything yet). */
    public static final String O_SCOPETEXTURE = "O_SCOPETEXTURE";
    public static final String CON_SMOKE = "CON_SMOKE";
    public static final String CON_ORCHESTRA = "CON_ORCHESTRA";
    public static final String CON_ONPRESSPRIMARY = "CON_ONPRESSPRIMARY";
    public static final String CON_ONPRESSSECONDARY = "CON_ONPRESSSECONDARY";
    public static final String CON_ONPRESSTERTIARY = "CON_ONPRESSTERTIARY";
    public static final String CON_ONPRESSRELOAD = "CON_ONPRESSRELOAD";
    public static final String CON_ONRELEASEPRIMARY = "CON_ONRELEASEPRIMARY";
    public static final String CON_ONRELEASESECONDARY = "CON_ONRELEASESECONDARY";
    public static final String CON_ONRELEASETERTIARY = "CON_ONRELEASETERTIARY";
    public static final String CON_ONRELEASERELOAD = "CON_ONRELEASERELOAD";
    public static final String CON_DECIDER = "CON_DECIDER";
    /** Key string only - see class javadoc, no {@code animations_DNA} field/getter exists to route through it yet. */
    public static final String FUN_ANIMNATIONS = "FUN_ANIMNATIONS";
    /** Key string only - see class javadoc, no {@code hudComponents_DNA} field/getter exists to route through it yet. */
    public static final String O_HUDCOMPONENTS = "O_HUDCOMPONENTS";

    /* FIELDS */

    public int index;
    /** Receivers used by the gun; primary and secondary are usually indices 0 and 1 respectively, if applicable. */
    protected Receiver[] receivers_DNA;
    protected float durability_DNA;
    protected int drawDuration_DNA = 0;
    protected int inspectDuration_DNA = 0;
    protected boolean inspectCancel_DNA = true;
    protected RenderScreenOverlay.Crosshair crosshair_DNA;
    protected boolean hideCrosshair_DNA = true;

    protected boolean thermalSights_DNA = false;
    protected boolean reloadRequiresTypeChange_DNA = false;
    protected boolean reloadAnimationsSequential_DNA;
    /** Handles smoke clientside - see this class's javadoc for why no default lambda is ported yet. */
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> smokeHandler_DNA;
    /** Triggers during reloads, playing sounds depending on reload progress. */
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> orchestra_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressPrimary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressSecondary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressTertiary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressReload_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleasePrimary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleaseSecondary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleaseTertiary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleaseReload_DNA;
    /** The engine for the state machine that determines the gun's overall behavior. */
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> decider_DNA;

    /* GETTERS - every value routes through XWeaponModManager.eval, see class javadoc */

    public Receiver[] getReceivers(ItemStack stack) { return XWeaponModManager.eval(receivers_DNA, stack, O_RECEIVERS, this, this.index); }
    public float getDurability(ItemStack stack) { return XWeaponModManager.eval(durability_DNA, stack, F_DURABILITY, this, this.index); }
    public int getDrawDuration(ItemStack stack) { return XWeaponModManager.eval(drawDuration_DNA, stack, I_DRAWDURATION, this, this.index); }
    public int getInspectDuration(ItemStack stack) { return XWeaponModManager.eval(inspectDuration_DNA, stack, I_INSPECTDURATION, this, this.index); }
    public boolean getInspectCancel(ItemStack stack) { return XWeaponModManager.eval(inspectCancel_DNA, stack, I_INSPECTCANCEL, this, this.index); }
    @Nullable
    public RenderScreenOverlay.Crosshair getCrosshair(ItemStack stack) { return XWeaponModManager.eval(crosshair_DNA, stack, O_CROSSHAIR, this, this.index); }
    public boolean getHideCrosshair(ItemStack stack) { return XWeaponModManager.eval(hideCrosshair_DNA, stack, B_HIDECROSSHAIR, this, this.index); }
    public boolean hasThermalSights(ItemStack stack) { return XWeaponModManager.eval(thermalSights_DNA, stack, B_THERMALSIGHTS, this, this.index); }
    public boolean getReloadChangesType(ItemStack stack) { return XWeaponModManager.eval(reloadRequiresTypeChange_DNA, stack, B_RELOADREQUIRESTYPECHANGE, this, this.index); }
    public boolean getReloadAnimSequential(ItemStack stack) { return XWeaponModManager.eval(reloadAnimationsSequential_DNA, stack, B_RELOADANIMATIONSEQUENTIAL, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getSmokeHandler(ItemStack stack) { return XWeaponModManager.eval(smokeHandler_DNA, stack, CON_SMOKE, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getOrchestra(ItemStack stack) { return XWeaponModManager.eval(orchestra_DNA, stack, CON_ORCHESTRA, this, this.index); }

    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressPrimary(ItemStack stack) { return XWeaponModManager.eval(onPressPrimary_DNA, stack, CON_ONPRESSPRIMARY, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressSecondary(ItemStack stack) { return XWeaponModManager.eval(onPressSecondary_DNA, stack, CON_ONPRESSSECONDARY, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressTertiary(ItemStack stack) { return XWeaponModManager.eval(onPressTertiary_DNA, stack, CON_ONPRESSTERTIARY, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressReload(ItemStack stack) { return XWeaponModManager.eval(onPressReload_DNA, stack, CON_ONPRESSRELOAD, this, this.index); }

    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleasePrimary(ItemStack stack) { return XWeaponModManager.eval(onReleasePrimary_DNA, stack, CON_ONRELEASEPRIMARY, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseSecondary(ItemStack stack) { return XWeaponModManager.eval(onReleaseSecondary_DNA, stack, CON_ONRELEASESECONDARY, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseTertiary(ItemStack stack) { return XWeaponModManager.eval(onReleaseTertiary_DNA, stack, CON_ONRELEASETERTIARY, this, this.index); }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseReload(ItemStack stack) { return XWeaponModManager.eval(onReleaseReload_DNA, stack, CON_ONRELEASERELOAD, this, this.index); }

    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getDecider(ItemStack stack) { return XWeaponModManager.eval(decider_DNA, stack, CON_DECIDER, this, this.index); }

    /* SETTERS - fluent builder, field-for-field port of CE's own */

    public GunConfig rec(Receiver... receivers) { this.receivers_DNA = receivers; for (Receiver r : receivers_DNA) r.parent = this; return this; }
    public GunConfig dura(float dura) { this.durability_DNA = dura; return this; }
    public GunConfig draw(int draw) { this.drawDuration_DNA = draw; return this; }
    public GunConfig inspect(int inspect) { this.inspectDuration_DNA = inspect; return this; }
    public GunConfig inspectCancel(boolean flag) { this.inspectCancel_DNA = flag; return this; }
    public GunConfig crosshair(RenderScreenOverlay.Crosshair crosshair) { this.crosshair_DNA = crosshair; return this; }
    public GunConfig hideCrosshair(boolean flag) { this.hideCrosshair_DNA = flag; return this; }

    public GunConfig thermalSights(boolean flag) { this.thermalSights_DNA = flag; return this; }
    public GunConfig reloadChangeType(boolean flag) { this.reloadRequiresTypeChange_DNA = flag; return this; }
    public GunConfig reloadSequential(boolean flag) { this.reloadAnimationsSequential_DNA = flag; return this; }

    public GunConfig smoke(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> smoke) { this.smokeHandler_DNA = smoke; return this; }
    public GunConfig orchestra(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> orchestra) { this.orchestra_DNA = orchestra; return this; }

    // press
    public GunConfig pp(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressPrimary_DNA = lambda; return this; }
    public GunConfig ps(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressSecondary_DNA = lambda; return this; }
    public GunConfig pt(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressTertiary_DNA = lambda; return this; }
    public GunConfig pr(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressReload_DNA = lambda; return this; }

    // release
    public GunConfig rp(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleasePrimary_DNA = lambda; return this; }
    public GunConfig rs(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleaseSecondary_DNA = lambda; return this; }
    public GunConfig rt(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleaseTertiary_DNA = lambda; return this; }
    public GunConfig rr(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleaseReload_DNA = lambda; return this; }

    // decider
    public GunConfig decider(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.decider_DNA = lambda; return this; }

    /**
     * Standard package for keybind handling and decider using LEGO prefabs: primary fire on LMB,
     * reload on R, aiming on MMB, and the standard decider (jamming + auto-fire handling).
     */
    public GunConfig setupStandardConfiguration() {
        this.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY);
        this.pr(Lego.LAMBDA_STANDARD_RELOAD);
        this.pt(Lego.LAMBDA_TOGGLE_AIM);
        this.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER);
        return this;
    }
}
