package dev.cchqphysics.compat;

import dev.cchqphysics.compat.audio.DebugCommands;
import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.ConfigScreenFactory;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
import dev.cchqphysics.compat.config.SpectralMixConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = CCHQSoundPhysicsCompat.MOD_ID, dist = Dist.CLIENT)
public final class CCHQSoundPhysicsCompat {
    public static final String MOD_ID = "cchq_soundphysics_compat";
    public static final String VERSION = "0.1.0-beta11-phase5-sync-hf50-config-test";
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    public CCHQSoundPhysicsCompat(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "cchq_soundphysics_compat-client.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ExtendedClientConfig.SPEC, "cchq_soundphysics_compat-advanced.toml");
        // Keep the approved HF50 config filename so existing validated values carry forward.
        container.registerConfig(ModConfig.Type.CLIENT, SpectralMixConfig.SPEC, "cchq_soundphysics_compat-sync-hf50.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigScreenFactory::create);
        DebugCommands.init();
        LOGGER.info("CC:HQ Sound Physics Compat {} initialized; configurable guarded synchronized HF50 candidate enabled", VERSION);
        if (ExtendedClientConfig.logConfig()) {
            LOGGER.info("Phase 5 advanced config: {} {}", ExtendedClientConfig.summary(), SpectralMixConfig.summary());
        }
    }
}
