package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.render.misc.RenderScreenOverlay;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.GunConfig} (168 lines) - per-gun-mode "DNA": the
 * receivers array, durability, draw/inspect durations, crosshair choice, and the click/decider lambda
 * slots. See {@code docs/phase3/gun_framework.md}'s Package B table, read in full.
 * <p>
 * Every getter here is a raw pass-through to the {@code _DNA} field (see {@link ItemGunBaseNT}'s
 * class javadoc for why - CE's {@code XWeaponModManager.eval(...)} wrapper is Package C, not ported
 * yet, and is documented as a pure pass-through when a stack has no mod NBT).
 * <p>
 * Not ported from CE's own field set (see {@code docs/phase3/gun_framework.md}'s Deferred scope):
 * {@code animations_DNA}/{@code getAnims}/{@code anim(...)} (needs
 * {@code com.hbm.render.anim.sedna.BusAnimationSedna}, unported Phase 5 keyframe-animation data - see
 * {@code docs/phase3/weapon_animation_hooks.md}) and {@code hudComponents_DNA}/
 * {@code getHUDComponents}/{@code hud(...)} (needs {@code com.hbm.items.weapon.sedna.hud.IHUDComponent},
 * unported Phase 5 HUD-widget package). Both were purely client-rendering config slots in CE (their
 * own field comments in CE say as much - {@code smokeHandler_DNA} is commented "Handles smoke
 * clientside" right next to them) with no bearing on the fire/reload state machine itself.
 */
public class GunConfig {

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

    /* GETTERS - raw pass-throughs, see class javadoc */

    public Receiver[] getReceivers(ItemStack stack) { return receivers_DNA; }
    public float getDurability(ItemStack stack) { return durability_DNA; }
    public int getDrawDuration(ItemStack stack) { return drawDuration_DNA; }
    public int getInspectDuration(ItemStack stack) { return inspectDuration_DNA; }
    public boolean getInspectCancel(ItemStack stack) { return inspectCancel_DNA; }
    @Nullable
    public RenderScreenOverlay.Crosshair getCrosshair(ItemStack stack) { return crosshair_DNA; }
    public boolean getHideCrosshair(ItemStack stack) { return hideCrosshair_DNA; }
    public boolean hasThermalSights(ItemStack stack) { return thermalSights_DNA; }
    public boolean getReloadChangesType(ItemStack stack) { return reloadRequiresTypeChange_DNA; }
    public boolean getReloadAnimSequential(ItemStack stack) { return reloadAnimationsSequential_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getSmokeHandler(ItemStack stack) { return smokeHandler_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getOrchestra(ItemStack stack) { return orchestra_DNA; }

    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressPrimary(ItemStack stack) { return onPressPrimary_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressSecondary(ItemStack stack) { return onPressSecondary_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressTertiary(ItemStack stack) { return onPressTertiary_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressReload(ItemStack stack) { return onPressReload_DNA; }

    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleasePrimary(ItemStack stack) { return onReleasePrimary_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseSecondary(ItemStack stack) { return onReleaseSecondary_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseTertiary(ItemStack stack) { return onReleaseTertiary_DNA; }
    @Nullable
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseReload(ItemStack stack) { return onReleaseReload_DNA; }

    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getDecider(ItemStack stack) { return decider_DNA; }

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
