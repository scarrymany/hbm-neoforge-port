package com.hbm.inventory.gui.turret;

import com.hbm.blockentity.turret.TurretBaseBlockEntity;
import com.hbm.packet.toserver.TurretControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ported from CE's {@code GUITurretMobFilter} - a bare {@link Screen} with no backing
 * {@link net.minecraft.world.inventory.AbstractContainerMenu} at all (CE's own screen has none
 * either - it mutates the target turret purely via a raw NBT packet), same as vanilla's own
 * sign-edit screen precedent. Mutations go through {@link TurretControlPacket} (see this class's
 * own javadoc for why {@code ItemControlPacket} doesn't apply here).
 * <p>
 * <b>Simplified from CE's custom {@code GUIScrollingList}/texture-driven layout</b>: this port has
 * no equivalent scrolling-list widget class, so this screen uses plain vanilla primitives (an
 * {@link EditBox} search field and two manually-rendered, click-and-scroll row lists) instead - see
 * {@code docs/phase3/turret_system.md}'s open question #5, which explicitly flagged this screen's
 * living-entity enumeration as unconfirmed and left to whoever implements it. The mob-name universe
 * is built from {@link BuiltInRegistries#ENTITY_TYPE} filtered to
 * {@link MobCategory#MISC MobCategory != MISC} (the closest broad "living creature" approximation
 * without a direct {@code EntityLiving.class.isAssignableFrom} equivalent), sorted by translated
 * name - a reasonable, documented approximation of CE's own
 * {@code EntityLiving.class.isAssignableFrom(...)} filter, not a byte-for-byte match.
 */
public class TurretMobFilterScreen extends Screen {

    private static final int ROW_HEIGHT = 12;
    private static final int VISIBLE_ROWS = 8;

    private final BlockPos turretPos;

    private record MobEntry(ResourceLocation id, String displayName) {
    }

    private final List<MobEntry> allMobs;
    private List<MobEntry> filteredMobs;

    private EditBox searchBox;
    private int mobScroll = 0;
    private int filterScroll = 0;
    private int selectedMobIndex = -1;
    private int selectedFilterIndex = -1;

    private int guiLeft;
    private int guiTop;
    private static final int WIDTH = 260;
    private static final int HEIGHT = 160;

    public TurretMobFilterScreen(BlockPos turretPos) {
        super(Component.translatable("gui.turretMobFilter"));
        this.turretPos = turretPos;

        this.allMobs = BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .filter(e -> e.getValue().getCategory() != MobCategory.MISC)
                .map(e -> new MobEntry(e.getKey().location(), stripFormatting(e.getValue().getDescription())))
                .sorted((a, b) -> a.displayName.compareToIgnoreCase(b.displayName))
                .collect(Collectors.toList());
        this.filteredMobs = new ArrayList<>(allMobs);
    }

    private static String stripFormatting(Component component) {
        return component.getString();
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - WIDTH) / 2;
        this.guiTop = (this.height - HEIGHT) / 2;

        this.searchBox = new EditBox(this.font, guiLeft + 8, guiTop + 16, 110, 14, Component.literal("Search"));
        this.searchBox.setResponder(this::updateSearch);
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("Add ->"), b -> addSelected())
                .bounds(guiLeft + 8, guiTop + 32 + VISIBLE_ROWS * ROW_HEIGHT + 4, 60, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("<- Remove"), b -> removeSelected())
                .bounds(guiLeft + WIDTH - 68, guiTop + 32 + VISIBLE_ROWS * ROW_HEIGHT + 4, 60, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Black/Whitelist"), b -> toggleBlacklist())
                .bounds(guiLeft + WIDTH / 2 - 50, guiTop + 32 + VISIBLE_ROWS * ROW_HEIGHT + 4, 100, 16).build());
    }

    private void updateSearch(String query) {
        String needle = query.toLowerCase();
        filteredMobs = allMobs.stream()
                .filter(m -> m.id.toString().toLowerCase().contains(needle) || m.displayName.toLowerCase().contains(needle))
                .collect(Collectors.toList());
        selectedMobIndex = -1;
        mobScroll = 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fill(guiLeft, guiTop, guiLeft + WIDTH, guiTop + HEIGHT, 0xC0101010);

        List<String> filter = getTurretMobFilter();

        int listY = guiTop + 34;
        renderList(guiGraphics, mouseX, mouseY, guiLeft + 8, listY, filteredMobs.stream().map(MobEntry::displayName).toList(), mobScroll, selectedMobIndex);
        renderList(guiGraphics, mouseX, mouseY, guiLeft + WIDTH - 118, listY, filter, filterScroll, selectedFilterIndex);

        guiGraphics.drawString(this.font, "All entities", guiLeft + 8, guiTop + 4, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Current filter", guiLeft + WIDTH - 118, guiTop + 4, 0xFFFFFF);

        TurretBaseBlockEntity turret = getTurret();
        String mode = turret != null && turret.isBlacklistMobFilter ? "Mode: Blacklist" : "Mode: Whitelist";
        guiGraphics.drawString(this.font, Component.literal(mode).withStyle(ChatFormatting.YELLOW), guiLeft + WIDTH / 2 - 40, guiTop + HEIGHT - 16, 0xFFFFFF, false);
    }

    private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, List<String> entries, int scroll, int selected) {
        guiGraphics.fill(x, y, x + 110, y + VISIBLE_ROWS * ROW_HEIGHT, 0x60000000);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= entries.size()) break;

            int rowY = y + row * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + 110 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (index == selected) {
                guiGraphics.fill(x, rowY, x + 110, rowY + ROW_HEIGHT, 0x8000AAFF);
            } else if (hovered) {
                guiGraphics.fill(x, rowY, x + 110, rowY + ROW_HEIGHT, 0x40FFFFFF);
            }

            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(entries.get(index), 106), x + 2, rowY + 2, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listY = guiTop + 34;

        if (mouseX >= guiLeft + 8 && mouseX < guiLeft + 118 && mouseY >= listY && mouseY < listY + VISIBLE_ROWS * ROW_HEIGHT) {
            int index = mobScroll + (int) ((mouseY - listY) / ROW_HEIGHT);
            if (index < filteredMobs.size()) selectedMobIndex = index;
            return true;
        }

        if (mouseX >= guiLeft + WIDTH - 118 && mouseX < guiLeft + WIDTH - 8 && mouseY >= listY && mouseY < listY + VISIBLE_ROWS * ROW_HEIGHT) {
            List<String> filter = getTurretMobFilter();
            int index = filterScroll + (int) ((mouseY - listY) / ROW_HEIGHT);
            if (index < filter.size()) selectedFilterIndex = index;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < guiLeft + WIDTH / 2D) {
            mobScroll = clampScroll(mobScroll - (int) Math.signum(scrollY), filteredMobs.size());
        } else {
            filterScroll = clampScroll(filterScroll - (int) Math.signum(scrollY), getTurretMobFilter().size());
        }
        return true;
    }

    private int clampScroll(int scroll, int size) {
        int max = Math.max(0, size - VISIBLE_ROWS);
        return Math.max(0, Math.min(scroll, max));
    }

    private void addSelected() {
        if (selectedMobIndex < 0 || selectedMobIndex >= filteredMobs.size()) return;

        CompoundTag data = new CompoundTag();
        data.putString("addMobFilter", filteredMobs.get(selectedMobIndex).id.toString());
        PacketDistributor.sendToServer(new TurretControlPacket(turretPos, data));
    }

    private void removeSelected() {
        List<String> filter = getTurretMobFilter();
        if (selectedFilterIndex < 0 || selectedFilterIndex >= filter.size()) return;

        CompoundTag data = new CompoundTag();
        data.putString("removeMobFilter", filter.get(selectedFilterIndex));
        PacketDistributor.sendToServer(new TurretControlPacket(turretPos, data));
        selectedFilterIndex = -1;
    }

    private void toggleBlacklist() {
        TurretBaseBlockEntity turret = getTurret();
        if (turret == null) return;
        // Reuses the shared toggle-button dispatch (meta 6) via a plain button-click packet path
        // would need a Menu; this screen has none, so it goes through the same NBT control path -
        // CE's own GUITurretMobFilter does the same (AuxButtonPacket meta 6 in CE, an equally
        // NBT-adjacent raw packet in that version's own networking stack).
        CompoundTag data = new CompoundTag();
        data.putBoolean("toggleBlacklist", !turret.isBlacklistMobFilter);
        PacketDistributor.sendToServer(new TurretControlPacket(turretPos, data));
    }

    private List<String> getTurretMobFilter() {
        TurretBaseBlockEntity turret = getTurret();
        return turret != null ? turret.mobFilter : List.of();
    }

    private TurretBaseBlockEntity getTurret() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return level.getBlockEntity(turretPos) instanceof TurretBaseBlockEntity turret ? turret : null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
