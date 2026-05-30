package code.essence.mixins;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.events.keyboard.KeyEvent;
import code.essence.display.screens.clickgui.MenuScreen;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (key != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {
            EventManager.callEvent(new KeyEvent(client.currentScreen, InputUtil.Type.KEYSYM, key, action));
        }
    }
}