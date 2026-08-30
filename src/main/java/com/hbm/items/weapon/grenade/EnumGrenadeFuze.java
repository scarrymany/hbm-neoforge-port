package com.hbm.items.weapon.grenade;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze} (5 values).
 * Per-fuze behavior is kept as private static methods referenced from this same class (rather than
 * CE's own shape of {@code public static final} lambda fields declared on the *outer* item class) -
 * deliberately, to avoid a same-class static-field-initialization-order hazard: an enum constant's
 * argument list runs before any additional {@code static final} field declared later in the same
 * class body would be initialized, so a lambda *field* reference from a constant to a later field in
 * this class would silently read {@code null}. A method reference to a private static method has no
 * such hazard (methods don't need their declaring class to finish initializing before the method
 * *reference* is formed - only an actual invocation does, and that only happens later, when the
 * fuze/extra/filling actually fires). See {@link EnumGrenadeExtra}/{@link EnumGrenadeFilling} for the
 * same pattern applied to the other two component enums.
 */
public enum EnumGrenadeFuze implements StringRepresentable {

    /** 3s timed. */
    S3(EnumGrenadeFuze::updateS3, null, 0x000000),
    /** 7s timed. */
    S7(EnumGrenadeFuze::updateS7, null, 0x404040),
    /** 15s timed. */
    S15(EnumGrenadeFuze::updateS15, null, 0x808080),
    /** On block/entity impact, 0.5s safety. */
    IMPACT(null, EnumGrenadeFuze::onImpactImpact, 0xE36C17),
    /** 1.5s safety, explodes once a floor is within 10 blocks straight down. */
    AIRBURST(EnumGrenadeFuze::updateAirburst, null, 0x56A137);

    public static final EnumGrenadeFuze[] VALUES = values();

    public static final Codec<EnumGrenadeFuze> CODEC = StringRepresentable.fromEnum(EnumGrenadeFuze::values);

    public final Consumer<EntityGrenadeUniversal> updateTick;
    public final BiConsumer<EntityGrenadeUniversal, HitResult> onImpact;
    public final int bandColor;

    EnumGrenadeFuze(Consumer<EntityGrenadeUniversal> updateTick, BiConsumer<EntityGrenadeUniversal, HitResult> onImpact, int bandColor) {
        this.updateTick = updateTick;
        this.onImpact = onImpact;
        this.bandColor = bandColor;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static void updateS3(EntityGrenadeUniversal grenade) {
        if (grenade.getTimer() >= 60) grenade.explode();
    }

    private static void updateS7(EntityGrenadeUniversal grenade) {
        if (grenade.getTimer() >= 140) grenade.explode();
    }

    private static void updateS15(EntityGrenadeUniversal grenade) {
        if (grenade.getTimer() >= 300) grenade.explode();
    }

    private static void onImpactImpact(EntityGrenadeUniversal grenade, HitResult mop) {
        if (grenade.getTimer() >= 10) {
            Vec3 hit = mop.getLocation();
            grenade.setPos(hit.x, hit.y, hit.z);
            grenade.explode();
        }
    }

    private static void updateAirburst(EntityGrenadeUniversal grenade) {
        if (grenade.getTimer() >= 30) {
            Vec3 start = grenade.position();
            Vec3 end = start.subtract(0, 10, 0);
            BlockHitResult mop = grenade.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, grenade));
            if (mop.getType() == HitResult.Type.BLOCK) grenade.explode();
        }
    }
}
