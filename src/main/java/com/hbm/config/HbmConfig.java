package com.hbm.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Aggregates every {@code com.hbm.config} category class into the three
 * {@link ModConfigSpec}s NeoForge expects (COMMON, SERVER, CLIENT) and registers them.
 * <p>
 * Mirrors CE's split more closely than a single monolithic file: CE's boot-time
 * {@code Configuration}-backed classes (general/nukes/mobs/radiation/etc.) all become COMMON,
 * CE's runtime-editable {@code ClientConfig} (client-only HUD/rendering prefs) becomes CLIENT,
 * and CE's runtime-editable {@code ServerConfig} (server-authoritative balance values, synced to
 * clients) becomes SERVER - see {@link com.hbm.config.ClientConfig} and
 * {@link com.hbm.config.ServerConfig} for the feature-reduction note on dropping CE's
 * {@code /ntmclient}/{@code /ntmserver} live-edit commands.
 * <p>
 * <b>Integration:</b> this class does not wire itself up. Call {@link #register(ModContainer)}
 * once from the mod's constructor (the {@code ModContainer} passed there), e.g. from
 * {@code com.hbm.main.MainRegistry}'s constructor: {@code HbmConfig.register(modContainer);}
 */
public final class HbmConfig {

    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        GeneralConfig.init(commonBuilder);
        BombConfig.init(commonBuilder);
        CompatibilityConfig.init(commonBuilder);
        MachineConfig.init(commonBuilder);
        MobConfig.init(commonBuilder);
        PotionConfig.init(commonBuilder);
        RadiationConfig.init(commonBuilder);
        StructureConfig.init(commonBuilder);
        ToolConfig.init(commonBuilder);
        WeaponConfig.init(commonBuilder);
        WorldConfig.init(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        ServerConfig.init(serverBuilder);
        SERVER_SPEC = serverBuilder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        ClientConfig.init(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private HbmConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
