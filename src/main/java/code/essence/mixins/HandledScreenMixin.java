package code.essence.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.events.container.HandledScreenEvent;
import code.essence.mixins.IScreen;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    public int backgroundWidth;
    @Shadow
    public int backgroundHeight;

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method = "render", at = @At("RETURN"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.callEvent(new HandledScreenEvent(context, focusedSlot, backgroundWidth, backgroundHeight, mouseX, mouseY, delta));
    }
    
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            IScreen iScreen = (IScreen) screen;
            for (net.minecraft.client.gui.Element element : iScreen.getChildren()) {
                if (element instanceof code.essence.display.widgets.ClearButtonWidget clearButton) {
                    if (clearButton.mouseClicked(mouseX, mouseY, button)) {
                        System.out.println("[ClearButton] Кнопка обработала клик в HandledScreenMixin!");
                        cir.setReturnValue(true);
                        cir.cancel();
                        return;
                    }
                }
            }
        }
    }
}
