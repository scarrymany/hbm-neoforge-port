package com.hbm.render.loader;

/**
 * Direct, unmodified port of CE's {@code com.hbm.render.loader.ModelFormatException}
 * (upstream/hbm-ce/src/main/java/com/hbm/render/loader/ModelFormatException.java) - a plain
 * unchecked exception wrapping OBJ-parse failures (bad syntax, out-of-range vertex/normal/uv
 * indices, IO errors reading the resource). Thrown by {@link HbmObjModel#load(net.minecraft.resources.ResourceLocation)}
 * and {@link HbmObjModel#parse(String, java.io.InputStream)}.
 */
public class ModelFormatException extends RuntimeException {

    private static final long serialVersionUID = 2023547503969671835L;

    public ModelFormatException() {
        super();
    }

    public ModelFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelFormatException(String message) {
        super(message);
    }

    public ModelFormatException(Throwable cause) {
        super(cause);
    }
}
