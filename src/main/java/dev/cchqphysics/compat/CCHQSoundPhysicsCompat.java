package dev.cchqphysics.compat;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.ConfigScreenFactory;
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
    public static final String VERSION = "0.1.0-beta11";
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    public CCHQSoundPhysicsCompat(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "cchq_soundphysics_compat-client.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigScreenFactory::create);
        LOGGER.info("CC:HQ Sound Physics Compat 0.1.0-beta11 initialized; beta9 acoustics preserved; beta10 exact direct-ray reuse retained; beta11 exact static room/bounce-ray memoization + batched mono decode + OpenAL alSourcePlayv group sync enabled", VERSION);
    }
}
