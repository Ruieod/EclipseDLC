package code.essence.mixins;

import code.essence.display.screens.clickgui.components.implement.autobuy.manager.AutoBuyManager;
import code.essence.utils.client.Instance;
import code.essence.utils.client.logs.Logger;
import code.essence.utils.client.packet.network.Network;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> {
    private ButtonWidget autoBuyButton;
    private ButtonWidget autoParserButton;
    private SliderWidget autoParserSlider;
    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    public GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructor(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {

    }

    private static boolean isAuctionTitle(String title) {
        if (title == null) return false;
        return title.contains("Аукцион") || title.contains("Аукционы") || title.contains("Поиск");
    }
}