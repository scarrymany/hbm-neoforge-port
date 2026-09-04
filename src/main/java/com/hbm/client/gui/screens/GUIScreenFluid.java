package com.hbm.client.gui.screens;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidIDMulti;
import com.hbm.items.machine.MachineItems;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.ItemControlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Port of CE {@code com.hbm.inventory.gui.GUIScreenFluid} (189 lines).
 * Containerless search GUI for {@link ItemFluidIDMulti}: up to 9 matching {@link FluidType}s,
 * left-click sets primary, right-click sets secondary. Saves via {@link ItemControlPacket}
 * (port of CE {@code NBTItemControlPacket}) — no pipe spreading.
 */
public class GUIScreenFluid extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_fluid.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 54;

    private final Player player;
    private int guiLeft;
    private int guiTop;
    private EditBox search;

    private FluidType primary = Fluids.NONE;
    private FluidType secondary = Fluids.NONE;
    private final FluidType[] searchArray = new FluidType[9];

    public GUIScreenFluid(Player player) {
        super(Component.translatable("item.hbm.fluid_identifier"));
        this.player = player;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - WIDTH) / 2;
        this.guiTop = (this.height - HEIGHT) / 2;

        this.search = new EditBox(this.font, guiLeft + 46, guiTop + 11, 86, 12, Component.literal("Search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setResponder(s -> updateSearch());
        this.addRenderableWidget(this.search);
        this.setInitialFocus(this.search);

        if (player.getMainHandItem().getItem() == MachineItems.FLUID_IDENTIFIER.get()) {
            this.primary = ItemFluidIDMulti.getType(player.getMainHandItem(), true);
            this.secondary = ItemFluidIDMulti.getType(player.getMainHandItem(), false);
        }

        updateSearch();
    }

    @Override
    public void tick() {
        if (player.getMainHandItem().isEmpty()
                || player.getMainHandItem().getItem() != MachineItems.FLUID_IDENTIFIER.get()) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawBackgroundLayer(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawTooltips(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (int) mouseX;
        int j = (int) mouseY;

        for (int k = 0; k < this.searchArray.length; k++) {
            if (this.searchArray[k] == null) {
                break;
            }
            if (guiLeft + 7 + k * 18 <= i && guiLeft + 7 + k * 18 + 18 > i
                    && guiTop + 29 < j && guiTop + 29 + 18 >= j) {
                if (button == 0) {
                    playClickSound();
                    this.primary = this.searchArray[k];
                    CompoundTag data = new CompoundTag();
                    data.putInt("primary", this.primary.getID());
                    PacketDistributor.sendToServer(new ItemControlPacket(data));
                } else if (button == 1) {
                    playClickSound();
                    this.secondary = this.searchArray[k];
                    CompoundTag data = new CompoundTag();
                    data.putInt("secondary", this.secondary.getID());
                    PacketDistributor.sendToServer(new ItemControlPacket(data));
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (this.search.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawBackgroundLayer(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, WIDTH, HEIGHT);

        if (this.search.isFocused()) {
            graphics.blit(TEXTURE, guiLeft + 43, guiTop + 7, 166, 54, 90, 18);
        }

        for (int k = 0; k < this.searchArray.length; k++) {
            FluidType type = this.searchArray[k];
            if (type == null) {
                break;
            }

            int color = type.getColor();
            graphics.setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1.0F);
            graphics.blit(TEXTURE, guiLeft + 12 + k * 18, guiTop + 31, 12 + k * 18, 56, 8, 14);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (type == this.primary && type == this.secondary) {
                graphics.blit(TEXTURE, guiLeft + 7 + k * 18, guiTop + 29, 176, 36, 18, 18);
            } else if (type == this.primary) {
                graphics.blit(TEXTURE, guiLeft + 7 + k * 18, guiTop + 29, 176, 0, 18, 18);
            } else if (type == this.secondary) {
                graphics.blit(TEXTURE, guiLeft + 7 + k * 18, guiTop + 29, 176, 18, 18, 18);
            }
        }
    }

    private void drawTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int k = 0; k < this.searchArray.length; k++) {
            if (this.searchArray[k] == null) {
                break;
            }
            if (guiLeft + 7 + k * 18 <= mouseX && guiLeft + 7 + k * 18 + 18 > mouseX
                    && guiTop + 29 < mouseY && guiTop + 29 + 18 >= mouseY) {
                graphics.renderTooltip(this.font, this.searchArray[k].getLocalizedName(), mouseX, mouseY);
            }
        }
    }

    private void updateSearch() {
        for (int i = 0; i < this.searchArray.length; i++) {
            this.searchArray[i] = null;
        }

        int next = 0;
        String subs = this.search.getValue().toLowerCase(Locale.US);

        for (FluidType type : Fluids.getInNiceOrder()) {
            String name = type.getLocalizedName().getString().toLowerCase(Locale.US);
            if (name.contains(subs) && !type.hasNoID()) {
                this.searchArray[next] = type;
                next++;
                if (next >= 9) {
                    return;
                }
            }
        }
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
