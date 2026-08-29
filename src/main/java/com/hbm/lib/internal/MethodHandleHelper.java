package com.hbm.lib.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static com.hbm.lib.internal.AbstractUnsafe.IMPL_LOOKUP;

/**
 * CE's MCP/SRG name-choosing overloads ({@code findX(Class, String mcp, String srg, ...)}, backed by
 * {@code com.hbm.core.HbmCorePlugin}, a 1.12 Forge coremod) are dropped: NeoForge 1.21 runs entirely
 * on Mojang official mappings, so there is no second obfuscated name to choose between.
 */
public final class MethodHandleHelper {

    private MethodHandleHelper() {
    }

    public static MethodHandles.Lookup lookup() {
        return IMPL_LOOKUP;
    }

    public static MethodHandle findVirtual(Class<?> owner, String name, MethodType type) {
        try {
            return IMPL_LOOKUP.findVirtual(owner, name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle findStatic(Class<?> owner, String name, MethodType type) {
        try {
            return IMPL_LOOKUP.findStatic(owner, name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle findSpecial(Class<?> owner, Class<?> specialCaller, String name, MethodType type) {
        try {
            return IMPL_LOOKUP.findSpecial(owner, name, type, specialCaller);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle findGetter(Class<?> owner, String name, Class<?> fieldType) {
        try {
            return IMPL_LOOKUP.findGetter(owner, name, fieldType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle findStaticGetter(Class<?> owner, String name, Class<?> fieldType) {
        try {
            return IMPL_LOOKUP.findStaticGetter(owner, name, fieldType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle findSetter(Class<?> owner, String name, Class<?> fieldType) {
        try {
            return IMPL_LOOKUP.findSetter(owner, name, fieldType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle findStaticSetter(Class<?> owner, String name, Class<?> fieldType) {
        try {
            return IMPL_LOOKUP.findStaticSetter(owner, name, fieldType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandle findConstructor(Class<?> owner, MethodType type) {
        try {
            return IMPL_LOOKUP.findConstructor(owner, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
