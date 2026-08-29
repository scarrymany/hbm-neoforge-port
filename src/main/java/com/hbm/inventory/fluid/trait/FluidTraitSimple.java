package com.hbm.inventory.fluid.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FluidTraitSimple {

    public static class FT_Gaseous extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.gaseous")).append("]").withStyle(ChatFormatting.BLUE));
        }
    }

    /** gaseous at room temperature, for cryogenic hydrogen for example */
    public static class FT_Gaseous_ART extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.gaseousroom")).append("]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_Liquid extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.liquid")).append("]").withStyle(ChatFormatting.BLUE));
        }
    }

    /** too viscous to be sprayed/turned into a mist */
    public static class FT_Viscous extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.viscous")).append("]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_Plasma extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.plasma")).append("]").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static class FT_Amat extends FluidTrait {
        @Override public void addInfo(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.antimatterliq")).append("]").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static class FT_LeadContainer extends FluidTrait {
        @Override public void addInfo(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.haztank")).append("]").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static class FT_Delicious extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.delicious")).append("]").withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    public static class FT_Unsiphonable extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[").append(Component.translatable("trait.nosiphon")).append("]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_NoID extends FluidTrait { }
    public static class FT_NoContainer extends FluidTrait { }
}
