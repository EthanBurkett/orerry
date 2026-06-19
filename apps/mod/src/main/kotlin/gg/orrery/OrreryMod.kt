package gg.orrery

import gg.orrery.atlas.Atlas
import gg.orrery.dev.DevPreview
import gg.orrery.eclipse.Eclipse
import gg.orrery.generated.Tokens
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager

/**
 * Orrery mod entrypoint — client side only (§6.1, §2).
 *
 * Phase 0: boots and prints a startup marker proving the mod loaded and the
 * design-token pipeline (Tokens.kt) is linked in. No packets, no gameplay
 * actions — §2 compliance is trivially satisfied here.
 */
@Environment(EnvType.CLIENT)
object OrreryMod : ClientModInitializer {

    private val logger = LogManager.getLogger("Orrery")

    const val MOD_ID = "orrery"

    override fun onInitializeClient() {
        // Phase-0 marker — visible in the log when the mod loads successfully.
        // References Tokens to prove the design-token pipeline links into the mod (§5.5).
        logger.info(
            "[Orrery] booted — brass instrument in the void · MC 26.1.2" +
                " · primary accent #${Integer.toHexString(Tokens.Color.brassBase).takeLast(6).uppercase()}" +
                " · motion.base ${Tokens.Motion.base}ms"
        )

        // Eclipse spine (Phase 1): register Atlas recognizers + Eclipse menu views so the
        // interception mixin can recognize and swap the SkyBlock main menu at runtime (§6.3).
        Atlas.registerDefaultRecognizers()
        Eclipse.registerDefaults()

        logger.info("[Orrery] subsystems registered — Eclipse spine live (Atlas recognizers + Eclipse views)")

        // Dev-only: offline menu preview harness. Never registered in a production build.
        // Run `/orrery preview` in a singleplayer world to see the SkyBlock menu intercepted
        // and re-rendered without a Mojang session / Hypixel (see DevPreview).
        if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            DevPreview.register()
            logger.info("[Orrery] dev preview enabled — run /orrery preview in a world")
        }
    }
}
