package com.hbm.inventory.gui;

import com.hbm.items.tool.ItemDesignatorManual;
import com.hbm.items.tool.ToolDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.hbm.packet.toserver.ItemControlPacket;

/**
 * Client-only, containerless GUI for {@link ItemDesignatorManual}, ported from CE's
 * {@code GUIScreenDesignator} (213 lines, read in full). CE's custom {@code GuiTextField} pair plus
 * hand-drawn flip-X/flip-Z/"here"/"save" button sprites are re-expressed against plain vanilla
 * {@link EditBox}/{@link Button} widgets (this port has no equivalent hand-rolled texture-button
 * framework, same simplification this port's {@code TurretMobFilterScreen} already established for
 * a bare-Screen GUI) - the underlying behavior (type X/Z, optionally flip sign, optionally snap to
 * the player's current position, Save writes back onto the held stack) is preserved exactly.
 * <p>
 * "Save" sends a {@link ItemControlPacket} carrying {@code designatorX}/{@code designatorZ} ints,
 * dispatched server-side to {@link ItemDesignatorManual#receiveControl} - see that class's javadoc
 * for why this reuses the existing generic control-packet mechanism rather than a new payload.
 */
public class DesignatorManualScreen extends Screen {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 100;

    private final ItemStack heldStack;
    private int guiLeft;
    private int guiTop;

    private EditBox xField;
    private EditBox zField;

    public DesignatorManualScreen(ItemStack heldStack) {
        super(Component.literal("Manual Designator"));
        this.heldStack = heldStack;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - WIDTH) / 2;
        this.guiTop = (this.height - HEIGHT) / 2;

        BlockPos saved = heldStack.get(ToolDataComponents.DETONATOR_POS.get());
        int startX = saved != null ? saved.getX() : 0;
        int startZ = saved != null ? saved.getZ() : 0;

        this.xField = new EditBox(this.font, guiLeft + 40, guiTop + 30, 100, 14, Component.literal("X"));
        this.xField.setValue(Integer.toString(startX));
        this.addRenderableWidget(this.xField);

        this.zField = new EditBox(this.font, guiLeft + 40, guiTop + 50, 100, 14, Component.literal("Z"));
        this.zField.setValue(Integer.toString(startZ));
        this.addRenderableWidget(this.zField);

        this.addRenderableWidget(Button.builder(Component.literal("Flip X"), b -> flip(xField))
                .bounds(guiLeft + 8, guiTop + 30, 28, 14).build());
        this.addRenderableWidget(Button.builder(Component.literal("Flip Z"), b -> flip(zField))
                .bounds(guiLeft + 8, guiTop + 50, 28, 14).build());

        this.addRenderableWidget(Button.builder(Component.literal("Here"), b -> useCurrentPos())
                .bounds(guiLeft + 30, guiTop + 74, 50, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(guiLeft + 96, guiTop + 74, 50, 18).build());

        this.setInitialFocus(this.xField);
    }

    private void flip(EditBox field) {
        int value = parse(field);
        field.setValue(Integer.toString(-value));
    }

    private void useCurrentPos() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        xField.setValue(Integer.toString((int) player.getX()));
        zField.setValue(Integer.toString((int) player.getZ()));
    }

    private int parse(EditBox field) {
        try {
            return Integer.parseInt(field.getValue().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        data.putInt("designatorX", parse(xField));
        data.putInt("designatorZ", parse(zField));
        PacketDistributor.sendToServer(new ItemControlPacket(data));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(guiLeft, guiTop, guiLeft + WIDTH, guiTop + HEIGHT, 0xC0101010);
        guiGraphics.drawCenteredString(this.font, this.title, guiLeft + WIDTH / 2, guiTop + 8, 0xFFFFFF);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            long dx = parse(xField) - (long) player.getX();
            long dz = parse(zField) - (long) player.getZ();
            long distance = (long) Math.sqrt(dx * dx + dz * dz);
            guiGraphics.drawString(this.font, "Distance: " + distance + " m", guiLeft + 8, guiTop + HEIGHT - 12, 0x0091FF, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
