package code.essence.utils.features.aura.utils;

import code.essence.features.impl.combat.Aura;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.client.packet.network.Network;
import code.essence.Essence;


public class Pressing implements QuickImports {
    private final int[] funTimeTicks = new int[]{10, 11, 10, 13}, spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12}, defaultTicks = new int[]{10, 11};
    long lastClickTime = System.currentTimeMillis();
    private static final long MINIMUM_COOLDOWN_MS = 500;


    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        boolean isMace = isHoldingMace();

        boolean cooldownReady = isMace || mc.player.getAttackCooldownProgress(ticks) > 0.9F;
        long delay = Aura.getInstance().getCps().get().equals("1.9") ? 450 : 60;
       /* if (Aura.getInstance().getCps().get().equals("1.9") && Aura.aimMode.get().equals("ХолиВорлд")) {
            delay = (long) (450 + Math.random() * 20);
        }*/
        boolean minimumDelayPassed = lastClickPassed() >= delay;
        return cooldownReady && minimumDelayPassed;
    }

    public boolean hasTicksElapsedSinceLastClick(int ticks) {
        return lastClickPassed() >= (ticks * 50L * (20F / Network.TPS));
    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate() {
        lastClickTime = System.currentTimeMillis();
    }

    int tickCount() {
        int count = Essence.getInstance().getAttackPerpetrator().getAttackHandler().getCount();
        return switch (Network.server) {

            default -> defaultTicks[count % defaultTicks.length];
        };
    }

    private boolean isHoldingMace() {
        ItemStack mainHand = mc.player.getMainHandStack();

        return mainHand.getItem().getTranslationKey().toLowerCase().contains("mace");
    }
}