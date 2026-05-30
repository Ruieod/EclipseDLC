package code.essence.features.impl.misc;

import code.essence.display.hud.Notifications;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.player.TickEvent;
import code.essence.utils.client.packet.network.Network;
import code.essence.utils.client.sound.SoundManager;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.math.time.StopWatch;
import code.essence.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.apache.commons.lang3.StringUtils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HolyWorldAutoJoin implements QuickImports {
    @NonFinal
    int targetAnarchy = -1;
    @NonFinal
    boolean isJoining = false;
    @NonFinal
    StopWatch hubDelayWatch = new StopWatch();
    @NonFinal
    StopWatch actionDelayWatch = new StopWatch();
    @NonFinal
    int currentStep = 0;
    @NonFinal
    boolean completed = false;
    @NonFinal
    boolean active = false;

    public HolyWorldAutoJoin() {
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!active) {
            if (isJoining || completed) {
                reset();
            }
            return;
        }

        if (!Network.isHolyWorld()) {
            if (isJoining || completed) {
                reset();
            }
            return;
        }

        if (completed) {
            return;
        }

        if (currentStep == 0 && !isJoining) {
            if (targetAnarchy <= 0) {
                int anarchy = Network.getAnarchy();
                if (anarchy > 0) {
                    targetAnarchy = anarchy;
                } else {
                    return;
                }
            }
            isJoining = true;
            currentStep = 1;
            hubDelayWatch.reset();
            mc.player.networkHandler.sendChatCommand("hub");
            return;
        }

        if (currentStep == 1) {
            if (hubDelayWatch.elapsedTime() >= 50) {
                InventoryTask.selectCompass();
                actionDelayWatch.reset();
                currentStep = 2;
            }
            return;
        }

        if (currentStep == 2) {
            var mainHandStack = mc.player.getMainHandStack();
            if (mainHandStack.getItem() == Items.COMPASS) {
                if (actionDelayWatch.elapsedTime() >= 50) {
                    mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
                    actionDelayWatch.reset();
                    currentStep = 3;
                }
            } else {
                if (actionDelayWatch.elapsedTime() >= 50) {
                    InventoryTask.selectCompass();
                    actionDelayWatch.reset();
                }
            }
            return;
        }

        if (currentStep == 3) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
                if (actionDelayWatch.elapsedTime() >= 250) {
                    currentStep = 4;
                    actionDelayWatch.reset();
                }
            } else {
                if (actionDelayWatch.elapsedTime() >= 250) {
                    var mainHandStack = mc.player.getMainHandStack();
                    if (mainHandStack.getItem() == Items.COMPASS) {
                        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
                        actionDelayWatch.reset();
                    } else {
                        InventoryTask.selectCompass();
                        currentStep = 2;
                        actionDelayWatch.reset();
                    }
                }
            }
            return;
        }

        if (currentStep == 4) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
                if (actionDelayWatch.elapsedTime() >= 500) {
                    InventoryTask.clickSlot(12, 0, SlotActionType.PICKUP, true);
                    currentStep = 5;
                    actionDelayWatch.reset();
                }
            } else {
                reset();
            }
            return;
        }

        if (currentStep == 5) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
                if (actionDelayWatch.elapsedTime() >= 500) {
                    int categorySlot = -1;
                    if (targetAnarchy <= 16) {
                        categorySlot = 0;
                    } else if (targetAnarchy <= 37) {
                        categorySlot = 1;
                    } else if (targetAnarchy <= 53) {
                        categorySlot = 2;
                    } else if (targetAnarchy >= 54) {
                        categorySlot = 3;
                    }

                    if (categorySlot != -1) {
                        InventoryTask.clickSlot(categorySlot, 0, SlotActionType.PICKUP, true);
                        currentStep = 6;
                        actionDelayWatch.reset();
                    }
                }
            } else {
                reset();
            }
            return;
        }

        if (currentStep == 6) {
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
                if (actionDelayWatch.elapsedTime() >= 500) {
                    boolean found = false;
                    var slots = screen.getScreenHandler().slots;
                    String title = screen.getTitle().getString();
                    boolean isLightAnarchy = title != null && (title.toLowerCase().contains("лайт") || title.contains("1") && title.contains("64"));

                    for (int i = 0; i < slots.size(); i++) {
                        var slot = slots.get(i);
                        var stack = slot.getStack();
                        if (stack.isEmpty()) continue;

                        Integer anarchyNumber = null;

                        if (stack.getItem() == Items.PLAYER_HEAD) {
                            var lore = stack.get(DataComponentTypes.LORE);
                            if (lore != null && !lore.lines().isEmpty()) {
                                for (Text line : lore.lines()) {
                                    String loreText = line.getString();
                                    if (loreText.contains("#")) {
                                        String numberAfterHash = StringUtils.substringAfter(loreText, "#");
                                        if (numberAfterHash != null && !numberAfterHash.isEmpty()) {
                                            anarchyNumber = parseLeadingInt(numberAfterHash.trim());
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (anarchyNumber == null && (isLightAnarchy || targetAnarchy >= 1 && targetAnarchy <= 64)) {
                            String name = stack.getName().getString().replaceAll("§[0-9a-fk-or]", "").replaceAll("§.", "").trim();
                            anarchyNumber = parseLeadingInt(name);
                            if (anarchyNumber == null && stack.get(DataComponentTypes.LORE) != null) {
                                for (Text line : stack.get(DataComponentTypes.LORE).lines()) {
                                    anarchyNumber = parseLeadingInt(line.getString().replaceAll("§[0-9a-fk-or]", "").trim());
                                    if (anarchyNumber != null) break;
                                }
                            }
                        }

                        if (anarchyNumber != null && anarchyNumber == targetAnarchy) {
                            InventoryTask.clickSlot(slot.id, 0, SlotActionType.PICKUP, false);
                            found = true;
                            mc.player.closeHandledScreen();
                            SoundManager.playSound(SoundManager.ENABLE_MODULE);
                            Notifications.getInstance().addList("Перезаход на анархию #" + Formatting.BLUE + anarchyNumber + Formatting.RESET + " выполнен", 3000);
                            reset();
                            active = false;
                            break;
                        }
                    }

                    if (!found && actionDelayWatch.elapsedTime() >= 1000) {
                        reset();
                    }
                }
            } else {
                if (!completed) {
                    reset();
                }
            }
            return;
        }
    }

    private static Integer parseLeadingInt(String s) {
        if (s == null || s.isEmpty()) return null;
        StringBuilder num = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) num.append(c);
            else break;
        }
        if (num.length() == 0) return null;
        try {
            return Integer.parseInt(num.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void reset() {
        targetAnarchy = -1;
        isJoining = false;
        currentStep = 0;
        completed = false;
        hubDelayWatch.reset();
        actionDelayWatch.reset();
    }

    public void setTargetAnarchy(int anarchy) {
        this.targetAnarchy = anarchy;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            reset();
        }
    }

    public boolean isActive() {
        return active;
    }
}