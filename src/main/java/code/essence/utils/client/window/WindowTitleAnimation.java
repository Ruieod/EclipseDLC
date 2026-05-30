package code.essence.utils.client.window;

import code.essence.Essence;

public class WindowTitleAnimation {
    private static WindowTitleAnimation INSTANCE;
    private String currentTitle;
    private int animationTick = 0;
    private boolean isRemoving = true;
    private boolean isUserPhase = true;
    private int pauseTicks = 0;
    private final int delayTicks = 1;
    private final int pauseDuration = 100;

    private WindowTitleAnimation() {
        String username = "Unknown";
        try {
            if (Essence.getInstance() != null && Essence.getInstance().getNativeUsername() != null) {
                username = Essence.getInstance().getNativeUsername();
            }
        } catch (Exception e) {
        }
        currentTitle = "<User: " + username + ">";
    }

    public static WindowTitleAnimation getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WindowTitleAnimation();
        }
        return INSTANCE;
    }

    public void updateTitle() {
        if (pauseTicks > 0) {
            pauseTicks--;
            return;
        }
        if (animationTick >= delayTicks) {
            String newTitle;
            String fullText = "| Закрытый бета тест | Build 0.06 19/11/2025";
            if (isRemoving) {
                if (currentTitle.length() > 1) {
                    newTitle = currentTitle.substring(0, currentTitle.length() - 1);
                } else {
                    newTitle = "<";
                    isRemoving = false;
                }
            } else {
                if (currentTitle.length() < fullText.length()) {
                    newTitle = fullText.substring(0, currentTitle.length() + 1);
                } else {
                    newTitle = fullText;
                    isRemoving = true;
                    isUserPhase = !isUserPhase;
                    pauseTicks = pauseDuration;
                }
            }
            currentTitle = newTitle;
            animationTick = 0;
        }
        animationTick++;
    }

    public String getCurrentTitle() {
        return "Essence 1.21.4 Build: 2.09.09 - Release date: 15/02/2026";
    }
}