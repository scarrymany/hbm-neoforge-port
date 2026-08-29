package com.hbm.util.i18n;

import java.util.Collections;
import java.util.List;

/**
 * CE's server-side branch of {@code I18nUtil.resolveKey} (there was no dedicated server class there)
 * just returned the untranslated key {@code s} as a safe fallback, since the server has no lang file
 * loaded to translate against. This class preserves that behavior instead of substituting a fixed
 * placeholder string, so any server-side caller that still expects a usable string back (a log message,
 * command feedback, text later sent to a client) gets the key rather than nonsense.
 */
public class I18nServer implements ITranslate {

    @Override
    public String resolveKey(String s, Object... args) {
        return s;
    }

    @Override
    public boolean exist(String key) {
        return false;
    }

    @Override
    public String[] resolveKeyArray(String s, Object... args) {
        return resolveKey(s, args).split("\\$");
    }

    @Override
    public List<String> autoBreakWithParagraphs(Object fontRenderer, String text, int width) {
        return Collections.singletonList(text);
    }

    @Override
    public List<String> autoBreak(Object fontRenderer, String text, int width) {
        return Collections.singletonList(text);
    }
}
