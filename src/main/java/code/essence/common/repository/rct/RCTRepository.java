package code.essence.common.repository.rct;

import code.essence.utils.client.sound.SoundManager;
import code.essence.utils.interactions.inv.InventoryTask;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.interfaces.QuickLogger;
import code.essence.utils.client.packet.network.Network;
import code.essence.utils.math.time.StopWatch;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;
import code.essence.display.hud.Notifications;

public class RCTRepository implements QuickImports, QuickLogger {
    private final StopWatch stopWatch = new StopWatch();
    private final StopWatch hubDelayWatch = new StopWatch();
    private boolean lobby;
    private int anarchy;
    private boolean rwReconnecting;
    private boolean hubSent;
    private boolean anSent;
    private int anarchyBeforeHub;
    private boolean rwLeftWorld;
    private long rwStartTime;

    public RCTRepository(EventManager eventManager) {eventManager.register(this);}

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (anarchy != 0 && e.getPacket() instanceof GameMessageS2CPacket message) {
            String text = message.content().getString().toLowerCase();
            if (!text.contains("хаб") && text.contains("не удалось")) {
                Notifications.getInstance().addList("[RCT] На данную анархию " + Formatting.RED + "нельзя" + Formatting.RESET + " зайти", 3000);
                anarchy = 0;
                rwReconnecting = false;
            }
        }
    }


    @EventHandler
    public void onTick(TickEvent e) {
        if (anarchy == 0) return;

        if (Network.isReallyWorld()) {
            tickReallyWorld();
        } else if (Network.isSpookyTime() || Network.isFunTime()) {
            tickCopyTime();
        } else if (anarchy > 0 && (hubSent || rwReconnecting)) {
            tickCopyTime();
        } else {
            anarchy = 0;
            rwReconnecting = false;
            hubSent = false;
            anSent = false;
            anarchyBeforeHub = -1;
            hubDelayWatch.reset();
        }
    }

    private void tickReallyWorld() {
        int currentAnarchy = Network.getAnarchy();

        
        
        if (!rwReconnecting) {
            rwReconnecting = true;
            hubSent = false;
            rwLeftWorld = false;
            anarchyBeforeHub = currentAnarchy;
            rwStartTime = System.currentTimeMillis();
            hubDelayWatch.reset();
        }

        
        if (System.currentTimeMillis() - rwStartTime > 25000) {
            Notifications.getInstance().addList("[RCT] Таймаут — не удалось перезайти на ГРИФ #" + Formatting.BLUE + anarchy + Formatting.RESET, 3500);
            anarchy = 0;
            rwReconnecting = false;
            hubSent = false;
            rwLeftWorld = false;
            anarchyBeforeHub = -1;
            return;
        }

        if (!hubSent) {
            mc.player.networkHandler.sendChatCommand("hub");
            hubSent = true;
            hubDelayWatch.reset();
            return;
        }

        
        int nowAnarchy = Network.getAnarchy();
        if (!rwLeftWorld && nowAnarchy != anarchyBeforeHub) {
            rwLeftWorld = true;
        }

        if (!rwLeftWorld) {
            return;
        }

        
        if (nowAnarchy == anarchy) {
            int completed = anarchy;
            SoundManager.playSound(SoundManager.ENABLE_MODULE);
            Notifications.getInstance().addList("Перезаход на ГРИФ #" + Formatting.BLUE + completed + Formatting.RESET + " выполнен", 3000);

            anarchy = 0;
            rwReconnecting = false;
            hubSent = false;
            rwLeftWorld = false;
            anarchyBeforeHub = -1;
            return;
        }

        
        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();

            if (title.contains("Выбор сервера")) {
                for (int i = 0; i < screen.getScreenHandler().getInventory().size(); i++) {
                    var stack = screen.getScreenHandler().getInventory().getStack(i);
                    String name = stack.getName().getString();
                    if (name.contains("ГРИФЕРСКОЕ ВЫЖИВАНИЕ") && stack.getItem() == Blocks.CRAFTING_TABLE.asItem()) {
                        InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                        return;
                    }
                }
            } else if (title.contains("Выбор мира грифа")) {
                for (int i = 0; i < screen.getScreenHandler().getInventory().size(); i++) {
                    var stack = screen.getScreenHandler().getInventory().getStack(i);
                    String name = stack.getName().getString();
                    if (name.contains("ГРИФ #" + anarchy) && stack.getItem() == Items.PLAYER_HEAD) {
                        InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                        return;
                    }
                }
            }
        } else if (stopWatch.every(500)) {
            
            InventoryTask.selectCompass();
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
        }
    }

    private void tickCopyTime() {
        int currentAnarchy = Network.getAnarchy();
        long elapsed = hubSent ? hubDelayWatch.elapsedTime() : 0;
        String serverTitle = mc.world != null && mc.world.getScoreboard() != null ? 
            (mc.world.getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR) != null ?
                mc.world.getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR).getDisplayName().getString() : "null") : "null";

        if (currentAnarchy == anarchy && anarchy > 0 && rwReconnecting && anSent && serverTitle.contains("Анархия-")) {
            int completedAnarchy = anarchy;
            SoundManager.playSound(SoundManager.ENABLE_MODULE);
            Notifications.getInstance().addList("Перезаход на анархию #" + Formatting.BLUE + completedAnarchy + Formatting.RESET + " выполнен", 3000);
            anarchy = 0;
            rwReconnecting = false;
            hubSent = false;
            anSent = false;
            anarchyBeforeHub = -1;
            hubDelayWatch.reset();
            return;
        }

        if (currentAnarchy == anarchy && anarchy > 0 && !rwReconnecting) {
            rwReconnecting = true;
            hubSent = false;
            anSent = false;
            hubDelayWatch.reset();
        }

        if (rwReconnecting) {
            if (!hubSent) {
                anarchyBeforeHub = currentAnarchy;
                mc.player.networkHandler.sendChatMessage("/hub");
                hubSent = true;
                anSent = false;
                hubDelayWatch.reset();
            } else if (!anSent) {
                elapsed = hubDelayWatch.elapsedTime();
                boolean worldChanged = (currentAnarchy != anarchyBeforeHub);
                
                if (elapsed >= 890 && worldChanged) {
                    mc.player.networkHandler.sendChatMessage("/an" + anarchy);
                    anSent = true;
                }
            }
        }
    }

    public void reconnect(int anarchy) {
        if (Network.isReallyWorld()) {
            if (anarchy > 0) {
                this.anarchy = anarchy;
                this.rwReconnecting = false;
                this.hubSent = false;
                this.rwLeftWorld = false;
                this.anarchyBeforeHub = -1;
                this.rwStartTime = 0;
            } else {
                Notifications.getInstance().addList("[RCT] Неверный " + Formatting.RED + "гриф", 3000);
            }
        } else if (Network.isSpookyTime() || Network.isFunTime()) {
            if (anarchy > 0) {
                this.anarchy = anarchy;
                this.rwReconnecting = false;
                this.hubSent = false;
                this.hubDelayWatch.reset();
            } else {
                int currentAnarchy = Network.getAnarchy();
                if (currentAnarchy > 0) {
                    this.anarchy = currentAnarchy;
                    this.rwReconnecting = false;
                    this.hubSent = false;
                    this.hubDelayWatch.reset();
                } else {
                    Notifications.getInstance().addList("[RCT] Неверный номер " + Formatting.RED + "анархии", 3000);
                }
            }
        }
    }
}
