package com.hbm.items.datagen;

import com.hbm.items.ICustomItemModelRegister;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Objects;

/**
 * Generates one item model per entry actually present in {@link ModItems#ITEMS} at datagen time -
 * never a hardcoded id list, so this class needs zero coordination with whichever area registers a
 * given item. Every item defaults to {@code basicItem(...)} (single {@code item/generated} layer,
 * covering the overwhelming majority of Phase 1's flat 2D resource items); an item class that needs
 * anything else (multi-layer textures, a {@code builtin/entity} renderer, property overrides) opts
 * out of the default by implementing {@link ICustomItemModelRegister} and doing its own registration.
 */
public class ModItemModelProvider extends ItemModelProvider {

    private final ExistingFileHelper files;

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MainRegistry.MODID, existingFileHelper);
        this.files = existingFileHelper;
    }

    @Override
    protected void registerModels() {
        ModItems.ITEMS.getEntries().forEach(holder -> {
            Item item = holder.get();
            ResourceLocation loc = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));

            try {
                if (item instanceof ICustomItemModelRegister custom) {
                    custom.registerItemModel(this, loc);
                } else if (hasExistingItemModel(loc)) {
                    // Phase 10 CE-converted models/item — don't emit a cube that shadows them.
                } else if (hasItemTexture(loc)) {
                    this.basicItem(item);
                }
            } catch (IllegalArgumentException missing) {
                // Phase 10 owns bulk assets; missing textures must not fail datagen.
            }
        });
    }

    private boolean hasItemTexture(ResourceLocation loc) {
        return files.exists(loc, PackType.CLIENT_RESOURCES, ".png", "textures/item");
    }

    private boolean hasExistingItemModel(ResourceLocation loc) {
        return files.exists(loc, PackType.CLIENT_RESOURCES, ".json", "models/item");
    }
}
