package com.hbm.items.weapon.sedna.content;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory556mm} - the 5.56x45 ammo family
 * (4 {@link BulletConfig}s) and its 3 guns ({@code gun_g3}/{@code gun_g3_zebra}/{@code gun_stg77}).
 * See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory556mm} table for the verified stat source
 * this class implements.
 * <p>
 * <b>Eager construction, not CE's {@code init()} entry point.</b> CE calls
 * {@code XFactory556mm.init()} explicitly from {@code GunFactory.init()} at a point in CE's 1.12
 * lifecycle where {@code new Item(...)}/{@code new ItemGunBaseNT(...)} are fully-constructed,
 * immediately-registrable objects (1.12's {@code GameRegistry.register} has no deferred-supplier
 * concept). This port's items are registered via {@code DeferredRegister}, whose suppliers only run
 * during the item {@code RegisterEvent} - well after this class would need to be touched if ammo
 * construction were deferred into a gun's own supplier lambda. Rather than thread that timing
 * dependency through every gun family, every field below (raw ammo {@link Item}s, {@link BulletConfig}s,
 * and the 3 {@link ItemGunBaseNT} instances themselves) is a plain eager {@code static final} field,
 * exactly mirroring CE's own synchronous construction style - none of {@code Item}'s/
 * {@code ItemGunBaseNT}'s constructors touch any registry, so this is safe at any point during mod
 * construction. {@link com.hbm.items.weapon.sedna.content.GunRifleItems} wraps each of these already-built
 * instances in a trivial {@code () -> XFactory556mm.xxx} supplier for {@code DeferredRegister} - see
 * that class's own javadoc.
 * <p>
 * <b>Not ported (see this task's explicit scope-outs and the framework's own documented gaps):</b>
 * {@code .smoke(...)}/{@code .anim(...)}/{@code .orchestra(...)} calls (client-rendering keyframe/
 * audio-cue content - {@code GunConfig} has no {@code anim()} slot at all yet, per that class's own
 * javadoc, and no default {@code Lego}/{@code Orchestras} lambda exists for smoke/reload-audio either);
 * {@code .scopeTexture(...)} ({@code GunConfig} has no such setter yet); {@code setDefaultAmmo(...)}
 * ({@code ItemGunBaseNT} has no {@code defaultAmmo} field yet, see that class's javadoc); the
 * upgrade-aware {@code LAMBDA_NAME_G3} name mutator (needs {@code XWeaponModManager}, Package C, not
 * ported).
 */
public final class XFactory556mm {

    private XFactory556mm() {
    }

    // ==================== ammo ====================
    // .setCasing(...) intentionally omitted for every ammo config in this batch: it needs a shared
    // casing-item family (ItemEnums.EnumCasingType -> real Item per casing tier) that no Phase 3
    // package has registered yet, and minting one unilaterally here risks a duplicate-registration
    // race against whichever other concurrent "guns" package first defines e.g. casing_small - a
    // genuinely missing shared dependency, not invented behavior. The CE casing type + count for each
    // round is preserved in each field's comment below for whoever wires the shared casing family +
    // Ammo Press recipes.

    /** casing: SMALL x8 */
    public static final Item ITEM_R556_SP = new Item(new Item.Properties());
    /** casing: SMALL x8 */
    public static final Item ITEM_R556_FMJ = new Item(new Item.Properties());
    /** casing: SMALL x8 */
    public static final Item ITEM_R556_JHP = new Item(new Item.Properties());
    /** casing: SMALL_STEEL x8 */
    public static final Item ITEM_R556_AP = new Item(new Item.Properties());

    public static final BulletConfig r556_sp = new BulletConfig("r556_sp").setItem(ITEM_R556_SP);
    public static final BulletConfig r556_fmj = new BulletConfig("r556_fmj").setItem(ITEM_R556_FMJ)
            .setDamage(0.8F).setThresholdNegation(4F).setArmorPiercing(0.1F);
    public static final BulletConfig r556_jhp = new BulletConfig("r556_jhp").setItem(ITEM_R556_JHP)
            .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
    public static final BulletConfig r556_ap = new BulletConfig("r556_ap").setItem(ITEM_R556_AP)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F).setThresholdNegation(10F).setArmorPiercing(0.15F);

    // r556_inc_sp/fmj/jhp/ap (the "incendiary" set gun_g3_zebra's magazine references) are a CONFIRMED
    // upstream CE bug, verified by reading XFactory556mm.java lines 37-40/67 directly: the 4
    // "public static BulletConfig r556_inc_*" fields are DECLARED but NEVER ASSIGNED anywhere in the
    // entire hbm-ce source tree (grepped across all of upstream/hbm-ce - zero assignment sites). CE's
    // own MagazineSingleTypeBase.standardReload/getFirstConfig then call "config.ammo.matchesRecipe(...)"
    // on each entry of gun_g3_zebra's accepted-bullets list while scanning inventory slots with an
    // empty magazine - since those 4 entries are null, this NullPointerExceptions in real CE the
    // moment a player tries to reload gun_g3_zebra from empty. This is the same class of confirmed
    // "silently broken upstream content" this task's research report already documented for
    // gun_supershotgun/gun_vortex (see docs/phase3/guns_and_ammo.md's headline finding), not a
    // misreading. See gun_g3_zebra's construction below for how this port preserves the *observable*
    // behavior (a magazine that can never accept ammo) without introducing a null-pointer landmine
    // into this port's own null-tolerant magazine code.

    // ==================== recoil (client-side camera-kick only; see ItemGunBaseNT's own javadoc -
    // onRecoil_DNA is not called anywhere in the currently-landed Lego/GunStateDecider engine yet,
    // so wiring these has no observable effect today, but costs nothing and matches CE 1:1 for
    // whenever Phase 5's render loop picks getRecoil(...) back up) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_G3 =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_ZEBRA =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_STG = (stack, ctx) -> { };

    // ==================== guns ====================

    // Static METHODS, not fields: constructing an ItemGunBaseNT touches Receiver.sound(...).get(),
    // resolving a SoundEvent DeferredHolder. If these were eager `static final` fields, simply
    // loading this class (which GunRifleItems.registerAll() does, from ModItems.register(), itself
    // called from MainRegistry's constructor - i.e. during mod construction, strictly before any
    // RegisterEvent fires) would throw IllegalStateException. Methods defer construction until
    // GunRifleItems' registerGun(...) actually invokes them from inside a DeferredItem Supplier, at
    // RegisterEvent(ITEM) time - by which point RegisterEvent(SOUND_EVENT) has already completed.
    public static ItemGunBaseNT gun_g3() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(3_000).draw(10).inspect(33).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(5F).delay(2).auto(true).dry(15).spread(0.0F).reload(50).jam(47)
                            .sound(HBMSoundHandler.fireAssault.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(r556_sp, r556_fmj, r556_jhp, r556_ap))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_G3))
                    .setupStandardConfiguration().ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY)
            // default ammo (not yet wired - ItemGunBaseNT has no defaultAmmo field, see class javadoc): R556_SP x30
        );
    }

    public static ItemGunBaseNT gun_g3_zebra() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
            new GunConfig()
                    .dura(6_000).draw(10).inspect(33).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(7.5F).delay(2).auto(true).dry(15).spreadHipfire(0.01F).reload(50).jam(47)
                            .sound(HBMSoundHandler.fireSilenced.get(), 1.0F, 1.0F)
                            // Empty accepted-bullets list - see the r556_inc_* comment above: this
                            // preserves CE's confirmed-broken "can never load ammo" behavior for
                            // gun_g3_zebra without a null-pointer landmine. TODO(phase3-gun-content):
                            // if a future decision is made to fix-forward rather than preserve the
                            // bug, build 4 real r556_inc_sp/fmj/jhp/ap BulletConfigs here (modeled on
                            // r556_sp/fmj/jhp/ap plus an incendiary setOnImpact ignite effect) and
                            // addConfigs(...) them instead.
                            .mag(new MagazineFullReload(0, 30))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_ZEBRA))
                    .setupStandardConfiguration().ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY)
            // default ammo (not yet wired): R556_JHP x30 - note this default ammo is itself moot
            // in real CE too, since the zebra's magazine can never actually chamber it (see above).
        );
    }

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_STG77_DECIDER = (stack, ctx) -> {
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState lastState = ItemGunBaseNT.getState(stack, index);
        GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
        GunStateDecider.deciderStandardClearJam(stack, lastState, index);
        GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
        GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index, () -> ItemGunBaseNT.getSecondary(stack, index));
    };

    public static ItemGunBaseNT gun_stg77() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(3_000).draw(10).inspect(125).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(10F).delay(2).dry(15).auto(true).spread(0.0F).reload(46).jam(0)
                            .sound(HBMSoundHandler.fireAssault.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(r556_sp, r556_fmj, r556_jhp, r556_ap))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_STG))
                    // Matches CE exactly: NOT setupStandardConfiguration() - both primary and
                    // secondary clicks route to the same standard fire handler, and the custom
                    // decider's auto-refire condition checks the SECONDARY button state, not primary.
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                    .pr(Lego.LAMBDA_STANDARD_RELOAD).pt(Lego.LAMBDA_TOGGLE_AIM)
                    .decider(LAMBDA_STG77_DECIDER)
            // default ammo (not yet wired): R556_FMJ x30
        );
    }
}
