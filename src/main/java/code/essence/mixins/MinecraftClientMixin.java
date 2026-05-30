package code.essence.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.Essence;
import code.essence.utils.client.managers.file.exception.FileProcessingException;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.client.logs.Logger;
import code.essence.events.container.SetScreenEvent;
import code.essence.events.player.HotBarUpdateEvent;
import code.essence.features.impl.combat.NoInteract;
import code.essence.utils.client.window.WindowStyle;
import code.essence.utils.client.window.WindowTitleAnimation;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements QuickImports {
    @Shadow @Nullable public abstract ClientPlayNetworkHandler getNetworkHandler();
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Final public GameRenderer gameRenderer;
    @Shadow @Nullable public Screen currentScreen;
    private WindowTitleAnimation titleUtil;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onInit(RunArgs args, CallbackInfo ci) {
        if (System.getProperty("java.version").equals("sk3dguard-production-vm")) {
            System.setProperty("sun.font.layout.ffm", "false");
        }
        Fonts.init();
        try {
            titleUtil = WindowTitleAnimation.getInstance();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                client.getWindow().setTitle(titleUtil.getCurrentTitle());
            }
        } catch (Exception e) {
            
        }
    }

    @Inject(at = @At("HEAD"), method = "stop")
    private void stop(CallbackInfo ci) {
        Logger.info("Stopping for MinecraftClient");
        try {
            Essence essence = Essence.getInstance();
            if (essence != null && essence.isInitialized()) {
                try {
                    essence.getFileController().saveFiles();
                } catch (FileProcessingException e) {
                    Logger.error("Error occurred while saving files: " + e.getMessage() + " " + e.getCause());
                } finally {
                    if (essence.getFileController() != null) {
                        essence.getFileController().stopAutoSave();
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"), cancellable = true)
    public void doItemUseHook(CallbackInfo ci) {
        NoInteract noInteract = NoInteract.getInstance();
        if (noInteract != null && noInteract.isState() && player != null && interactionManager != null) {
            for (Hand hand : Hand.values()) {
                if (player.getStackInHand(hand).isEmpty()) continue;
                ActionResult result = interactionManager.interactItem(player, hand);
                if (result.isAccepted()) {
                    if (result instanceof ActionResult.Success success && success.swingSource().equals(ActionResult.SwingSource.CLIENT)) {
                        gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                        player.swingHand(hand);
                    }
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "setScreen", at = @At(value = "HEAD"), cancellable = true)
    public void setScreenHook(Screen screen, CallbackInfo ci) {
        try {
            SetScreenEvent event = new SetScreenEvent(screen);
            EventManager.callEvent(event);
            Essence essence = Essence.getInstance();
            if (essence != null && essence.getDraggableRepository() != null) {
                essence.getDraggableRepository().draggable().forEach(drag -> drag.setScreen(event));
            }
            Screen eventScreen = event.getScreen();
            if (screen != eventScreen) {
                mc.setScreen(eventScreen);
                ci.cancel();
            }
        } catch (Exception e) {
            
        }
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void applyDarkMode(CallbackInfo ci) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                WindowStyle.setDarkMode(client.getWindow().getHandle());
            }
        } catch (Exception e) {
            
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        try {
            if (titleUtil == null) {
                titleUtil = WindowTitleAnimation.getInstance();
            }
            titleUtil.updateTitle();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                client.getWindow().setTitle(titleUtil.getCurrentTitle());
            }
        } catch (Exception e) {
            
        }
    }

    @Inject(method = "updateWindowTitle", at = @At("HEAD"), cancellable = true)
    private void onUpdateWindowTitle(CallbackInfo ci) {
        try {
            if (titleUtil == null) {
                titleUtil = WindowTitleAnimation.getInstance();
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                client.getWindow().setTitle(titleUtil.getCurrentTitle());
            }
            ci.cancel();
        } catch (Exception e) {
            
        }
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    public void handleInputEventsHook(CallbackInfo ci) {
        HotBarUpdateEvent event = new HotBarUpdateEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }
}