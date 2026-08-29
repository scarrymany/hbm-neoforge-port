package com.hbm.inventory.fluid.trait;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

public class FT_Pheromone extends FluidTrait {

    public int type;

    // no-arg ctor required for reflective instantiation by the JSON trait-override loader (Fluids#readTraits)
    public FT_Pheromone() { }

    public FT_Pheromone(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Override
    public void addInfo(List<Component> info) {

        if(type == 1) {
            info.add(Component.literal("[").append(Component.translatable("trait.pherg")).append("]").withStyle(ChatFormatting.AQUA));
        } else {
            info.add(Component.literal("[").append(Component.translatable("trait.pherm")).append("]").withStyle(ChatFormatting.BLUE));
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("type").value(type);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        this.type = obj.get("type").getAsInt();
    }
}
