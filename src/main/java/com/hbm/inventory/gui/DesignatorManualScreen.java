package com.hbm.inventory.gui;

import com.hbm.items.tool.ItemDesignatorManual;
import com.hbm.items.tool.ToolDataComponents;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.hbm.packet.toserver.ItemControlPacket;

/**
 * Client-only, containerless GUI for {@link ItemDesignatorManual}, ported from CE's
 * {@code GUIScreenDesignator} (213 lines, read in full for this review pass).
 * CE's custom {@code GuiTextField} pair plus hand-drawn flip-X/flip-Z/"here"/"save" button sprites
 * are re-expressed against plain vanilla {@link EditBox}/{@link Button} widgets.
 * <p>
 * "Save" sends a {@link ItemControlPacket} carrying {@code designatorX}/{@code designatorZ} ints,
 * dispatched server-side to {@link ItemDesignatorManual#receiveControl}.
 * <p>
 * Review-pass fixes (all against CE's real {@code GUIScreenDesignator}): HEIGHT corrected from
 * 100 to CE's real {@code ySize} of 126, Flip X/Flip Z/Here now play {@link HBMSoundHandler#buttonYes}
 * on click, Save now plays {@link HBMSoundHandler#techBleep}, Save no longer closes the screen (CE
 * deliberately stays open for repeat adjustments), added hover tooltips on all 4 buttons and the
 * distance readout, matching CE's 5 {@code drawHoveringText} call sites exactly.
 */
public class DesignatorManualScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/gui_designator.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 126;

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
                .bounds(guiLeft + 8, guiTop + 30, 28, 14)
                .tooltip(Tooltip.create(Component.literal("Click to Flip X to -X")))
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Flip Z"), b -> flip(zField))
                .bounds(guiLeft + 8, guiTop + 50, 28, 14)
                .tooltip(Tooltip.create(Component.literal("Click to Flip Z to -Z")))
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Here"), b -> useCurrentPos())
                .bounds(guiLeft + 30, guiTop + 90, 50, 18)
                .tooltip(Tooltip.create(Component.literal("Set coordinates to player position")))
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(guiLeft + 96, guiTop + 90, 50, 18)
                .tooltip(Tooltip.create(Component.literal("Save coordinates")))
                .build());

        this.setInitialFocus(this.xField);
    }

    private void playButtonYes() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(HBMSoundHandler.buttonYes.get(), 1.0F));
    }

    private void flip(EditBox field) {
        int value = parse(field);
        field.setValue(Integer.toString(-value));
        playButtonYes();
    }

    private void useCurrentPos() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        xField.setValue(Integer.toString((int) player.getX()));
        zField.setValue(Integer.toString((int) player.getZ()));
        playButtonYes();
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
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(HBMSoundHandler.techBleep.get(), 1.0F));
        // CE does NOT close the screen on save (it flashes the button and stays open for repeat
        // adjustments) - see this class's own javadoc.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, WIDTH, HEIGHT);
        guiGraphics.drawCenteredString(this.font, this.title, guiLeft + WIDTH / 2, guiTop + 8, 0xFFFFFF);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            long dx = parse(xField) - (long) player.getX();
            long dz = parse(zField) - (long) player.getZ();
            long distance = (long) Math.sqrt(dx * dx + dz * dz);
            int distanceY = guiTop + 72;
            guiGraphics.drawString(this.font, "Distance: " + distance + " m", guiLeft + 8, distanceY, 0x0091FF, false);

            if (isHovering(guiLeft + 8, distanceY, 100, this.font.lineHeight, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, Component.literal("Distance from player to coordinates"), mouseX, mouseY);
            }
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
