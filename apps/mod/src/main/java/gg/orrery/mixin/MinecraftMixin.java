package gg.orrery.mixin;

import gg.orrery.eclipse.EclipseViews;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MinecraftClientMixin — the Eclipse interception spine (DESIGN_SPEC §6.3, ADR 0003 §4).
 *
 * <p>Injects at the HEAD of {@code MinecraftClient.setScreen(Screen)}. When the incoming
 * screen is a vanilla chest screen (a {@link ContainerScreen} — the UI every SkyBlock
 * menu uses) and is NOT already an Orrery screen, it asks Eclipse for an Orrery view. If
 * Eclipse owns the menu, the original {@code setScreen} call is cancelled and re-issued with
 * the Orrery screen instead.
 *
 * <h2>Why setScreen and not a HandledScreen ctor hook</h2>
 * The container-open packet causes the client to construct the vanilla {@code HandledScreen}
 * and call {@code setScreen} with it. Intercepting at {@code setScreen} lets us swap the
 * VISUAL screen while reusing the very same {@link ChestMenu} the vanilla
 * screen carries (same {@code syncId}) — so clicks routed through Eclipse hit the real server
 * slots and the open/close lifecycle stays vanilla-correct. We do NOT touch the handler or
 * any packet (§2 compliance — the swap is render-only).
 *
 * <h2>Recursion / fall-through guards</h2>
 * <ul>
 *   <li>If {@code screen} is already an Orrery screen, we do nothing (the re-issued
 *       {@code setScreen(orreryScreen)} re-enters this inject but is not a
 *       {@code GenericContainerScreen}, so it is ignored — belt-and-suspenders, we also skip
 *       anything that is not a vanilla {@code GenericContainerScreen}).</li>
 *   <li>Unrecognized menus -> {@link EclipseViews#orreryScreenFor} returns null -> we cancel
 *       nothing and vanilla proceeds untouched (degrade, never break — §6.5).</li>
 *   <li>Any exception is swallowed and we fall through to vanilla, so a bad parse can never
 *       break menu opening.</li>
 * </ul>
 *
 * <h2>Yarn 1.21.11 names</h2>
 * <ul>
 *   <li>{@code MinecraftClient.setScreen(Screen)} = method_1507</li>
 *   <li>{@code GenericContainerScreen} = class_476; extends {@code HandledScreen<GenericContainerScreenHandler>}</li>
 *   <li>{@code HandledScreen.getScreenHandler()} = method_17577 -> {@code GenericContainerScreenHandler}</li>
 *   <li>{@code Screen.getTitle()} = method_25440 -> {@code Text}</li>
 * </ul>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void orrery$interceptHandledScreen(Screen screen, CallbackInfo ci) {
        // Only intercept the vanilla chest UI SkyBlock menus use. Anything else (including our
        // own Orrery screen) falls through untouched — this also prevents re-interception.
        if (!(screen instanceof ContainerScreen)) {
            return;
        }

        try {
            ContainerScreen containerScreen = (ContainerScreen) screen;
            ChestMenu handler = containerScreen.getMenu();
            Component title = containerScreen.getTitle();

            // Ask Eclipse: does Orrery own this menu? Reuses the live handler (same syncId).
            Screen orreryScreen = EclipseViews.orreryScreenFor(handler, title);
            if (orreryScreen != null) {
                // Cancel the vanilla screen and open the Orrery one in its place.
                ci.cancel();
                Minecraft.getInstance().setScreen(orreryScreen);
            }
            // else: not recognized -> do nothing -> vanilla screen opens normally.
        } catch (Throwable t) {
            // Degrade to vanilla; never break menu opening because of an Orrery error (§6.5).
        }
    }
}
