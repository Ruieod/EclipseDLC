package code.essence.features.impl.misc;

import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.math.time.TimerUtil;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoDuel extends Module implements QuickImports {


    SelectSetting kits = new SelectSetting("Разрешенные киты", "auto_duel.kits.desc")
            .value(
                    "Щиты",
                    "Шипы 3",
                    "Лук",
                    "Тотемы",
                    "НоДебафф",
                    "Шары",
                    "Классик",
                    "Читерский рай",
                    "Без эндер-жемчуга"
            )
            .selected("Шары");

    List<String> sent = new ArrayList<>();
    final TimerUtil duelDelay = TimerUtil.create();
    final TimerUtil clearSentDelay = TimerUtil.create();
    final TimerUtil kitChoiceDelay = TimerUtil.create();
    final TimerUtil duelSetupDelay = TimerUtil.create();
    static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    public AutoDuel() {
        super("AutoDuel", ModuleCategory.MISC);
        setup(kits);
    }

    @Override
    public void activate() {
        sent.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        List<String> players = getOnlinePlayers();

        if (clearSentDelay.hasTimeElapsed(800L * Math.max(players.size(), 1))) {
            sent.clear();
            clearSentDelay.resetCounter();
        }

        for (String player : players) {
            if (sent.contains(player) || player.equals(mc.player.getGameProfile().getName())) continue;
            if (duelDelay.hasTimeElapsed(1000L)) {
                mc.player.networkHandler.sendChatMessage("/duel " + player);
                sent.add(player);
                duelDelay.resetCounter();
            }
            break;
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();
            if (title.contains("Выбор набора (1/1)") || title.contains("Kit selection")) {
                handleKitSelection();
            } else if (title.contains("Настройка поединка") || title.contains("Duel setup")) {
                handleDuelSetup();
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String text = packet.content().getString().toLowerCase();
            if ((text.contains("начало") && text.contains("через") && text.contains("секунд")) ||
                    text.contains("дуэли » во время поединка запрещено использовать команды") ||
                    (text.contains("duel") && text.contains("during") && text.contains("forbidden"))) {
                if (isState()) switchState();
            }
        }
    }

    private List<String> getOnlinePlayers() {
        return mc.getNetworkHandler().getPlayerList().stream()
                .map(entry -> entry.getProfile().getName())
                .filter(name -> NAME_PATTERN.matcher(name).matches())
                .collect(Collectors.toList());
    }

    private void handleKitSelection() {
        List<String> list = kits.getList();
        if (list == null || list.isEmpty()) return;

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (kits.isSelected(list.get(i))) slots.add(i);
        }
        if (slots.isEmpty()) return;

        Collections.shuffle(slots);
        int slotId = slots.get(0);

        if (kitChoiceDelay.hasTimeElapsed(90)) {
            InventoryTask.clickSlot(slotId, 0, SlotActionType.QUICK_MOVE, false);
            kitChoiceDelay.resetCounter();
        }
    }

    private void handleDuelSetup() {
        if (duelSetupDelay.hasTimeElapsed(90)) {
            InventoryTask.clickSlot(0, 0, SlotActionType.QUICK_MOVE, false);
            duelSetupDelay.resetCounter();
        }
    }
}
