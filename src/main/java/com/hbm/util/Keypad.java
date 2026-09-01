package com.hbm.util;

import com.hbm.interfaces.IKeypadHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** CE {@code com.hbm.util.Keypad} — server logic. Client VFX (KeypadClient OBJ) not ported. */
public class Keypad {

    public final BlockEntity te;
    public final Button[] buttons = new Button[12];
    public byte successColorTicks = 0;
    public byte failColorTicks = 0;
    public boolean isActive = true;
    public int storedCode = -1;
    public byte[] code = new byte[]{-1, -1, -1, -1, -1, -1};
    public boolean isSettingCode = true;

    public Keypad(BlockEntity te) {
        this.te = te;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Button();
        }
    }

    public void update() {
        boolean active = false;
        for (Button b : buttons) {
            if (b.cooldown > 0) {
                b.cooldown--;
                active = true;
            }
        }
        if (successColorTicks > 0) {
            successColorTicks--;
            active = true;
        }
        if (failColorTicks > 0) {
            failColorTicks--;
            active = true;
        }
        if (isSettingCode) {
            active = true;
        }
        isActive = active;
    }

    public void buttonClicked(int id) {
        if (id < 0 || id >= buttons.length) return;
        if (buttons[id].cooldown != 0) return;
        buttons[id].cooldown = 20;
        byte num;
        switch (id) {
            case 9 -> {
                int newCode = buildIntCode();
                if (storedCode == newCode) {
                    isSettingCode = true;
                }
                clearCode();
                return;
            }
            case 10 -> num = 0;
            case 11 -> {
                int newCode = buildIntCode();
                if (isSettingCode) {
                    storedCode = newCode;
                    successColorTicks = 20;
                    isSettingCode = false;
                    if (te instanceof IKeypadHandler handler) {
                        handler.passwordSet();
                    }
                } else if (storedCode == newCode) {
                    if (te instanceof IKeypadHandler handler) {
                        handler.keypadActivated();
                    }
                    successColorTicks = 20;
                } else {
                    failColorTicks = 20;
                }
                clearCode();
                return;
            }
            default -> num = (byte) (id + 1);
        }
        if (num < 10 && code[code.length - 1] < 0) {
            successColorTicks = 0;
            failColorTicks = 0;
            shiftCode();
            code[0] = num;
        }
    }

    public void shiftCode() {
        for (int i = code.length - 1; i > 0; i--) {
            code[i] = code[i - 1];
            code[i - 1] = -1;
        }
    }

    public void clearCode() {
        for (int i = 0; i < code.length; i++) {
            code[i] = -1;
        }
    }

    public int buildIntCode() {
        if (code[0] < 0) return -1;
        int num = 0;
        for (int i = code.length - 1; i >= 0; i--) {
            if (code[i] < 0) continue;
            num = num * 10 + code[i];
        }
        return num;
    }

    public CompoundTag writeToNbt(CompoundTag tag) {
        tag.putByteArray("code", code);
        tag.putInt("currentPassword", storedCode);
        tag.putBoolean("isSettingCode", isSettingCode);
        tag.putByte("successColorTicks", successColorTicks);
        tag.putByte("failColorTicks", failColorTicks);
        byte[] cooldowns = new byte[12];
        for (int i = 0; i < 12; i++) {
            cooldowns[i] = buttons[i].cooldown;
        }
        tag.putByteArray("buttonCooldowns", cooldowns);
        return tag;
    }

    public void readFromNbt(CompoundTag tag) {
        byte[] readCode = tag.getByteArray("code");
        if (readCode.length == 6) code = readCode;
        storedCode = tag.getInt("currentPassword");
        isSettingCode = tag.getBoolean("isSettingCode");
        successColorTicks = tag.getByte("successColorTicks");
        failColorTicks = tag.getByte("failColorTicks");
        byte[] buttonCooldowns = tag.getByteArray("buttonCooldowns");
        if (buttonCooldowns.length == 12) {
            for (int i = 0; i < 12; i++) {
                buttons[i].cooldown = buttonCooldowns[i];
            }
        }
    }

    public static class Button {
        public byte cooldown = 0;
    }
}
