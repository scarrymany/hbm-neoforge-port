package com.hbm.items.weapon.sedna.hud;

/**
 * Reusable {@link IHUDComponent} instances, narrowly mirroring the handful of constants CE keeps on
 * its client-only {@code com.hbm.items.weapon.sedna.factory.LegoClient} ({@code HUD_COMPONENT_*}
 * fields) - this port has no port of {@code LegoClient} itself (that class is overwhelmingly bullet-
 * renderer wiring, out of this area's scope, see {@code docs/phase5/hud_overlays_geiger_armor_gun.md}),
 * so only the HUD-relevant subset lives here instead.
 * <p>
 * <b>Real, documented scope cut</b>: CE's {@code GunFactoryClient.init()} (255 lines) calls
 * {@code GunConfig#hud(...)} by hand for every one of CE's ~60 Sedna guns, individually choosing
 * {@link #AMMO} vs. {@link #AMMO_NOCOUNTER} vs. the mirrored pair for dual-receiver/akimbo weapons.
 * Reproducing that gun-by-gun list is real work belonging to whichever package owns this port's gun
 * item registration (out of this HUD-rendering task's own scope - see this task's structured-output
 * notes). Until that per-gun wiring lands, {@link #DEFAULT} ({@link #DURABILITY} + {@link #AMMO}) -
 * the pair CE itself gives the overwhelming majority of its guns - is used by
 * {@code ItemGunBaseNT#renderHUD} as a fallback for any gun whose {@code GunConfig} has not had
 * {@code .hud(...)} called at all, so every Sedna gun in this port shows a real, correctly-reading
 * ammo counter + durability bar today rather than nothing.
 */
public final class HUDComponents {

    private HUDComponents() {
    }

    public static final IHUDComponent DURABILITY = new HUDComponentDurabilityBar();
    public static final IHUDComponent DURABILITY_MIRROR = new HUDComponentDurabilityBar(true);

    public static final IHUDComponent AMMO = new HUDComponentAmmoCounter(0);
    public static final IHUDComponent AMMO_NOCOUNTER = new HUDComponentAmmoCounter(0).noCounter();
    public static final IHUDComponent AMMO_MIRROR = new HUDComponentAmmoCounter(0).mirror();

    /** Fallback default for any {@code GunConfig} with no {@code .hud(...)} call of its own - see class javadoc. */
    public static final IHUDComponent[] DEFAULT = {DURABILITY, AMMO};
}
