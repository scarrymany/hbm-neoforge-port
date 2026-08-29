package com.hbm.inventory.fluid.trait;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FT_Toxin extends FluidTrait {

    public List<ToxinEntry> entries = new ArrayList<>();

    public FT_Toxin addEntry(ToxinEntry entry) {
        entries.add(entry);
        return this;
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("trait.toxin")).append("]").withStyle(ChatFormatting.LIGHT_PURPLE));

        for(ToxinEntry entry : entries) {
            entry.addInfo(info);
        }
    }

    public void affect(LivingEntity entity, double intensity) {

        for(ToxinEntry entry : entries) {
            entry.poison(entity, intensity);
        }
    }

    public abstract static class ToxinEntry {

        public HazardClass clazz;
        public boolean fullBody;

        public ToxinEntry(HazardClass clazz, boolean fullBody) {
            this.clazz = clazz;
            this.fullBody = fullBody;
        }

        public boolean isProtected(LivingEntity entity) {

            boolean hasMask = clazz == null;
            boolean hasSuit = !fullBody;

            if(clazz != null && ArmorRegistry.hasAllProtection(entity, EquipmentSlot.HEAD, clazz)) {
                ArmorUtil.damageGasMaskFilter(entity, 1);
                hasMask = true;
            }

            if(fullBody && ArmorUtil.checkForHazmat(entity)) {
                hasSuit = true;
            }

            return hasMask && hasSuit;
        }

        public abstract void poison(LivingEntity entity, double intensity);
        public abstract void addInfo(List<Component> info);
    }

    public static class ToxinDirectDamage extends ToxinEntry {

        public ResourceKey<DamageType> damageType;
        public float amount;
        public int delay;

        public ToxinDirectDamage(ResourceKey<DamageType> damageType, float amount, int delay, HazardClass clazz, boolean fullBody) {
            super(clazz, fullBody);
            this.damageType = damageType;
            this.amount = amount;
            this.delay = delay;
        }

        @Override
        public void poison(LivingEntity entity, double intensity) {

            if(isProtected(entity)) return;

            if(delay == 0 || entity.level().getGameTime() % delay == 0) {
                entity.hurt(entity.damageSources().source(damageType), (float) (amount * intensity));
            }
        }

        @Override
        public void addInfo(List<Component> info) {
            MutableComponent line = Component.literal("- ").append(Component.translatable(clazz.lang));

            if(fullBody) {
                line.append(Component.literal(" ").append(Component.translatable("trait.needhaz")).withStyle(ChatFormatting.RED));
            }

            line.append(": ").append(String.format(Locale.US, "%,.1f", amount * 20 / delay) + " DPS");
            info.add(line.withStyle(ChatFormatting.YELLOW));
        }
    }

    public static class ToxinEffects extends ToxinEntry {

        public List<MobEffectInstance> effects = new ArrayList<>();

        public ToxinEffects(HazardClass clazz, boolean fullBody) {
            super(clazz, fullBody);
        }

        public ToxinEffects add(MobEffectInstance... effs) {
            for(MobEffectInstance eff : effs) this.effects.add(eff);
            return this;
        }

        @Override
        public void poison(LivingEntity entity, double intensity) {

            if(isProtected(entity)) return;

            for(MobEffectInstance eff : effects) {
                entity.addEffect(new MobEffectInstance(eff.getEffect(), (int) (eff.getDuration() * intensity), eff.getAmplifier()));
            }
        }

        @Override
        public void addInfo(List<Component> info) {
            MutableComponent header = Component.literal("- ").append(Component.translatable(clazz.lang));

            if(fullBody) {
                header.append(Component.literal(" ").append(Component.translatable("trait.needhaz")).withStyle(ChatFormatting.RED));
            }

            header.append(":");
            info.add(header.withStyle(ChatFormatting.YELLOW));

            for(MobEffectInstance eff : effects) {
                MutableComponent line = Component.literal("   - ").append(Component.translatable(eff.getEffect().value().getDescriptionId()));

                if(eff.getAmplifier() > 0) line.append(" ").append(Component.translatable("potion.potency." + eff.getAmplifier()));

                line.append(" " + formatDuration(eff.getDuration()));
                info.add(line.withStyle(ChatFormatting.YELLOW));
            }
        }

        // Mirrors CE's StringUtils.ticksToElapsedTime(ticks): minutes:seconds, seconds zero-padded below 10.
        private static String formatDuration(int ticks) {
            int totalSeconds = ticks / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return seconds < 10 ? minutes + ":0" + seconds : minutes + ":" + seconds;
        }
    }

    @Override public void serializeJSON(JsonWriter writer) throws IOException {

        writer.name("entries").beginArray();

        for(ToxinEntry entry : entries) {
            writer.beginObject();

            if(entry instanceof ToxinDirectDamage e) {
                writer.name("type").value("directdamage");
                writer.name("amount").value(e.amount);
                writer.name("source").value(e.damageType.location().toString());
                writer.name("delay").value(e.delay);
                writer.name("hazmat").value(e.fullBody);
                writer.name("masktype").value(e.clazz.name());
            }
            if(entry instanceof ToxinEffects e) {
                writer.name("type").value("effects");
                writer.name("effects").beginArray();
                writer.setIndent("");
                for(MobEffectInstance effect : e.effects) {
                    writer.beginArray();
                    ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                    writer.value(id.toString()).value(effect.getDuration()).value(effect.getAmplifier()).value(effect.isAmbient());
                    writer.endArray();
                }
                writer.endArray();
                writer.setIndent("  ");
                writer.name("hazmat").value(e.fullBody);
                writer.name("masktype").value(e.clazz.name());
            }

            writer.endObject();
        }

        writer.endArray();
    }

    @Override public void deserializeJSON(JsonObject obj) {
        JsonArray array = obj.get("entries").getAsJsonArray();

        for(int i = 0; i < array.size(); i++) {
            JsonObject entry = array.get(i).getAsJsonObject();
            String name = entry.get("type").getAsString();

            if(name.equals("directdamage")) {
                ToxinDirectDamage e = new ToxinDirectDamage(
                        ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(entry.get("source").getAsString())),
                        entry.get("amount").getAsFloat(),
                        entry.get("delay").getAsInt(),
                        HazardClass.valueOf(entry.get("masktype").getAsString()),
                        entry.get("hazmat").getAsBoolean()
                );
                this.entries.add(e);
            }

            if(name.equals("effects")) {
                ToxinEffects e = new ToxinEffects(
                        HazardClass.valueOf(entry.get("masktype").getAsString()),
                        entry.get("hazmat").getAsBoolean()
                );
                JsonArray effects = entry.get("effects").getAsJsonArray();
                for(int j = 0; j < effects.size(); j++) {
                    JsonArray effect = effects.get(j).getAsJsonArray();
                    ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.parse(effect.get(0).getAsString()));
                    Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(key);
                    if(holder.isPresent()) {
                        MobEffectInstance instance = new MobEffectInstance(holder.get(),
                                effect.get(1).getAsInt(),
                                effect.get(2).getAsInt(),
                                effect.get(3).getAsBoolean(), true);
                        e.effects.add(instance);
                    }
                }
                this.entries.add(e);
            }
        }
    }
}
