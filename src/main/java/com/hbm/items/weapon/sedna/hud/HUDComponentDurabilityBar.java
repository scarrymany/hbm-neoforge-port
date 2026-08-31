package com.hbm.items.weapon.sedna.hud;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Ported from CE's {@code com.hbm.items.weapon.sedna.hud.HUDComponentDurabilityBar} (52 lines,
 * full) - a small fixed-height fill bar showing gun wear/condition, always anchored at
 * {@code screenHeight - 21} regardless of any other stacked {@link IHUDComponent}'s
 * {@code bottomOffset} (CE's own real behavior - not a port bug, this bar simply never moves).
 * <p>
 * <b>Deliberate, documented addition beyond CE</b>: {@link #renderHUDComponent} guards against a
 * zero-or-negative {@code GunConfig#getDurability} before dividing (CE's own body divides
 * unconditionally, safe in CE only because every gun definition that attaches this component also
 * always calls {@code .dura(...)} with a positive value). This port's client-side default HUD-layout
 * fallback ({@code ItemGunBaseNT#renderHUD}, see its own javadoc) can attach this component to a gun
 * whose {@code GunConfig} has not had {@code .dura(...)} called yet (still {@code 0F}, the field's
 * Java default) - the guard avoids a divide-by-zero producing a garbage/NaN-derived bar width for
 * such a gun instead of the intended "always full" (zero wear against zero durability) reading.
 */
public class HUDComponentDurabilityBar implements IHUDComponent {

    private static final ResourceLocation MISC_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/misc/overlay_misc.png");

    protected final boolean mirrored;

    public HUDComponentDurabilityBar() {
        this(false);
    }

    public HUDComponentDurabilityBar(boolean mirror) {
        this.mirrored = mirror;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 5;
    }

    @Override
    public void renderHUDComponent(RenderGuiLayerEvent.Pre event, Player player, ItemStack stack, int bottomOffset, int gunIndex) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Window window = Minecraft.getInstance().getWindow();

        boolean offhandOccupied = !player.getOffhandItem().isEmpty();

        int pX = window.getGuiScaledWidth() / 2 + (mirrored ? -(62 + 36 + 52 + (offhandOccupied ? 29 : 0)) : (62 + 36));
        int pZ = window.getGuiScaledHeight() - 21;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        GunConfig config = gun.getConfig(stack, gunIndex);
        float maxDurability = config.getDurability(stack);
        int dura = maxDurability > 0F
                ? Math.round(50F * ItemGunBaseNT.getWear(stack, gunIndex) / maxDurability)
                : 0;
        dura = Math.max(0, Math.min(50, dura));

        guiGraphics.blit(MISC_TEXTURE, pX, pZ + 16, 94, 0, 52, 3);
        guiGraphics.blit(MISC_TEXTURE, pX + 1, pZ + 16, 95, 3, 50 - dura, 3);
    }
}
