package com.hbm.interfaces;

import com.hbm.util.Keypad;

/** CE {@code com.hbm.interfaces.IKeypadHandler}. */
public interface IKeypadHandler {

    Keypad getKeypad();

    default void keypadActivated() {
    }

    default void passwordSet() {
    }
}
