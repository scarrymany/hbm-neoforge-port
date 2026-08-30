package com.hbm.items.weapon.sedna.content;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryBlackPowder} - the flintlock
 * pepperbox and its 4-round STONE/STONE_AP/STONE_IRON/STONE_SHOT ammo family (no
 * {@code EnumCasingType} at all: black-powder rounds burn on fire, no casing item is ejected). See
 * {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryBlackPowder} table.
 * <p>
 * Ammo {@link Item}s and {@link BulletConfig}s are plain eager {@code static final} fields (see
 * {@code XFactory556mm}'s class javadoc for why that is safe: neither type touches any registry at
 * construction time). <b>The gun itself is deliberately NOT an eager field</b> - unlike the sibling
 * {@code XFactory556mm}/{@code XFactory762mm}/{@code XFactory50} content classes, which build
 * {@code ItemGunBaseNT} (and its {@code Receiver.sound(HBMSoundHandler.xxx.get(), ...)} call) as a
 * plain eager {@code static final} field. That pattern resolves a {@code DeferredHolder<SoundEvent,
 * SoundEvent>} the moment the class is first touched - and {@code registerAll()}-style "force this
 * class to load" calls run from {@code ModItems.register(modEventBus)}, itself called directly from
 * {@code MainRegistry}'s constructor, i.e. during mod construction, strictly BEFORE any
 * {@code RegisterEvent} fires for ANY registry (sound included). Resolving
 * {@code HBMSoundHandler.fireBlackPowder.get()} at that point throws {@code IllegalStateException} -
 * confirmed by this port's own established practice elsewhere (grepped every
 * {@code HBMSoundHandler.xxx.get()} call site outside this gun-content area: every one of them runs
 * from an instance/runtime method, never from a static field initializer). This class instead exposes
 * {@link #gun_pepperbox()}, a plain static <i>method</i>; {@link GunPistolItems} calls it from inside
 * the {@code Supplier} it hands {@code ModItems.ITEMS.register(...)}, which only runs at
 * {@code RegisterEvent} time for the {@code ITEM} registry - by then {@code SOUND_EVENT}'s own
 * {@code RegisterEvent} has already completed (it fires first in vanilla's {@code BuiltInRegistries}
 * order, the same guarantee every "register a BlockItem for a Block" call in any Forge/NeoForge mod
 * already relies on), so the sound holder resolves correctly. See this task's structured-output notes
 * for a flag recommending the same fix for the (already-committed, not-yet-wired) sibling files.
 * <p>
 * Not ported (see this task's explicit scope-outs, matching {@code XFactory556mm}'s documented gaps):
 * {@code .smoke(...)}/{@code .anim(...)}/{@code .orchestra(...)} (client-rendering, no default lambda
 * exists yet); {@code setDefaultAmmo(...)} ({@code ItemGunBaseNT} has no {@code defaultAmmo} field yet).
 */
public final class XFactoryBlackPowder {

    private XFactoryBlackPowder() {
    }

    // ==================== ammo (4) ====================

    public static final Item ITEM_STONE = new Item(new Item.Properties());
    public static final Item ITEM_STONE_AP = new Item(new Item.Properties());
    public static final Item ITEM_STONE_IRON = new Item(new Item.Properties());
    public static final Item ITEM_STONE_SHOT = new Item(new Item.Properties());

    public static final BulletConfig stone = new BulletConfig("stone").setItem(ITEM_STONE)
            .setBlackPowder(true).setHeadshot(1F).setSpread(0.025F).setRicochetAngle(15);
    public static final BulletConfig stone_ap = new BulletConfig("stone_ap").setItem(ITEM_STONE_AP)
            .setBlackPowder(true).setHeadshot(1F).setSpread(0.01F).setRicochetAngle(5)
            .setDoesPenetrate(true).setDamage(1.5F);
    public static final BulletConfig stone_iron = new BulletConfig("stone_iron").setItem(ITEM_STONE_IRON)
            .setBlackPowder(true).setHeadshot(1F).setSpread(0F).setRicochetAngle(90).setRicochetCount(5)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F);
    public static final BulletConfig stone_shot = new BulletConfig("stone_shot").setItem(ITEM_STONE_SHOT)
            .setBlackPowder(true).setHeadshot(1F).setSpread(0.1F).setRicochetAngle(45).setProjectiles(6, 6)
            .setDamage(1F / 6F);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_PEPPERBOX =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    // ==================== gun (1) ====================
    // A static METHOD, not an eager field - see class javadoc for why (defers HBMSoundHandler.get()
    // past mod-construction time). default ammo (not yet wired - ItemGunBaseNT has no defaultAmmo
    // field yet): STONE x12

    public static ItemGunBaseNT gun_pepperbox() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(5F).delay(27).reload(67).jam(58)
                                .sound(HBMSoundHandler.fireBlackPowder.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 6).addConfigs(stone, stone_ap, stone_iron, stone_shot))
                                .offset(0.75, -0.0625, -0.1875D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_PEPPERBOX))
                        .setupStandardConfiguration());
    }
}
