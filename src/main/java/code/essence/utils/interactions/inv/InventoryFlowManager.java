package code.essence.utils.interactions.inv;

import code.essence.Essence;
import code.essence.features.impl.misc.ElytraHelper;
import code.essence.features.impl.movement.GuiMove;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.interactions.simulate.Simulations;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.math.task.TaskPriority;
import code.essence.utils.math.script.Script;
import code.essence.utils.client.packet.network.Network;
import code.essence.events.player.InputEvent;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.features.impl.misc.ClickPearl;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@UtilityClass
public class InventoryFlowManager implements QuickImports {
    private static boolean shouldLog() {
        ClickPearl clickPearl = ClickPearl.getInstance();
        return clickPearl != null && clickPearl.isState() && clickPearl.getLogSetting().isValue();
    }

    private static void log(String message) {
        if (shouldLog()) {
            ChatMessage.brandmessage(message);
        }
    }
    public final List<KeyBinding> moveKeys = List.of(mc.options.forwardKey,
            mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
    public static final Script script = new Script(), postScript = new Script();
    private static final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    public boolean canMove = true;

    public void tick() {
        script.update();
        InventoryTask.updateSwapAndUseScript();
        
        
        if (script.isFinished() && !taskQueue.isEmpty()) {
            Runnable nextTask = taskQueue.poll();
            if (nextTask != null) {
                log("§a[InventoryFlow] Обрабатываю задачу из очереди");
                addTask(nextTask);
            }
        }
    }

    public void postMotion() {
        postScript.update();
    }

    public void input(InputEvent e) {

    }

    private void waitForGroundAndExecute(Runnable task) {
        script.cleanup().addTickStep(0, () -> {
            InventoryFlowManager.disableMoveKeys();
            script.addTickStep(2, () -> {
                task.run();
                enableMoveKeys();
            });
        });
    }

    public void addTask(Runnable task) {
        boolean scriptFinished = script.isFinished();
        boolean hasMovement = Simulations.hasPlayerMovement();
        String selectedMode = GuiMove.mode.getSelected();

        log("§b[InventoryFlow] addTask вызван | scriptFinished=" + scriptFinished + " | hasMovement=" + hasMovement + " | mode=" + selectedMode);

        if (scriptFinished) {
            log("§a[InventoryFlow] Условие выполнено, добавляю в script");
            switch (selectedMode) {
                case "ФанТайм" -> {
                    log("§a[InventoryFlow] Режим: ФанТайм");
                    script.cleanup().addTickStep(0, () -> {
                        log("§a[InventoryFlow] ФанТайм: disableMoveKeys");
                        InventoryFlowManager.disableMoveKeys();
                    }).addTickStep(2, () -> {
                        log("§a[InventoryFlow] ФанТайм: выполнение задачи");
                        task.run();
                    }).addTickStep(2, () -> {
                        log("§a[InventoryFlow] ФанТайм: enableMoveKeys");
                        InventoryFlowManager.enableMoveKeys();
                    });
                    return;
                }
                case "СпукиТайм" -> {
                    log("§a[InventoryFlow] Режим: СпукиТайм");
                    script.cleanup().addTickStep(0, () -> {
                        log("§a[InventoryFlow] СпукиТайм: disableMoveKeys");
                        InventoryFlowManager.disableMoveKeys();
                    }).addTickStep(2, () -> {
                        log("§a[InventoryFlow] СпукиТайм: выполнение задачи (swapAndUse)");
                        task.run();
                    }).addTickStep(2, () -> {
                        log("§a[InventoryFlow] СпукиТайм: enableMoveKeys");
                        InventoryFlowManager.enableMoveKeys();
                    });
                    return;
                }
                case "РиллиВорлд" -> {
                    log("§a[InventoryFlow] Режим: РиллиВорлд");
                    script.cleanup().addTickStep(0, () -> {
                        log("§a[InventoryFlow] РиллиВорлд: disableMoveKeys");
                        InventoryFlowManager.disableMoveKeys();
                        script.addTickStep(0, () -> {
                            log("§a[InventoryFlow] РиллиВорлд: выполнение задачи");
                            task.run();
                            log("§a[InventoryFlow] РиллиВорлд: enableMoveKeys");
                            enableMoveKeys();
                        });
                    });
                    return;
                }
                case "КопиТайм" -> {
                    log("§a[InventoryFlow] Режим: КопиТайм");
                    script.cleanup().addTickStep(0, () -> {
                        log("§a[InventoryFlow] КопиТайм: disableMoveKeys");
                        InventoryFlowManager.disableMoveKeys();
                    }).addTickStep(2, () -> {
                        log("§a[InventoryFlow] КопиТайм: выполнение задачи");
                        task.run();
                    }).addTickStep(3, () -> {
                        log("§a[InventoryFlow] КопиТайм: enableMoveKeys");
                        InventoryFlowManager.enableMoveKeys();
                    });
                    return;
                }
                case "ХолиВорлд" -> {
                    log("§a[InventoryFlow] Режим: ХолиВорлд");
                    script.cleanup().addTickStep(0, () -> {
                        log("§a[InventoryFlow] ХолиВорлд: disableMoveKeys");
                        InventoryFlowManager.disableMoveKeys();
                    }).addTickStep(4, () -> {
                        log("§a[InventoryFlow] ХолиВорлд: выполнение задачи");
                        task.run();
                    }).addTickStep(5, () -> {
                        log("§a[InventoryFlow] ХолиВорлд: enableMoveKeys");
                        InventoryFlowManager.enableMoveKeys();
                    });
                    return;
                }
                default -> {
                    log("§c[InventoryFlow] Неизвестный режим: " + selectedMode + ", добавляю в очередь задач");
                    taskQueue.offer(task);
                    return;
                }
            }
        } else {
            log("§c[InventoryFlow] Условие НЕ выполнено, добавляю в очередь задач");
            
            taskQueue.offer(task);
        }
    }

    private void rotateToCamera() {
        Module module = new Module("InventoryComponent","Inventory Component", ModuleCategory.PLAYER);
        module.state = true;
        TurnsConnection.INSTANCE.rotateTo(MathAngle.cameraAngle(), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, module);
    }

    public void disableMoveKeys() {
        log("§c[InventoryFlow] disableMoveKeys | scriptFinished=" + script.isFinished());
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        log("§a[InventoryFlow] enableMoveKeys | scriptFinished=" + script.isFinished());
        InventoryTask.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
        mc.player.setSprinting(false);
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding ->
                keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
    }

    public boolean shouldSkipExecution() {
        if (mc.currentScreen == null) return false;
        if (PlayerInteractionHelper.isChat(mc.currentScreen)) return false;
        if (mc.currentScreen instanceof SignEditScreen
                || mc.currentScreen instanceof AnvilScreen
                || mc.currentScreen instanceof AbstractCommandBlockScreen
                || mc.currentScreen instanceof StructureBlockScreen ) {

            return false;
        }
        if (GuiMove.mode.isSelected("ХолиВорлд") && mc.currentScreen instanceof GenericContainerScreen ) {
            return false;
        }
        return true;
    }
}