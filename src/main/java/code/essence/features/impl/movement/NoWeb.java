package code.essence.features.impl.movement;


import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.utils.interactions.simulate.Simulations;
import code.essence.utils.client.Instance;
import code.essence.events.player.TickEvent;
import code.essence.utils.input.MoveUtil;
import net.minecraft.util.math.Box;

import static code.essence.utils.interactions.simulate.Simulations.forward;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoWeb extends Module {
    public static NoWeb getInstance() {
        return Instance.get(NoWeb.class);
    }

    public final SelectSetting webMode = new SelectSetting("Режим", "Выберите режим обхода")
            .value("Грим", "Грим новый");
    
    SliderSettings speedy = new SliderSettings("Скорость Y", "Выберите скорость для стрейфа")
            .setValue(0.42F).range(0F, 3F).step(0.1f)
            .visible(() -> webMode.getSelected().equals("Грим"));
    
    SliderSettings speedxz = new SliderSettings("Скорость XZ", "Выберите скорость для стрейфа")
            .setValue(0.64F).range(0F, 1F).step(0.01f)
            .visible(() -> webMode.getSelected().equals("Грим"));

    
    final SliderSettings grimSpeed = new SliderSettings("Скорость XZ", "Скорость движения в паутине")
            .range(0.05f, 0.5f)
            .setValue(0.05f)
            .step(0.05f)
            .visible(() -> webMode.getSelected().equals("Грим новый"));
    final SliderSettings grimSpeedY = new SliderSettings("Скорость Y", "Скорость движения в паутине")
            .range(0.05f, 0.8f)
            .setValue(0.65f)
            .step(0.05f)
            .visible(() -> webMode.getSelected().equals("Грим новый"));

    
    int webTicks = 0;
    boolean wasInWeb = false;
    boolean grimFlag = false;
    int packetDelay = 0;

    public NoWeb() {
        super("NoWeb", "NoWeb", ModuleCategory.MOVEMENT);
        setup(webMode, speedy, speedxz, grimSpeed,grimSpeedY);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        String mode = webMode.getSelected();
        
        if (mode.equals("Грим")) {
            if (PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
                double[] speed = Simulations.calculateDirection(speedxz.getValue());
                mc.player.addVelocity(speed[0], 0, speed[1]);
                mc.player.velocity.y = mc.options.jumpKey.isPressed() ? speedy.getValue() : mc.options.sneakKey.isPressed() ? -speedy.getValue() *1.3 : 0;
            }
        } else if (mode.equals("Грим новый")) {
            if (mc.player == null || mc.world == null) return;

            boolean inWeb = isPlayerInWeb();

            if (inWeb) {
                if (!wasInWeb) {
                    webTicks = 0;
                    grimFlag = false;
                    packetDelay = 0;
                }
                webTicks++;
                wasInWeb = true;

                handleWebBypass();
            } else {
                webTicks = 0;
                wasInWeb = false;
                grimFlag = false;
                packetDelay = 0;
            }

            if (packetDelay > 0) {
                packetDelay--;
            }
        }
    }

    private void handleWebBypass() {
        String mode = webMode.getSelected();
        if (mode.equals("Грим новый")) {
            handleGrimV2();
        }
    }

    private void handleGrimV2() {
        if (mc.player.getHungerManager().getFoodLevel() < 20) {
            return;
        }

        double baseSpeed = grimSpeed.getValue();

        if (webTicks == 0 && !grimFlag) {
            boolean isSwimming = mc.player.isSwimming();

            if (!isSwimming) {
                mc.player.networkHandler.sendPacket(
                        new HandSwingC2SPacket(Hand.MAIN_HAND)
                );

                PlayerInteractionHelper.interactItem(Hand.OFF_HAND);

                grimFlag = true;
                packetDelay = 2;
            }
        }

        if (webTicks > 0 && packetDelay == 0) {
            if (MoveUtil.isMoving()) {
                double[] motion = forward(baseSpeed);
                mc.player.addVelocity(motion[0], 0, motion[1]);
            }

            if (!mc.player.isOnGround()) {
                if (mc.options.jumpKey.isPressed()) {
                    mc.player.velocity.y = grimSpeedY.getValue();
                } else if (mc.options.sneakKey.isPressed()) {
                    mc.player.velocity.y = -grimSpeedY.getValue();
                }
            }
        }
    }

    private boolean isPlayerInWeb() {
        if (mc.player == null || mc.world == null) return false;
        Box box = mc.player.getBoundingBox();
        return PlayerInteractionHelper.isBoxInBlock(box, Blocks.COBWEB);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        webTicks = 0;
        wasInWeb = false;
        grimFlag = false;
        packetDelay = 0;
    }
}