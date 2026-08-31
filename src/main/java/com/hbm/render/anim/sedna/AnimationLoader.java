package com.hbm.render.anim.sedna;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.EType;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.HType;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.IType;
import com.hbm.render.anim.sedna.BusAnimationSequenceSedna.Dimension;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Port of CE's {@code com.hbm.render.anim.sedna.AnimationLoader} (209 lines) - the JSON keyframe
 * parser that turns one {@code assets/hbm/models/weapons/animations/*.json} resource into a
 * {@code Map<String, BusAnimationSedna>} keyed by top-level Blender-action name (e.g.
 * {@code "Fire"}, {@code "Reload"}, {@code "Inspect"}). Read in full directly from CE for this port.
 * CE's own header comments (preserved for provenance/context - explain <i>why</i> this JSON format
 * exists at all, alongside the Collada {@code com.hbm.animloader} system that is explicitly out of
 * this task's scope):
 * <pre>
 * // The collada loader is great, but is not so backwards compatible and spews keyframes rather
 * // than doing interpolation. Yeah - more animation loading is not so great, but 3mb for a single
 * // door opening is maybe overkill on a 50mb mod, and even though the format supports multiple
 * // animations, no animation software will actually export multiple animations (even though
 * // Blender has a toggle for it, it doesn't _do_ anything). This instead just loads transformation
 * // data from a JSON file, turning it into a set of BusAnimations. See ntm-animator.blend for a
 * // JSON format creation script.
 * </pre>
 *
 * <h2>1.21.1 API delta (the only real change from CE's 1.12.2 version)</h2>
 * CE's resource read is {@code Minecraft.getMinecraft().getResourceManager().getResource(file)
 * .getInputStream()} (throws {@link IOException}, caught -> returns {@code null}). This port uses
 * the confirmed-real 1.21.1 shape instead - {@code Optional<Resource>}-returning
 * {@code ResourceManager#getResource(ResourceLocation)}, read directly from
 * {@code upstream/neo-edition}'s own real, compiling {@code com.hbm.render.anim.AnimationLoader}
 * (same method body shape, cross-checked for API surface only per this task's ground rules - the
 * JSON format itself and every parsing decision below is CE's, not Neo Edition's). The JSON format
 * is otherwise engine-agnostic and required zero changes.
 */
public class AnimationLoader {

    public static final Gson gson = new Gson();

    /**
     * Loads and parses {@code file} into a fresh {@code Map<String, BusAnimationSedna>}, or
     * {@code null} if the resource does not exist or fails to read - matching CE's own
     * "silently return null, let the caller's {@code .get(name)}-on-null NPE (or the caller guard
     * against it)" contract exactly; not wrapped in a friendlier {@link Optional} here so a 1:1
     * port of every CE {@code ResourceManager.xxx_anim.get("...")}-style call site behaves
     * identically. Not cached - callers wanting a single shared, resource-reload-aware instance
     * should hold the returned map themselves (mirroring CE's own
     * {@code public static final HashMap<...> xxx_anim = AnimationLoader.load(...)} per-gun static
     * field pattern), same as this port's {@code com.hbm.render.loader.HbmObjModel} does for OBJ
     * geometry - unlike that class, no reload-listener is provided here since re-parsing a live
     * animation JSON mid-game while an animation is actively playing raises implicit-in-progress-
     * state questions CE itself never has to answer (its own loader has no reload listener either).
     */
    public static Map<String, BusAnimationSedna> load(ResourceLocation file) {
        HashMap<String, BusAnimationSedna> animations = new HashMap<>();

        JsonObject json;
        try {
            Optional<Resource> resourceOpt = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resourceOpt.isEmpty()) return null;

            try (InputStream in = resourceOpt.get().open();
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                json = gson.fromJson(reader, JsonObject.class);
            }
        } catch (IOException ex) {
            return null;
        }

        if (json == null) return null;

        // Load our model offsets, we'll place these into all the sequences that share the name of
        // the offset. The offsets are only required when sequences are played for an object, which
        // is why we don't globally offset! The obj rendering handles the non-animated case fine.
        // Effectively, this removes double translation AND ensures that rotations occur around the
        // individual object origin, rather than the weapon origin. (CE's own comment, preserved.)
        HashMap<String, double[]> offsets = new HashMap<>();
        if (json.has("offset")) {
            for (Map.Entry<String, JsonElement> root : json.getAsJsonObject("offset").entrySet()) {
                JsonArray array = root.getValue().getAsJsonArray();

                double[] offset = new double[3];
                for (int i = 0; i < 3; i++) {
                    offset[i] = array.get(i).getAsDouble();
                }

                offsets.put(root.getKey(), offset);
            }
        }

        // Rotation modes, swizzled into our local space. YZX in blender becomes XYZ due to:
        //  * rotation order reversed in blender (XYZ -> ZYX)
        //  * dimensions Y and Z are swapped in blender (ZYX -> YZX)
        // (CE's own comment, preserved - and CE's own bug, preserved too: the parsed rotMode array
        // is built but never stored into `rotModes`, so every real CE animation JSON in practice
        // falls through to BusAnimationSequenceSedna's own {0,1,2} identity default below. Verified:
        // no shipped CE animation JSON under models/weapons/animations/*.json even has a top-level
        // "rotmode" key, so this dead branch never actually executes for any real asset - a 1:1
        // port keeps the same no-op shape rather than silently "fixing" behavior CE itself never
        // exercises.)
        HashMap<String, double[]> rotModes = new HashMap<>();
        if (json.has("rotmode")) {
            for (Map.Entry<String, JsonElement> root : json.getAsJsonObject("rotmode").entrySet()) {
                String mode = root.getValue().getAsString();

                double[] rotMode = new double[3];
                rotMode[0] = getRot(mode.charAt(2));
                rotMode[1] = getRot(mode.charAt(0));
                rotMode[2] = getRot(mode.charAt(1));
                // Note: CE never calls rotModes.put(...) here either - see comment above.
            }
        }

        // Top level parsing, this is for the animation name as set in Blender
        for (Map.Entry<String, JsonElement> root : json.getAsJsonObject("anim").entrySet()) {
            BusAnimationSedna animation = new BusAnimationSedna();

            // Loading the buses for this animation
            JsonObject entryObject = root.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> model : entryObject.entrySet()) {
                String modelName = model.getKey();
                double[] offset = new double[3];
                double[] rotMode = new double[]{0, 1, 2};
                if (offsets.containsKey(modelName)) offset = offsets.get(modelName);
                if (rotModes.containsKey(modelName)) rotMode = rotModes.get(modelName);
                animation.addBus(modelName, loadSequence(model.getValue().getAsJsonObject(), offset, rotMode));
            }

            animations.put(root.getKey(), animation);
        }

        return animations;
    }

    private static double getRot(char value) {
        return switch (value) {
            case 'X' -> 0;
            case 'Y' -> 1;
            case 'Z' -> 2;
            default -> 0;
        };
    }

    private static BusAnimationSequenceSedna loadSequence(JsonObject json, double[] offset, double[] rotMode) {
        BusAnimationSequenceSedna sequence = new BusAnimationSequenceSedna();

        // Location fcurves
        if (json.has("location")) {
            JsonObject location = json.getAsJsonObject("location");

            if (location.has("x")) addToSequence(sequence, Dimension.TX, location.getAsJsonArray("x"));
            if (location.has("y")) addToSequence(sequence, Dimension.TY, location.getAsJsonArray("y"));
            if (location.has("z")) addToSequence(sequence, Dimension.TZ, location.getAsJsonArray("z"));
        }

        // Rotation fcurves, only euler at the moment
        if (json.has("rotation_euler")) {
            JsonObject rotation = json.getAsJsonObject("rotation_euler");

            if (rotation.has("x")) addToSequence(sequence, Dimension.RX, rotation.getAsJsonArray("x"));
            if (rotation.has("y")) addToSequence(sequence, Dimension.RY, rotation.getAsJsonArray("y"));
            if (rotation.has("z")) addToSequence(sequence, Dimension.RZ, rotation.getAsJsonArray("z"));
        }

        // Scale fcurves
        if (json.has("scale")) {
            JsonObject scale = json.getAsJsonObject("scale");

            if (scale.has("x")) addToSequence(sequence, Dimension.SX, scale.getAsJsonArray("x"));
            if (scale.has("y")) addToSequence(sequence, Dimension.SY, scale.getAsJsonArray("y"));
            if (scale.has("z")) addToSequence(sequence, Dimension.SZ, scale.getAsJsonArray("z"));
        }

        sequence.offset = offset;
        sequence.rotMode = rotMode;

        return sequence;
    }

    private static void addToSequence(BusAnimationSequenceSedna sequence, Dimension dimension, JsonArray array) {
        IType prevInterp = null;
        for (JsonElement element : array) {
            BusAnimationKeyframeSedna keyframe = loadKeyframe(element, prevInterp);
            prevInterp = keyframe.interpolationType;
            sequence.addKeyframe(dimension, keyframe);
        }
    }

    private static BusAnimationKeyframeSedna loadKeyframe(JsonElement element, IType prevInterp) {
        JsonArray array = element.getAsJsonArray();

        double value = array.get(0).getAsDouble();
        int duration = array.get(1).getAsInt();
        IType interpolation = array.size() >= 3 ? IType.valueOf(array.get(2).getAsString()) : IType.LINEAR;
        EType easing = array.size() >= 4 ? EType.valueOf(array.get(3).getAsString()) : EType.AUTO;

        BusAnimationKeyframeSedna keyframe = new BusAnimationKeyframeSedna(value, duration, interpolation, easing);

        int i = 4;

        if (prevInterp == IType.BEZIER) {
            keyframe.leftX = array.get(i++).getAsDouble();
            keyframe.leftY = array.get(i++).getAsDouble();
            keyframe.leftType = HType.valueOf(array.get(i++).getAsString());
        }

        if (interpolation == IType.LINEAR || interpolation == IType.CONSTANT)
            return keyframe;

        if (interpolation == IType.BEZIER) {
            keyframe.rightX = array.get(i++).getAsDouble();
            keyframe.rightY = array.get(i++).getAsDouble();
            keyframe.rightType = HType.valueOf(array.get(i++).getAsString());
        }

        if (interpolation == IType.ELASTIC) {
            keyframe.amplitude = array.get(i++).getAsDouble();
            keyframe.period = array.get(i++).getAsDouble();
        } else if (interpolation == IType.BACK) {
            keyframe.back = array.get(i++).getAsDouble();
        }

        return keyframe;
    }
}
