package code.essence.features.impl.player;

import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.math.calc.Calculate;

import static code.essence.utils.display.interfaces.QuickImports.mc;

public class AntiAFK extends Module {

    private final RadioSetting mode = new RadioSetting("Режим", "Режим работы AntiAFK",
            new String[]{"Движение", "Автобай"}, "Движение");
    
    private final SliderSettings moveDelay = new SliderSettings("Задержка перед движением", "Задержка в секундах")
            .setValue(59f).range(1f, 60f).step(0.5f)
            .visible(() -> mode.get().equals("Движение"));

    private long lastSpecialMoveTime = 0L;
    private boolean performingSpecialMove = false;
    private long specialMoveStartTime = 0L;
    private int specialMovePhase = 0; 

    
    private long lastRctTime = 0L;
    private long ahScheduledTime = 0L;

    public AntiAFK() {
        super("AntiAFK", ModuleCategory.PLAYER);
        setup(mode, moveDelay);
    }

    @Override
    public void activate() {
        lastSpecialMoveTime = System.currentTimeMillis();
        performingSpecialMove = false;
        specialMovePhase = 0;
        lastRctTime = System.currentTimeMillis();
        ahScheduledTime = 0L;
        super.activate();
    }

    @Override
    public void deactivate() {
        resetAllKeys();
        ahScheduledTime = 0L;
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        long currentTime = System.currentTimeMillis();

        if (mode.get().equals("Движение")) {
            handleMovementMode(currentTime);
        } else if (mode.get().equals("Автобай")) {
            handleAutobuyMode(currentTime);
        }
    }

    private void handleMovementMode(long currentTime) {
        
        if (performingSpecialMove) {
            long specialMoveElapsed = currentTime - specialMoveStartTime;

            switch (specialMovePhase) {
                case 0: 
                    resetAllKeys();
                    mc.options.forwardKey.setPressed(true);

                    if (specialMoveElapsed >= 280) {
                        specialMovePhase = 1;
                        specialMoveStartTime = currentTime;
                        resetAllKeys();
                    }
                    break;

                case 1: 
                    resetAllKeys();
                    mc.options.backKey.setPressed(true);

                    if (specialMoveElapsed >= 350) {
                        specialMovePhase = 2;
                        performingSpecialMove = false;
                        resetAllKeys();
                    }
                    break;
            }
            return;
        }

        
        long delayMs = (long) (moveDelay.getValue() * 1000);

        
        if (currentTime - lastSpecialMoveTime >= delayMs) {
            performingSpecialMove = true;
            specialMoveStartTime = currentTime;
            specialMovePhase = 0;
            lastSpecialMoveTime = currentTime;
            resetAllKeys();
        }
    }

    private void handleAutobuyMode(long currentTime) {
        
        if (ahScheduledTime > 0 && currentTime >= ahScheduledTime) {
            ahScheduledTime = 0L;
        }

        
        if (currentTime - lastRctTime >= 59000) {
            mc.player.networkHandler.sendChatMessage(".rct");
            lastRctTime = currentTime;
            
            ahScheduledTime = currentTime + 5000;
        }
    }

    private void resetAllKeys() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }
}
