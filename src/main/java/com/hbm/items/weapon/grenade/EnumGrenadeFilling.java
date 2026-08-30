package com.hbm.items.weapon.grenade;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import static com.hbm.items.weapon.grenade.EnumGrenadeShell.FRAG;
import static com.hbm.items.weapon.grenade.EnumGrenadeShell.NUKE;
import static com.hbm.items.weapon.grenade.EnumGrenadeShell.STICK;
import static com.hbm.items.weapon.grenade.EnumGrenadeShell.TECH;

/**
 * Port of CE's {@code com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling} (13
 * values) - the actual detonation logic table. Behavior itself lives in
 * {@link GrenadeFillingActions} (see that class's javadoc for the full forward-reference list and
 * {@link EnumGrenadeFuze}'s javadoc for why constants reference method handles rather than shared
 * lambda fields declared in this same file).
 */
public enum EnumGrenadeFilling implements StringRepresentable {

    /** Gunpowder - weakest HE tier. */
    POWDER(GrenadeFillingActions::explodePowder, 0x424242, 0x939176, FRAG, STICK),
    /** High explosive - the "default" grenade. */
    HE(GrenadeFillingActions::explodeHe, 0x595533, 0xA49D62, FRAG, STICK),
    /** Demolition. */
    DEMO(GrenadeFillingActions::explodeDemo, 0x595533, 0xDD4029, FRAG, STICK),
    /** Incendiary. */
    INC(GrenadeFillingActions::explodeInc, 0x5A5A5A, 0xFF5F21, FRAG, STICK),
    /** White phosphorus. */
    WP(GrenadeFillingActions::explodeWp, 0xDCDCDC, 0xFF5F21, FRAG, STICK),
    /** Explosive pellets. */
    CLUSTER(GrenadeFillingActions::explodeCluster, 0x5A5A5A, 0xFFC711, FRAG, STICK),
    /** Tesla - EMP. */
    EMP(GrenadeFillingActions::explodeEmp, 0x93A1AC, 0x00FFFF, TECH),
    /** EMP but more oomph. */
    PLASMA(GrenadeFillingActions::explodePlasma, 0x655B2C, 0x4CFF00, TECH),
    /** Pew pew pew. */
    LASER(GrenadeFillingActions::explodeLaser, 0x493A3A, 0xFF0000, TECH),
    /** Cluster but fat. */
    CLUSTER_HEAVY(GrenadeFillingActions::explodeClusterHeavy, 0x5A5A5A, 0xFF5F21, NUKE),
    /** Nuka grenade. */
    NUCLEAR(GrenadeFillingActions::explodeNuclear, 0xDFD7A8, 0xA49D62, NUKE),
    /** Demolition nuka grenade. */
    NUCLEAR_DEMO(GrenadeFillingActions::explodeNuclearDemo, 0xDFD7A8, 0xDD4029, NUKE),
    /** What used to be aschrab. */
    SCHRAB(GrenadeFillingActions::explodeSchrab, 0x00BDBD, 0x000000, NUKE);

    public static final EnumGrenadeFilling[] VALUES = values();

    public static final Codec<EnumGrenadeFilling> CODEC = StringRepresentable.fromEnum(EnumGrenadeFilling::values);

    public final Consumer<EntityGrenadeUniversal> explode;
    public final Set<EnumGrenadeShell> compatibleShells;
    public final int bodyColor;
    public final int labelColor;

    EnumGrenadeFilling(Consumer<EntityGrenadeUniversal> explode, int bodyColor, int labelColor, EnumGrenadeShell... compatibleShells) {
        this.explode = explode;
        Set<EnumGrenadeShell> shells = EnumSet.noneOf(EnumGrenadeShell.class);
        Collections.addAll(shells, compatibleShells);
        this.compatibleShells = Collections.unmodifiableSet(shells);
        this.bodyColor = bodyColor;
        this.labelColor = labelColor;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
