package com.hbm.render.loader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern (MC 1.21.1 / NeoForge) replacement for CE's Wavefront OBJ pipeline -
 * {@code com.hbm.render.loader.HFRWavefrontObject} + {@code GroupObject}/{@code Face}/
 * {@code Vertex}/{@code TextureCoordinate} + {@code WaveFrontObjectVAO}
 * (upstream/hbm-ce/src/main/java/com/hbm/render/loader/HFRWavefrontObject.java and its sibling
 * files in the same package - read in full while designing this class).
 *
 * <h2>Why this is a hand-parsed runtime mesh, not a baked-model {@code IGeometryLoader}</h2>
 *
 * CE's OBJ pipeline is deliberately <b>not</b> item/block "static model" shaped. Every real call
 * site (grepped across upstream/hbm-ce/src/main/java/com/hbm/render: ~209 files call
 * {@code renderPart}/{@code renderOnly}/{@code renderAllExcept}) is a
 * {@code TileEntitySpecialRenderer}/{@code ItemRenderBase} that:
 * <ol>
 *   <li>binds a texture itself ({@code bindTexture(...)}, chosen at render time, not baked into
 *       the model - CE ships zero {@code .mtl} files, confirmed by grep; every OBJ is one mesh
 *       painted with whatever texture the call site binds);</li>
 *   <li>pushes/pops its own GL matrix stack <i>between</i> individual {@code renderPart} calls to
 *       reposition one named part relative to another (see
 *       {@code render/tileentity/RenderRBMKAutoloader.java}: translates by the live piston-lerp
 *       offset between rendering the "Base" and "Piston" groups of the same model,
 *       every frame); and</li>
 *   <li>tints individual parts with {@code GlStateManager.color(...)} around just that one
 *       {@code renderPart} call (see {@code render/tileentity/RenderFloodlight.java}: the "Lamps"
 *       group is drawn once at 25% grey when off, full white + fullbright when on).</li>
 * </ol>
 * None of that is expressible through vanilla's {@code IUnbakedGeometry}/{@code IGeometryLoader}
 * pipeline, which bakes one static {@code BakedModel} keyed by {@code ItemDisplayContext} /
 * blockstate - it has no concept of "render only this named sub-part, with this transform, with
 * this tint, decided fresh every frame from live block-entity/gun/vehicle state" (per-part
 * visibility toggling and mid-model matrix pushes are exactly the shape a
 * {@code BlockEntityRenderer}/{@code EntityRenderer}/{@code ItemStack} "extra" renderer wants, and
 * exactly the shape the baked-model contract does not support). Every large NeoForge mod with
 * multi-part OBJ/TESR-style meshes (confirmed by reading how upstream/neo-edition's own
 * {@code HFRWavefrontObject}+{@code ObjRenderer} do it, cross-checked for API shape only per this
 * port's ground rules) reaches the same conclusion and hand-parses the OBJ into a runtime mesh
 * instead. This class does the same, but deliberately does <b>not</b> copy Neo Edition's own
 * design of uploading each group to a persistent {@code VertexBuffer} drawn through a bespoke
 * shader/material system ({@code com.hbm.render.material.*}, {@code NtmShaders}) - that is a
 * legitimate choice but it is Neo Edition's own added infrastructure, not present anywhere in
 * this port, not required by CE's actual behavior, and out of scope for this task. Instead each
 * named part is re-submitted every frame straight into the caller-supplied
 * {@link VertexConsumer}, exactly like vanilla's own {@code ModelPart} and every hand-rolled
 * "extra geometry" helper in this port's own {@code com.hbm.render.util} package do - it is the
 * standard, idiomatic way to draw an arbitrary triangle mesh from a
 * {@code BlockEntityRenderer}/{@code EntityRenderer}/item renderer in modern Minecraft, integrates
 * for free with vanilla's translucency sorting and render-type batching (via whatever
 * {@link RenderType} the call site's {@link net.minecraft.client.renderer.MultiBufferSource}
 * buffer was obtained from), and needs no additional GL-buffer lifecycle management of its own.
 *
 * <h2>Public contract</h2>
 * A model is loaded once (from an {@code .obj} resource, cached by {@link #get(ResourceLocation)}
 * so every future renderer referencing the same file shares one parsed instance - matching CE's
 * own pattern of one {@code public static final} model field per renderer class, just without the
 * silent parse-it-twice trap if two renderer classes point at the same file) and then any call
 * site can render one or more named groups on demand via {@link #renderPart}/{@link #renderOnly}/
 * {@link #renderAllExcept}/{@link #renderAll}, each taking the caller's own
 * {@link PoseStack}/{@link VertexConsumer}/packed light/packed overlay - i.e. exactly CE's
 * {@code renderPart(String)} etc., translated onto the modern immediate-mode-successor API. Group
 * name lookups are case-insensitive, matching CE's {@code equalsIgnoreCase} throughout
 * {@code HFRWavefrontObject}/{@code GroupObject}.
 *
 * <h2>Parsing behavior ported from CE, and deliberate deviations</h2>
 * <ul>
 *   <li>Same directives recognized: {@code # comment}, {@code v x y z}, {@code vn x y z},
 *       {@code vt u v [w]} (the OBJ {@code v}-flip {@code 1 - v} CE and Neo Edition both apply is
 *       preserved), {@code f ...} in all four vertex-ref shapes ({@code v}, {@code v/vt},
 *       {@code v//vn}, {@code v/vt/vn}), and {@code g}/{@code o} starting a new named group.</li>
 *   <li><b>Arbitrary n-gon faces are fan-triangulated exactly like CE's indexed-VBO path</b>
 *       ({@code WaveFrontObjectVAO.buildGroupBuffers}: {@code for i=2..n-1: emit (0, i-1, i)}) -
 *       but unlike CE's old fixed-pipeline {@code HFRWavefrontObject.renderAll}, which required
 *       every face in a group to agree on tri-vs-quad because a single {@code glBegin(mode)} call
 *       drew the whole group, this class has no such restriction: triangles and quads (and larger
 *       n-gons) may freely mix within one group, because every face is independently
 *       fan-triangulated into the same flat {@code GL_TRIANGLES}-equivalent vertex stream before
 *       rendering. This is a strict relaxation of a legacy immediate-mode limitation, not a
 *       behavior change for any well-formed CE model.</li>
 *   <li>Face normal fallback matches CE's {@code Face.calculateFaceNormal()} exactly: when a face
 *       omits explicit per-vertex normals ({@code f v..} or {@code f v/vt..}), one flat normal is
 *       computed from the cross product of the <i>original polygon's</i> first three corners
 *       (before fan-triangulation) and applied to every corner of every sub-triangle from that
 *       face - not recomputed per sub-triangle.</li>
 *   <li>CE's rare "baked vertex color" {@code v} extension ({@code v x y z r g b [a]}, 6 or 7
 *       tokens) is preserved and multiplied against the caller's runtime tint at render time (see
 *       {@code WaveFrontObjectVAO}'s "colored" mode) - though as of this port's asset date no
 *       shipped CE {@code .obj} uses it (grep of upstream/hbm-ce found none), so it is untested
 *       against a real file.</li>
 *   <li><b>Dropped as dead code:</b> CE's {@code Face.addFaceForRender(BufferBuilder, float
 *       textureOffset)} anti-seam-bleed UV-inset trick. Every real call site in CE
 *       ({@code GroupObject.render}) invokes the zero-argument overload, i.e. {@code textureOffset}
 *       is always {@code 0F} in practice - the inset math never executes. Not ported.</li>
 *   <li><b>Dropped as dead code:</b> CE's per-group {@code glDrawingMode} tri/quad-uniformity
 *       validation (superseded by the n-gon relaxation above) and the {@code .mtl}/material
 *       loading CE never actually has (zero {@code .mtl} references anywhere in
 *       upstream/hbm-ce - texture binding is entirely the render call site's job, exactly as
 *       {@link #renderType(ResourceLocation)} below assumes).</li>
 *   <li>CE tolerates (and its own final line unconditionally executes)
 *       {@code groupObjects.add(currentGroupObject)} even when {@code currentGroupObject} is
 *       still {@code null} (a file with zero {@code f}/{@code g}/{@code o} lines) or empty
 *       (a trailing {@code g} line with no faces after it). This port skips adding a null or
 *       zero-face group instead of storing a bogus empty part - {@link #getPartNames()} therefore
 *       never returns a phantom entry, which cannot affect any real (non-empty) named part.</li>
 * </ul>
 *
 * <h2>Resource reload</h2>
 * Every model obtained through {@link #get(ResourceLocation)} is tracked and automatically
 * re-parsed on a client resource reload (F3+T, resource pack switch, {@code /reload}) by
 * {@link HbmObjModelReloader}, which self-registers via its own {@code @EventBusSubscriber}
 * hook on {@code RegisterClientReloadListenersEvent} - see that class for why this needed no edit
 * to the shared {@code ClientModRegistry}. Reparsing mutates the existing instance in place (same
 * object identity), so a {@code public static final HbmObjModel FOO = HbmObjModel.get(...)} field
 * in a future renderer class picks up the new geometry automatically, matching CE/Neo Edition's
 * own reload-in-place behavior. Models obtained via {@link #parse(String, InputStream)} (a raw
 * stream, no backing {@link ResourceLocation}) are one-shot and are not tracked for reload.
 *
 * <h2>Safe to eagerly load from a static field (unlike registry holders)</h2>
 * This port's ground rules flag a recurring bug: a {@code DeferredHolder}/registry-object
 * {@code .get()} call inside a static field initializer crashes if the containing class loads
 * before the matching {@code RegisterEvent} has fired. {@link #get(ResourceLocation)} does
 * <b>not</b> have that hazard - it touches only {@code Minecraft.getInstance().getResourceManager()}
 * (populated during early client bootstrap resource loading, long before any mod registry
 * event fires) and never a NeoForge registry. A future renderer class is safe to write
 * {@code public static final HbmObjModel FOO = HbmObjModel.get(rl);} as a plain eager static
 * field exactly like CE's own {@code public static final IModelCustom floodlight = new
 * HFRWavefrontObject(...)} pattern - by the time any such class is even classloaded (block
 * entity/entity renderer registration happens in {@code EntityRenderersEvent.RegisterRenderers},
 * itself a client-setup-time mod-bus event that fires well after resources are ready), the
 * resource manager is guaranteed present.
 *
 * <h2>Texture / light note for future call sites</h2>
 * CE's fixed-pipeline "fullbright" tricks (e.g. {@code RenderFloodlight}'s lit "Lamps" group,
 * toggled via a GL lightmap override around just that one {@code renderPart} call) have no GL
 * state-toggle equivalent here - pass {@code net.minecraft.client.renderer.LightTexture.FULL_BRIGHT}
 * (packs to {@code 0xF000F0}) as this method's {@code packedLight} argument for just that one call
 * instead. Left as a note for whichever future package ports the concrete TESR classes that need
 * it (out of this task's scope, which is the loader/renderer utility itself).
 */
public final class HbmObjModel {

    /** Per-vertex float stride in {@link Group#data}: pos.x pos.y pos.z, normal.x normal.y normal.z, u, v. */
    private static final int STRIDE = 8;

    private static final Map<ResourceLocation, HbmObjModel> CACHE = new ConcurrentHashMap<>();

    /** Non-null only for instances obtained through {@link #load}/{@link #get} (resource-backed, reloadable). */
    private final ResourceLocation resource;
    private final String debugName;

    // Mutated in place by #applyParsed - see the class javadoc's "Resource reload" section.
    private volatile Map<String, Group> groupsByName;
    private volatile List<String> partNamesView;

    private HbmObjModel(ResourceLocation resource, String debugName) {
        this.resource = resource;
        this.debugName = debugName;
    }

    // ------------------------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------------------------

    /**
     * Returns the cached model for {@code resource}, parsing and caching it on first use.
     * Every future renderer that wants to draw parts of the same {@code .obj} file should go
     * through this method (not {@link #load}) so the file is only ever parsed once, and so it
     * participates in {@link HbmObjModelReloader}'s automatic reload-on-resource-reload tracking.
     */
    public static HbmObjModel get(ResourceLocation resource) {
        return CACHE.computeIfAbsent(resource, HbmObjModel::load);
    }

    /**
     * Loads and parses {@code resource} fresh (bypassing the cache). Prefer {@link #get} for
     * normal use; this exists for callers that deliberately want an independent, non-shared
     * instance.
     *
     * @throws ModelFormatException on any IO or parse failure, mirroring CE's constructor
     *         contract ({@code HFRWavefrontObject(ResourceLocation)} also throws unchecked on
     *         failure - there is no recoverable "partial model" state in CE either).
     */
    public static HbmObjModel load(ResourceLocation resource) {
        try {
            Resource res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(resource);
            try (InputStream in = res.open()) {
                HbmObjModel model = new HbmObjModel(resource, resource.toString());
                model.applyParsed(parseObjModel(model.debugName, in));
                return model;
            }
        } catch (IOException e) {
            throw new ModelFormatException("Failed to load OBJ model: " + resource, e);
        }
    }

    /**
     * Parses an already-open stream directly, for the rare case a caller has the {@code .obj}
     * bytes some other way than the resource manager (mirrors CE's second constructor,
     * {@code HFRWavefrontObject(String filename, InputStream inputStream)}). The returned
     * instance has no backing {@link ResourceLocation} and is <b>not</b> tracked for automatic
     * resource-pack-reload re-parsing. The stream is always closed before this method returns.
     */
    public static HbmObjModel parse(String debugName, InputStream in) {
        HbmObjModel model = new HbmObjModel(null, debugName);
        try {
            model.applyParsed(parseObjModel(debugName, in));
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // hush, matching CE's own best-effort close in HFRWavefrontObject.loadObjModel's finally block
            }
        }
        return model;
    }

    /** @return the {@code .obj} resource this model was loaded from, or {@code null} for a {@link #parse}-loaded instance. */
    public ResourceLocation getResource() {
        return resource;
    }

    /** Package-private: re-parses this instance's backing resource in place. Called only by {@link HbmObjModelReloader}. */
    void reload(ResourceManager resourceManager) {
        if (resource == null) return;
        try {
            Resource res = resourceManager.getResourceOrThrow(resource);
            try (InputStream in = res.open()) {
                applyParsed(parseObjModel(debugName, in));
            }
        } catch (IOException e) {
            // Matches CE/Neo Edition's own reload-listener failure handling (HFRModelReloader.apply's
            // empty `catch(IOException e) {}`): keep whatever geometry was already loaded rather than
            // crash the shared resource-reload barrier over one bad/missing model.
        }
    }

    static Collection<HbmObjModel> allTracked() {
        return CACHE.values();
    }

    private void applyParsed(List<Group> groups) {
        Map<String, Group> byName = new LinkedHashMap<>();
        List<String> names = new ArrayList<>(groups.size());
        for (Group g : groups) {
            byName.put(g.name.toLowerCase(Locale.ROOT), g);
            names.add(g.name);
        }
        this.groupsByName = byName;
        this.partNamesView = Collections.unmodifiableList(names);
    }

    // ------------------------------------------------------------------------------------
    // Introspection
    // ------------------------------------------------------------------------------------

    /** Names of every named group ({@code g}/{@code o} directive) this model parsed, in file order. */
    public List<String> getPartNames() {
        return partNamesView;
    }

    /** Case-insensitive, matching CE's {@code GroupObject}/{@code HFRWavefrontObject} lookups throughout. */
    public boolean hasPart(String name) {
        return groupsByName.containsKey(name.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------------------------
    // Rendering - see class javadoc for why these take an explicit PoseStack/VertexConsumer/
    // packed light+overlay instead of going through the baked-model pipeline.
    // ------------------------------------------------------------------------------------

    /** Renders one named group at full white tint. No-op if no group matches (case-insensitive), same as CE's {@code renderPart}. */
    public void renderPart(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, String partName) {
        renderPart(poseStack, consumer, packedLight, packedOverlay, 1F, 1F, 1F, 1F, partName);
    }

    /** Renders one named group, tinted by a packed {@code 0xAARRGGBB} color multiplied against each vertex's own baked color (usually opaque white). */
    public void renderPart(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb, String partName) {
        renderPart(poseStack, consumer, packedLight, packedOverlay, unpackR(argb), unpackG(argb), unpackB(argb), unpackA(argb), partName);
    }

    /** Renders one named group, tinted by explicit {@code 0..1} RGBA - the modern equivalent of CE wrapping a single {@code renderPart} call in {@code GlStateManager.color(r,g,b,a)}. */
    public void renderPart(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a, String partName) {
        Group group = groupsByName.get(partName.toLowerCase(Locale.ROOT));
        if (group != null) {
            renderGroup(group, poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
        }
    }

    /** Renders every named group whose name is in {@code partNames} (case-insensitive), each at full white tint. Mirrors CE's {@code renderOnly(String...)}. */
    public void renderOnly(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, String... partNames) {
        renderOnly(poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF, partNames);
    }

    /** Renders every named group whose name is in {@code partNames} (case-insensitive), all sharing one tint. */
    public void renderOnly(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb, String... partNames) {
        for (String name : partNames) {
            renderPart(poseStack, consumer, packedLight, packedOverlay, argb, name);
        }
    }

    /** Renders every named group except those in {@code excludedPartNames} (case-insensitive), at full white tint. Mirrors CE's {@code renderAllExcept(String...)}. */
    public void renderAllExcept(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, String... excludedPartNames) {
        renderAllExcept(poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF, excludedPartNames);
    }

    /** Renders every named group except those in {@code excludedPartNames} (case-insensitive), all sharing one tint. */
    public void renderAllExcept(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb, String... excludedPartNames) {
        float r = unpackR(argb), g = unpackG(argb), b = unpackB(argb), a = unpackA(argb);
        outer:
        for (Group group : groupsByName.values()) {
            for (String excluded : excludedPartNames) {
                if (group.name.equalsIgnoreCase(excluded)) continue outer;
            }
            renderGroup(group, poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
        }
    }

    /** Renders the whole model (every named group) at full white tint. Mirrors CE's {@code renderAll()}. */
    public void renderAll(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        renderAll(poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF);
    }

    /** Renders the whole model (every named group), all sharing one tint. */
    public void renderAll(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb) {
        float r = unpackR(argb), g = unpackG(argb), b = unpackB(argb), a = unpackA(argb);
        for (Group group : groupsByName.values()) {
            renderGroup(group, poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
        }
    }

    private static float unpackA(int argb) { return ((argb >>> 24) & 0xFF) / 255F; }
    private static float unpackR(int argb) { return ((argb >>> 16) & 0xFF) / 255F; }
    private static float unpackG(int argb) { return ((argb >>> 8) & 0xFF) / 255F; }
    private static float unpackB(int argb) { return (argb & 0xFF) / 255F; }

    private void renderGroup(Group group, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        int count = group.vertexCount;
        if (count == 0) return;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        // Normal matrix transform done here explicitly (rather than trusting an assumed
        // VertexConsumer#setNormal(PoseStack.Pose, float, float, float) convenience overload)
        // because this sandbox's outbound network access to the NeoForge/Mojang javadoc hosts
        // needed to confirm that overload's exact signature was blocked (see this task's
        // handoff notes) - PoseStack.Pose#pose()/#normal() and JOML's Matrix3f#transform(Vector3f)
        // are long-stable APIs confirmed directly against upstream/neo-edition source targeting
        // this exact neo_version, so self-transforming is the verifiably-correct fallback.
        Matrix3f normalMatrix = pose.normal();

        float[] data = group.data;
        int[] colors = group.colors;
        // One reused scratch vector for the whole group instead of allocating per-vertex -
        // this runs every frame for every visible part, potentially many times a frame once
        // RBMK column/gun/vehicle renderers land on top of this class.
        Vector3f normalScratch = new Vector3f();

        for (int i = 0, o = 0; i < count; i++, o += STRIDE) {
            float cr = r, cg = g, cb = b, ca = a;
            if (colors != null) {
                int baked = colors[i];
                ca *= ((baked >>> 24) & 0xFF) / 255F;
                cr *= ((baked >>> 16) & 0xFF) / 255F;
                cg *= ((baked >>> 8) & 0xFF) / 255F;
                cb *= (baked & 0xFF) / 255F;
            }

            normalScratch.set(data[o + 3], data[o + 4], data[o + 5]);
            normalMatrix.transform(normalScratch);

            consumer.addVertex(positionMatrix, data[o], data[o + 1], data[o + 2])
                    .setColor(cr, cg, cb, ca)
                    .setUv(data[o + 6], data[o + 7])
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(normalScratch.x(), normalScratch.y(), normalScratch.z());
        }
    }

    /**
     * Convenience {@link RenderType} for OBJ part rendering: cutout-alpha, both faces drawn
     * (CE's TESRs routinely {@code GlStateManager.disableCull()} before drawing OBJ parts - e.g.
     * {@code RenderFloodlight}'s {@code renderInventory}), lightmap + overlay enabled. Callers
     * with different blending needs (a translucent glass part, an additive glow part, etc.) should
     * pick their own {@link RenderType} instead - this is a default for the common case, not a
     * requirement; every render method above takes a plain {@link VertexConsumer} regardless of
     * how it was obtained.
     */
    public static RenderType renderType(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    // ------------------------------------------------------------------------------------
    // Parsing - see the class javadoc's "Parsing behavior" section for what is/isn't ported
    // from CE's HFRWavefrontObject verbatim.
    // ------------------------------------------------------------------------------------

    private static List<Group> parseObjModel(String debugName, InputStream inputStream) {
        List<RawVertex> vertices = new ArrayList<>();
        List<RawVertex> normals = new ArrayList<>();
        List<RawUv> uvs = new ArrayList<>();
        List<PendingGroup> pendingGroups = new ArrayList<>();
        PendingGroup current = null;

        int lineNo = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.replaceAll("\\s+", " ").trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                } else if (line.startsWith("v ")) {
                    vertices.add(parseVertex(line, lineNo, debugName));
                } else if (line.startsWith("vn ")) {
                    normals.add(parseNormal(line, lineNo, debugName));
                } else if (line.startsWith("vt ")) {
                    uvs.add(parseUv(line, lineNo, debugName));
                } else if (line.startsWith("f ")) {
                    if (current == null) {
                        current = new PendingGroup("Default");
                    }
                    current.faces.add(parseFace(line, lineNo, debugName, vertices, normals, uvs));
                } else if (line.startsWith("g ") || line.startsWith("o ")) {
                    if (current != null) {
                        pendingGroups.add(current);
                    }
                    String name = line.substring(2).trim();
                    current = name.isEmpty() ? null : new PendingGroup(name);
                }
            }
        } catch (IOException e) {
            throw new ModelFormatException("IO exception reading OBJ model '" + debugName + "'", e);
        }
        if (current != null) {
            pendingGroups.add(current);
        }

        List<Group> baked = new ArrayList<>(pendingGroups.size());
        for (PendingGroup pending : pendingGroups) {
            if (!pending.faces.isEmpty()) {
                baked.add(bakeGroup(pending));
            }
        }
        return baked;
    }

    private static RawVertex parseVertex(String line, int lineNo, String debugName) {
        String[] tok = line.substring(2).trim().split(" ");
        try {
            if (tok.length == 2) {
                return new RawVertex(Float.parseFloat(tok[0]), Float.parseFloat(tok[1]), 0F);
            } else if (tok.length == 3) {
                return new RawVertex(Float.parseFloat(tok[0]), Float.parseFloat(tok[1]), Float.parseFloat(tok[2]));
            } else if (tok.length == 6) {
                RawVertex v = new RawVertex(Float.parseFloat(tok[0]), Float.parseFloat(tok[1]), Float.parseFloat(tok[2]));
                v.color = packColor(Float.parseFloat(tok[3]), Float.parseFloat(tok[4]), Float.parseFloat(tok[5]), 1F);
                return v;
            } else if (tok.length == 7) {
                RawVertex v = new RawVertex(Float.parseFloat(tok[0]), Float.parseFloat(tok[1]), Float.parseFloat(tok[2]));
                v.color = packColor(Float.parseFloat(tok[3]), Float.parseFloat(tok[4]), Float.parseFloat(tok[5]), Float.parseFloat(tok[6]));
                return v;
            }
        } catch (NumberFormatException e) {
            throw new ModelFormatException(formatError(debugName, lineNo, line, "bad number in vertex"), e);
        }
        throw new ModelFormatException(formatError(debugName, lineNo, line, "unsupported vertex token count " + tok.length));
    }

    private static RawVertex parseNormal(String line, int lineNo, String debugName) {
        String[] tok = line.substring(3).trim().split(" ");
        if (tok.length != 3) {
            throw new ModelFormatException(formatError(debugName, lineNo, line, "vertex normal needs exactly 3 components"));
        }
        try {
            return new RawVertex(Float.parseFloat(tok[0]), Float.parseFloat(tok[1]), Float.parseFloat(tok[2]));
        } catch (NumberFormatException e) {
            throw new ModelFormatException(formatError(debugName, lineNo, line, "bad number in vertex normal"), e);
        }
    }

    private static RawUv parseUv(String line, int lineNo, String debugName) {
        // A third ('w') token is legal OBJ grammar for 3D textures but is never consumed by any
        // CE render path (Face.addFaceForRender only ever reads .u/.v) - accepted and ignored.
        String[] tok = line.substring(3).trim().split(" ");
        if (tok.length < 2) {
            throw new ModelFormatException(formatError(debugName, lineNo, line, "texture coordinate needs at least 2 components"));
        }
        try {
            // OBJ stores V from the bottom; flip to Minecraft's top-down UV convention, matching
            // both CE's HFRWavefrontObject and upstream/neo-edition's port verbatim (`1 - v`).
            return new RawUv(Float.parseFloat(tok[0]), 1F - Float.parseFloat(tok[1]));
        } catch (NumberFormatException e) {
            throw new ModelFormatException(formatError(debugName, lineNo, line, "bad number in texture coordinate"), e);
        }
    }

    private static PendingFace parseFace(String line, int lineNo, String debugName, List<RawVertex> vertices, List<RawVertex> normals, List<RawUv> uvs) {
        String[] tok = line.substring(2).trim().split(" ");
        if (tok.length < 3) {
            throw new ModelFormatException(formatError(debugName, lineNo, line, "face needs at least 3 vertices"));
        }

        int n = tok.length;
        RawVertex[] faceVerts = new RawVertex[n];
        RawUv[] faceUvs = null;
        RawVertex[] faceNormals = null;

        try {
            for (int i = 0; i < n; i++) {
                String corner = tok[i];
                int vIdx, vtIdx = -1, vnIdx = -1;
                if (corner.contains("//")) {
                    String[] parts = corner.split("//");
                    vIdx = Integer.parseInt(parts[0]);
                    vnIdx = Integer.parseInt(parts[1]);
                } else if (corner.contains("/")) {
                    String[] parts = corner.split("/");
                    vIdx = Integer.parseInt(parts[0]);
                    vtIdx = Integer.parseInt(parts[1]);
                    if (parts.length >= 3 && !parts[2].isEmpty()) {
                        vnIdx = Integer.parseInt(parts[2]);
                    }
                } else {
                    vIdx = Integer.parseInt(corner);
                }

                faceVerts[i] = vertices.get(vIdx - 1);
                if (vtIdx >= 0) {
                    if (faceUvs == null) faceUvs = new RawUv[n];
                    faceUvs[i] = uvs.get(vtIdx - 1);
                }
                if (vnIdx >= 0) {
                    if (faceNormals == null) faceNormals = new RawVertex[n];
                    faceNormals[i] = normals.get(vnIdx - 1);
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new ModelFormatException(formatError(debugName, lineNo, line, "malformed or out-of-range face reference"), e);
        }

        // Well-formed OBJ (every real exporter) uses one uniform corner format per face line -
        // CE assumes this too (it validates the whole line against one of 4 regexes up front).
        // This parser sniffs each corner independently instead (see class javadoc), which is
        // strictly more lenient for well-formed files but would otherwise let a genuinely
        // mixed-format line (some corners "v/vt/vn", others bare "v") through with a null hole
        // in faceUvs/faceNormals that would NPE deep inside bakeGroup - reject it here instead,
        // with a diagnostic that names the actual line.
        if (faceUvs != null || faceNormals != null) {
            for (int i = 0; i < n; i++) {
                if ((faceUvs != null && faceUvs[i] == null) || (faceNormals != null && faceNormals[i] == null)) {
                    throw new ModelFormatException(formatError(debugName, lineNo, line, "face mixes vertex-reference formats across its corners"));
                }
            }
        }

        PendingFace face = new PendingFace();
        face.verts = faceVerts;
        face.uvs = faceUvs;
        face.normals = faceNormals;
        return face;
    }

    private static String formatError(String debugName, int lineNo, String line, String reason) {
        return "Error parsing OBJ '" + debugName + "' line " + lineNo + " ('" + line + "'): " + reason;
    }

    private static int packColor(float r, float g, float b, float a) {
        int ai = ((int) (a * 255F)) & 0xFF;
        int ri = ((int) (r * 255F)) & 0xFF;
        int gi = ((int) (g * 255F)) & 0xFF;
        int bi = ((int) (b * 255F)) & 0xFF;
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    /** Fan-triangulates every face in {@code pending} into one flat, already-triangulated vertex stream. */
    private static Group bakeGroup(PendingGroup pending) {
        FloatArrayList data = new FloatArrayList();
        IntArrayList colors = new IntArrayList();
        boolean anyNonWhiteColor = false;

        for (PendingFace face : pending.faces) {
            int n = face.verts.length;

            RawVertex flatNormal = null;
            if (face.normals == null) {
                flatNormal = computeFaceNormal(face.verts[0], face.verts[1], face.verts[2]);
            }

            for (int i = 2; i < n; i++) {
                emitCorner(face, 0, flatNormal, data, colors);
                emitCorner(face, i - 1, flatNormal, data, colors);
                emitCorner(face, i, flatNormal, data, colors);
                if (face.verts[0].color != 0xFFFFFFFF || face.verts[i - 1].color != 0xFFFFFFFF || face.verts[i].color != 0xFFFFFFFF) {
                    anyNonWhiteColor = true;
                }
            }
        }

        // toArray(T[]) rather than the no-arg toFloatArray()/toIntArray() convenience methods:
        // this task's sandbox could not reach the fastutil javadoc to confirm the no-arg
        // methods' exact presence in the 8.5.x line NeoForge 21.1.228 bundles, but the
        // primitive-array-argument overload is confirmed directly against the fastutil classes
        // actually present in this machine's local Gradle installation (javap'd during
        // development of this class) - same method family, verifiably real.
        float[] dataArr = data.toArray(new float[data.size()]);
        int vertexCount = dataArr.length / STRIDE;
        int[] colorArr = anyNonWhiteColor ? colors.toArray(new int[colors.size()]) : null;
        return new Group(pending.name, dataArr, colorArr, vertexCount);
    }

    private static void emitCorner(PendingFace face, int corner, RawVertex flatNormal, FloatArrayList data, IntArrayList colors) {
        RawVertex pos = face.verts[corner];
        RawVertex normal = face.normals != null ? face.normals[corner] : flatNormal;
        RawUv uv = face.uvs != null ? face.uvs[corner] : null;

        data.add(pos.x);
        data.add(pos.y);
        data.add(pos.z);
        data.add(normal.x);
        data.add(normal.y);
        data.add(normal.z);
        data.add(uv != null ? uv.u : 0F);
        data.add(uv != null ? uv.v : 0F);
        colors.add(pos.color);
    }

    /** Verbatim port of CE's {@code Face.calculateFaceNormal()} - always uses the polygon's first 3 corners. */
    private static RawVertex computeFaceNormal(RawVertex v0, RawVertex v1, RawVertex v2) {
        float e1x = v1.x - v0.x, e1y = v1.y - v0.y, e1z = v1.z - v0.z;
        float e2x = v2.x - v0.x, e2y = v2.y - v0.y, e2z = v2.z - v0.z;
        float nx = e1y * e2z - e1z * e2y;
        float ny = e1z * e2x - e1x * e2z;
        float nz = e1x * e2y - e1y * e2x;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0e-6F) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        return new RawVertex(nx, ny, nz);
    }

    // ------------------------------------------------------------------------------------
    // Parse-time-only data. Kept private/nested: no external caller needs raw pre-bake vertex
    // data, only named-group render calls (the public contract above) and part-name lookups.
    // ------------------------------------------------------------------------------------

    private static final class RawVertex {
        final float x, y, z;
        int color = 0xFFFFFFFF;

        RawVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class RawUv {
        final float u, v;

        RawUv(float u, float v) {
            this.u = u;
            this.v = v;
        }
    }

    private static final class PendingFace {
        RawVertex[] verts;
        RawUv[] uvs;
        RawVertex[] normals;
    }

    private static final class PendingGroup {
        final String name;
        final List<PendingFace> faces = new ArrayList<>();

        PendingGroup(String name) {
            this.name = name;
        }
    }

    /** One named, fully-baked (fan-triangulated) group, ready to feed straight into a {@link VertexConsumer}. */
    private static final class Group {
        final String name;
        /** {@link #STRIDE} floats per vertex, {@code vertexCount} vertices, GL_TRIANGLES order. */
        final float[] data;
        /** Packed {@code 0xAARRGGBB} per vertex, or {@code null} if every vertex in this group is default white (the overwhelming common case). */
        final int[] colors;
        final int vertexCount;

        Group(String name, float[] data, int[] colors, int vertexCount) {
            this.name = name;
            this.data = data;
            this.colors = colors;
            this.vertexCount = vertexCount;
        }
    }
}
