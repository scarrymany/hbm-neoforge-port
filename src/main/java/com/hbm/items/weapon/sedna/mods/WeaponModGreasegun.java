package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Port of CE's {@code WeaponModGreasegun} (50 lines) - cleans/lightens the greasegun: tougher,
 * harder-hitting, tighter spread, faster cycling.
 * <p>
 * <b>Stubbed branch, documented rather than silent.</b> CE's {@code CON_ORCHESTRA} lambda
 * ({@code ORCHESTRA_GREASEGUN}) spawns a {@code SpentCasing} particle effect via
 * {@code CasingCreator.composeEffect} on its own {@code CYCLE} animation branch, falling back to
 * {@code Orchestras.ORCHESTRA_GREASEGUN} otherwise - neither {@code SpentCasing}/{@code CasingCreator}
 * (pure client particle rendering, see {@code BulletConfig}'s own javadoc for why they're deferred)
 * nor {@code Orchestras} (1,694-line Phase 5 sound-cue timing table) exist in this port. The 4 real,
 * numeric gameplay effects (durability/damage/spread/fire-delay) are still wired below.
 */
public class WeaponModGreasegun extends WeaponModBase {

    public WeaponModGreasegun(String id) {
        super(id, "FURNITURE");
        this.setPriority(PRIORITY_ADDITIVE);
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, GunConfig.F_DURABILITY)) return cast((Float) base * 3F, base);
        if (Objects.equals(key, Receiver.F_BASEDAMAGE)) return cast((Float) base + 2F, base);
        if (Objects.equals(key, Receiver.F_SPREADINNATE)) return cast(0F, base);
        if (Objects.equals(key, Receiver.I_DELAYAFTERFIRE)) return cast((Integer) base / 2, base);
        return base;
    }

    // TODO(render-phase5): CE's CON_ORCHESTRA -> ORCHESTRA_GREASEGUN branch is not wired - see class
    // javadoc for the exact missing SpentCasing/CasingCreator/Orchestras dependencies.
    public static final BiConsumer<ItemStack, LambdaContext> ORCHESTRA_GREASEGUN = (stack, ctx) -> {
        // Intentionally empty - see class javadoc.
    };
}
