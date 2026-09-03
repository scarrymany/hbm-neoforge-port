package com.hbm.client.gui.screens.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.inventory.container.machine.rbmk.RBMKControlAutoMenu;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKControlAuto}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKControlAuto.java
 * <p>
 * GUI with control rod level bar, function selector, and 4 text input fields for auto-control parameters.
 */
public class RBMKControlAutoScreen extends AbstractContainerScreen<RBMKControlAutoMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_control_auto.png");
    private EditBox[] fields;

    public RBMKControlAutoScreen(RBMKControlAutoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        // CE GUIRBMKControlAuto.java:40-60: 4 text fields
        fields = new EditBox[4];
        RBMKControlAutoBlockEntity be = menu.be;
        for (int i = 0; i < 4; i++) {
            fields[i] = new EditBox(this.font, leftPos + 30, topPos + 27 + 11 * i, 26, 6, Component.empty());
            fields[i].setBordered(false);
            fields[i].setMaxLength(i < 2 ? 3 : 4);
            fields[i].setTextColor(0xFFFFFF);
            addWidget(fields[i]);
        }
        fields[0].setValue(String.valueOf((int) be.levelUpper));
        fields[1].setValue(String.valueOf((int) be.levelLower));
        fields[2].setValue(String.valueOf((int) be.heatUpper));
        fields[3].setValue(String.valueOf((int) be.heatLower));
    }


    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        RBMKControlAutoBlockEntity be = menu.be;

        // CE GUIRBMKControlAuto.java:154-157: rod level bar (8x56px) at x=124, y=29
        int height = (int) (56 * (1D - be.extraction));
        if (height > 0) {
            graphics.blit(TEXTURE, leftPos + 124, topPos + 29, 176, 56 - height, 8, height);
        }

        // CE GUIRBMKControlAuto.java:159-160: function indicator (26x19px) at x=59, y=27
        int f = be.function.ordinal();
        graphics.blit(TEXTURE, leftPos + 59, topPos + 27, 184, f * 19, 26, 19);

        // CE GUIRBMKControlAuto.java:162-164: power indicator (16x16px) at x=136, y=21
        if (be.isPowered()) {
            graphics.blit(TEXTURE, leftPos + 136, topPos + 21, 210, be.hasPower ? 16 : 0, 16, 16);
        }

        // CE GUIRBMKControlAuto.java:172-180: render text fields
        for (EditBox field : fields) {
            field.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKControlAuto.java:141-144: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIRBMKControlAuto.java:64-87: tooltips for various UI elements
        RBMKControlAutoBlockEntity be = menu.be;
        if (mouseX >= leftPos + 124 && mouseX < leftPos + 124 + 16 && mouseY >= topPos + 29 && mouseY < topPos + 29 + 56) {
            graphics.renderTooltip(this.font, Component.literal((int) (be.extraction * 100) + "%"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 58 && mouseX < leftPos + 58 + 28 && mouseY >= topPos + 26 && mouseY < topPos + 26 + 19) {
            String func = "Function: ";
            switch (be.function) {
                case LINEAR: func += "Linear"; break;
                case QUAD_UP: func += "Quadratic"; break;
                case QUAD_DOWN: func += "Inverse Quadratic"; break;
            }
            graphics.renderTooltip(this.font, Component.literal(func), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIRBMKControlAuto.java:90-138: handle field clicks + save button + function buttons
        for (EditBox field : fields) {
            if (field.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Save button at x=28, y=70, 30x10 (CE :99-124)
        if (mouseX >= leftPos + 28 && mouseX < leftPos + 58 && mouseY >= topPos + 70 && mouseY < topPos + 80) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            double[] vals = new double[4];
            for (int k = 0; k < 4; k++) {
                double clamp = k < 2 ? 100 : 9999;
                try {
                    double parsed = Double.parseDouble(fields[k].getValue());
                    int clamped = (int) Mth.clamp(parsed, 0, clamp);
                    fields[k].setValue(String.valueOf(clamped));
                    vals[k] = clamped;
                } catch (NumberFormatException e) {
                    fields[k].setValue("0");
                    vals[k] = 0;
                }
            }
            data.putDouble("levelUpper", vals[0]);
            data.putDouble("levelLower", vals[1]);
            data.putDouble("heatUpper", vals[2]);
            data.putDouble("heatLower", vals[3]);
            PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), data));
            return true;
        }

        // Function buttons at x=61, y=48+k*11 (3 buttons, 22x10) (CE :127-136)
        for (int k = 0; k < 3; k++) {
            if (mouseX >= leftPos + 61 && mouseX < leftPos + 83 && mouseY >= topPos + 48 + k * 11 && mouseY < topPos + 58 + k * 11) {
                playClickSound();
                CompoundTag data = new CompoundTag();
                data.putInt("function", k);
                PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), data));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound() {
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox field : fields) {
            if (field.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox field : fields) {
            if (field.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
}
