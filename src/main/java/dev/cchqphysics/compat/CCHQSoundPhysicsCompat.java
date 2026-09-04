package dev.cchqphysics.compat;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.ConfigScreenFactory;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
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
    public static final String VERSION = "0.1.0-beta11-phase5-test";
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    public CCHQSoundPhysicsCompat(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "cchq_soundphysics_compat-client.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ExtendedClientConfig.SPEC, "cchq_soundphysics_compat-advanced.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigScreenFactory::create);
        LOGGER.info("CC:HQ Sound Physics Compat {} initialized; Phase 4 parity defaults preserved; Phase 5 advanced/debug controls available", VERSION);
    }
}
