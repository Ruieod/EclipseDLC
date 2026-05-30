package code.essence.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.events.keyboard.GuiScrollEvent;
import code.essence.events.keyboard.HotBarScrollEvent;
import code.essence.events.keyboard.KeyEvent;
import code.essence.events.keyboard.MouseRotationEvent;
import code.essence.events.render.FovEvent;

@Mixin(Mouse.class)
public class MouseMixin {
    @Final @Shadow private MinecraftClient client;
    @Shadow public double cursorDeltaX, cursorDeltaY;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    public void onMouseButtonHook(long window, int button, int action, int mods, CallbackInfo ci) {
        if (button != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {
            EventManager.callEvent(new KeyEvent(client.currentScreen, InputUtil.Type.MOUSE, button, action));
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    public void onGuiScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (client.currentScreen != null) {
            double mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight();
            GuiScrollEvent guiEvent = new GuiScrollEvent(mouseX, mouseY, horizontal, vertical);
            EventManager.callEvent(guiEvent);
        }
    }

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        HotBarScrollEvent event = new HotBarScrollEvent(horizontal, vertical);
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "updateMouse", at = @At(value = "HEAD"))
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        FovEvent event = new FovEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            double slowdown = (double) event.getFov() / client.options.getFov().getValue();
            this.cursorDeltaX *= slowdown;
            this.cursorDeltaY *= slowdown;
        }
    }

    @WrapWithCondition(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 1, allow = 1)
    private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
        MouseRotationEvent event = new MouseRotationEvent((float) cursorDeltaX, (float) cursorDeltaY);
        EventManager.callEvent(event);
        if (event.isCancelled()) return false;
        instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
        return false;
    }

}
