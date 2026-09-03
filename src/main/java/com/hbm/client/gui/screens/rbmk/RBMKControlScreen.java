package com.hbm.client.gui.screens.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlManualBlockEntity;
import com.hbm.inventory.container.machine.rbmk.RBMKControlMenu;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKControl}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKControl.java
 * <p>
 * GUI with control rod level bar, color group selector, power indicator, and level buttons.
 */
public class RBMKControlScreen extends AbstractContainerScreen<RBMKControlMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_control.png");

    public RBMKControlScreen(RBMKControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        RBMKControlManualBlockEntity be = menu.be;

        // CE GUIRBMKControl.java:86-89: rod level bar (8x56px) at x=75, y=29
        int height = (int) (56 * (1D - be.extraction));
        if (height > 0) {
            graphics.blit(TEXTURE, leftPos + 75, topPos + 29, 176, 56 - height, 8, height);
        }

        // CE GUIRBMKControl.java:91-95: color group indicator (12x10px)
        if (be.color != null) {
            int color = be.color.ordinal();
            graphics.blit(TEXTURE, leftPos + 28, topPos + 26 + color * 11, 184, color * 10, 12, 10);
        }

        // CE GUIRBMKControl.java:97-99: power indicator (16x16px) at x=87, y=21
        if (be.isPowered()) {
            graphics.blit(TEXTURE, leftPos + 87, topPos + 21, 196, be.hasPower ? 16 : 0, 16, 16);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKControl.java:73-76: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIRBMKControl.java:47-68: level buttons (5 rows at x=118, y=26+k*11, 30x10) + color buttons (5 at x=28, y=26+k*11, 12x10)
        for (int k = 0; k < 5; k++) {
            // Level control buttons (CE :51-58)
            if (mouseX >= leftPos + 118 && mouseX < leftPos + 118 + 30 && mouseY >= topPos + 26 + k * 11 && mouseY < topPos + 26 + 10 + k * 11) {
                CompoundTag data = new CompoundTag();
                data.putDouble("level", 1.0D - (k * 0.25D));
                PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), data));
                return true;
            }

            // Color group buttons (CE :61-67)
            if (mouseX >= leftPos + 28 && mouseX < leftPos + 28 + 12 && mouseY >= topPos + 26 + k * 11 && mouseY < topPos + 26 + 10 + k * 11) {
                CompoundTag data = new CompoundTag();
                data.putInt("color", k);
                PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), data));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIRBMKControl.java:38-42: tooltips for level bar and power indicator
        RBMKControlManualBlockEntity be = menu.be;
        if (mouseX >= leftPos + 71 && mouseX < leftPos + 71 + 16 && mouseY >= topPos + 29 && mouseY < topPos + 29 + 56) {
            graphics.renderTooltip(this.font, Component.literal((int) (be.extraction * 100) + "%"), mouseX, mouseY);
        }
        if (be.isPowered() && mouseX >= leftPos + 87 && mouseX < leftPos + 87 + 16 && mouseY >= topPos + 21 && mouseY < topPos + 21 + 16) {
            graphics.renderTooltip(this.font, Component.literal(String.format("%.1f / %.1f HE", (double) be.power, (double) RBMKControlBlockEntity.MAX_POWER)), mouseX, mouseY);
        }
    }
}
