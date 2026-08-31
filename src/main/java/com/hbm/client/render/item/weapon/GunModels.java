package com.hbm.client.render.item.weapon;

import com.hbm.main.MainRegistry;
import com.hbm.render.anim.sedna.AnimationLoader;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.loader.HbmObjModel;
import com.hbm.render.loader.ModelFormatException;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OBJ model / animation-map / texture resource holder for the 3 guns this task ({@code c6-weapon-
 * gun-rendering}) fully wired ({@link ItemRenderSpas12}/{@link ItemRenderUzi}/{@link ItemRenderAm180}),
 * mirroring CE's own {@code com.hbm.main.ResourceManager} per-gun static-field pattern
 * ({@code public static final IModelCustom spas_12 = new HFRWavefrontObject(...).asVBO();
 * public static final HashMap<String, BusAnimationSedna> spas_12_anim = AnimationLoader.load(...);}) -
 * scoped to this task's own small package rather than a port-wide {@code ResourceManager}, since no
 * such class exists yet in this port (confirmed absent by search) and creating the real port-wide
 * equivalent is a separate concern belonging to whichever future package first needs one for a
 * non-gun model too (see this class's own "Not a port-wide ResourceManager" note below).
 *
 * <h2>Why every field here is lazy, not an eager {@code public static final}</h2>
 * {@link com.hbm.render.loader.HbmObjModel}'s own class javadoc states it is "safe to eagerly load
 * from a static field" - true only once the backing {@code .obj}/texture resources actually exist on
 * disk. As of this task, CE's weapon {@code .obj}/texture asset migration has <b>not</b> happened yet
 * (confirmed: {@code find src/main/resources -ipath '*models/obj/weapons*'} and
 * {@code '*textures/models/weapons*'} both return zero files in this port, vs. 0 and 123 respectively
 * in {@code upstream/hbm-ce}) - a separate, substantial asset-migration task
 * {@code docs/phase5/weapon_gun_rendering_animloader.md} explicitly flags as out of this report's
 * (and this task's) scope. An eager {@code public static final HbmObjModel SPAS_12 =
 * HbmObjModel.get(...)} field would throw {@link ModelFormatException} the moment this class is
 * classloaded - i.e. client startup would crash the instant any code so much as references this
 * class, long before a player ever equips the gun. Every accessor below is therefore lazy (mirroring
 * {@code ExamplePlaceholderBEWLR}'s own established lazy-load pattern in the sibling
 * {@code com.hbm.client.render.item} package) <b>and</b> defensive: a missing model/animation
 * resource is caught once, logged once ({@link #warnOnce}, a per-{@link ResourceLocation} guard so a
 * gun rendered 20x/second while missing its asset doesn't spam the log or crash the render loop), and
 * reported back as {@code null} - every concrete gun renderer in this package treats a {@code null}
 * model as "skip drawing this frame" rather than propagating the exception. This is a deliberate,
 * temporary safety net for this port's current mid-migration state, not a permanent design choice -
 * once the real {@code .obj}/texture/animation-JSON assets are ported (this task copies the 12
 * animation JSONs verbatim, see {@code docs/phase5/} - the {@code .obj}/{@code .png} files are not
 * copied by this task), every lookup below resolves normally and the try/catch/log-once machinery
 * simply never triggers.
 *
 * <h2>Resource paths - mirrored 1:1 from CE</h2>
 * Every path below is copied verbatim from {@code upstream/hbm-ce/.../main/main/ResourceManager.java}
 * (grepped and read directly, not guessed) so that once the asset-migration task lands the exact
 * same files, every lookup here resolves with zero further code changes.
 */
public final class GunModels {

    private GunModels() {
    }

    // ------------------------------------------------------------------------------------
    // SPAS-12 - CE: ResourceManager.spas_12 / spas_12_tex / spas_12_anim
    // ------------------------------------------------------------------------------------

    public static final ResourceLocation SPAS12_OBJ = rl("models/weapons/spas-12.obj");
    public static final ResourceLocation SPAS12_TEX = rl("textures/models/weapons/spas-12.png");
    public static final ResourceLocation SPAS12_ANIM = rl("models/weapons/animations/spas12.json");
    public static final ResourceLocation CASINGS_TEX = rl("textures/particle/casings.png");

    private static volatile HbmObjModel spas12Model;
    private static volatile Map<String, BusAnimationSedna> spas12Anim;

    @Nullable
    public static HbmObjModel spas12() {
        if (spas12Model == null) spas12Model = tryLoadModel(SPAS12_OBJ);
        return spas12Model;
    }

    public static Map<String, BusAnimationSedna> spas12Anim() {
        if (spas12Anim == null) spas12Anim = tryLoadAnim(SPAS12_ANIM);
        return spas12Anim;
    }

    // ------------------------------------------------------------------------------------
    // Uzi - CE: ResourceManager.uzi / uzi_tex / uzi_saturnite_tex (no dedicated animation JSON -
    // CE's own LAMBDA_UZI_ANIMS in XFactory9mm.java builds every BusAnimationSedna programmatically,
    // see GunAnimationRegistration.UZI_ANIM in this package, ported verbatim from that lambda).
    // ------------------------------------------------------------------------------------

    public static final ResourceLocation UZI_OBJ = rl("models/weapons/uzi.obj");
    public static final ResourceLocation UZI_TEX = rl("textures/models/weapons/uzi.png");
    public static final ResourceLocation UZI_SATURNITE_TEX = rl("textures/models/weapons/uzi_saturnite.png");

    private static volatile HbmObjModel uziModel;

    @Nullable
    public static HbmObjModel uzi() {
        if (uziModel == null) uziModel = tryLoadModel(UZI_OBJ);
        return uziModel;
    }

    // ------------------------------------------------------------------------------------
    // AM-180 - CE: ResourceManager.am180 / am180_tex / am180_anim
    // ------------------------------------------------------------------------------------

    public static final ResourceLocation AM180_OBJ = rl("models/weapons/am180.obj");
    public static final ResourceLocation AM180_TEX = rl("textures/models/weapons/am180.png");
    public static final ResourceLocation AM180_ANIM = rl("models/weapons/animations/am180.json");

    private static volatile HbmObjModel am180Model;
    private static volatile Map<String, BusAnimationSedna> am180Anim;

    @Nullable
    public static HbmObjModel am180() {
        if (am180Model == null) am180Model = tryLoadModel(AM180_OBJ);
        return am180Model;
    }

    public static Map<String, BusAnimationSedna> am180Anim() {
        if (am180Anim == null) am180Anim = tryLoadAnim(AM180_ANIM);
        return am180Anim;
    }

    // ------------------------------------------------------------------------------------
    // Shared lazy-load / defensive-fallback machinery - see class javadoc.
    // ------------------------------------------------------------------------------------

    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();

    @Nullable
    private static HbmObjModel tryLoadModel(ResourceLocation resource) {
        try {
            return HbmObjModel.get(resource);
        } catch (ModelFormatException e) {
            warnOnce(resource, "OBJ model", e);
            return null;
        }
    }

    /** Never {@code null} - falls back to an empty, immutable map (every bus lookup then naturally returns the identity/no-op transform, matching CE's own {@code null}-tolerant call sites). */
    private static Map<String, BusAnimationSedna> tryLoadAnim(ResourceLocation resource) {
        try {
            Map<String, BusAnimationSedna> loaded = AnimationLoader.load(resource);
            if (loaded != null) return loaded;
            warnOnce(resource, "animation JSON (resource not found)", null);
        } catch (Exception e) {
            warnOnce(resource, "animation JSON", e);
        }
        return Map.of();
    }

    private static void warnOnce(ResourceLocation resource, String kind, @Nullable Exception e) {
        if (WARNED.add(resource)) {
            MainRegistry.logger.warn(
                    "GunModels: failed to load {} '{}' - this gun will render without it until the " +
                            "weapon asset-migration task ports the missing file. (Logged once per resource.)",
                    kind, resource, e);
        }
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
    }
}
