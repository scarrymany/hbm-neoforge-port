package com.hbm.util.i18n;

import net.neoforged.fml.loading.FMLLoader;

import java.util.List;

/**
 * CE dispatched client-vs-server translation through {@code FMLCommonHandler.instance().getSide()}.
 * That type is gone in NeoForge; {@link FMLLoader#getDist()} is the direct replacement for asking
 * which physical side is currently running, confirmed via the Neo Edition reference
 * ({@code NuclearTechMod} picks its proxy the same way). This area does not own the mod's
 * client/server proxy infrastructure (that belongs to {@code com.hbm.main}), so the dispatch is done
 * here directly instead of routing through a proxy field.
 * <p>
 * {@link I18nClient} is {@code @OnlyIn(Dist.CLIENT)} and does not exist in a dedicated server's
 * stripped jar, so it must never be referenced from an eagerly-initialized field - {@link ClientHolder}
 * defers loading that class until {@link #delegate()} actually needs it on the client.
 */
public final class I18nUtil {

    private I18nUtil() {
    }

    private static ITranslate delegate() {
        return FMLLoader.getDist().isClient() ? ClientHolder.INSTANCE : ServerHolder.INSTANCE;
    }

    private static final class ClientHolder {
        static final ITranslate INSTANCE = new I18nClient();
    }

    private static final class ServerHolder {
        static final ITranslate INSTANCE = new I18nServer();
    }

    // Simple wrapper for I18n, for consistency
    public static String resolveKey(String s, Object... args) {
        return delegate().resolveKey(s, args);
    }

    public static String format(String s, Object... args) {
        return delegate().resolveKey(s, args);
    } //alias

    public static boolean exist(String s) {
        return delegate().exist(s);
    }

    // Wrapper for I18n but cuts up the result using NTM's line break character ($)
    public static String[] resolveKeyArray(String s, Object... args) {
        return delegate().resolveKeyArray(s, args);
    }

    // The same as autoBreak, but it also respects NTM's break character ($) for manual line breaking in addition to the automatic ones
    public static List<String> autoBreakWithParagraphs(Object fontRenderer, String text, int width) {
        return delegate().autoBreakWithParagraphs(fontRenderer, text, width);
    }

    // Turns one string into a list of strings, cutting sentences up to fit within the defined width if they were rendered in a GUI
    public static List<String> autoBreak(Object fontRenderer, String text, int width) {
        return delegate().autoBreak(fontRenderer, text, width);
    }
}
