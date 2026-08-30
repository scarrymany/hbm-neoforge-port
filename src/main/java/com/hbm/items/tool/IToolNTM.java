package com.hbm.items.tool;

/** One-method default-method marker interface. Ported verbatim from CE; carries no logic of its own. */
public interface IToolNTM {
    default IToolNTM getTool() {
        return this;
    }
}
