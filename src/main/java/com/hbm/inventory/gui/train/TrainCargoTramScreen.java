package com.hbm.inventory.gui.train;

import com.hbm.inventory.container.TrainCargoTramMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link TrainCargoTramMenu} - port of CE's {@code GUITrainCargoTram}. Review-pass fix:
 * {@code ModMenuTypes.TRAIN_CARGO_TRAM} was a fully working, entity-reachable
 * ({@code TrainCargoTram#interact} calls {@code player.openMenu(this, ...)}) {@link
 * net.minecraft.world.inventory.MenuType}/{@link TrainCargoTramMenu} pair with no client-side
 * {@code Screen} bound to it anywhere - {@link net.minecraft.client.gui.screens.MenuScreens} silently
 * refuses to open a menu type with no registered factory (logs a warning, does nothing), so right-
 * clicking a placed cargo tram did nothing visible on the client despite the server-side container
 * opening correctly. See {@code com.hbm.main.VehicleCargoClientRegistry} for the registration this
 * class is bound through.
 * <p>
 * <b>Texture note</b>: matches every other Phase 1/2 screen's documented convention (e.g.
 * {@link com.hbm.inventory.gui.CrateScreen}/{@link com.hbm.inventory.gui.BatteryScreen}) - this port
 * has no {@code assets/hbm/textures/**} tree yet, so this renders NeoForge's missing-texture
 * placeholder rather than crashing.
 */
public class TrainCargoTramScreen extends GuiInfoContainer<TrainCargoTramMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/train/gui_cargo_tram.png");

    public TrainCargoTramScreen(TrainCargoTramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // CE: 4x7 cargo grid (rows end at y=90) + one battery slot at (152,72), player inventory rows
        // starting at y=122, hotbar at y=180 - see TrainCargoTramMenu's own slot layout.
        this.imageWidth = 176;
        this.imageHeight = 206;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
