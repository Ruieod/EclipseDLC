package code.essence.utils.client.packet.network;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.time.StopWatch;
import code.essence.events.packet.PacketEvent;

@Getter
@UtilityClass
public class Network implements QuickImports {
    private final StopWatch pvpWatch = new StopWatch();
    public String server = "Vanilla";
    public float TPS = 20;
    public long timestamp;
    @Getter
    public int anarchy;
    @Getter
    public boolean pvpEnd;

    public void tick() {
        anarchy = getAnarchyMode();
        server = getServer();
        pvpEnd = inPvpEnd();
        if (inPvp()) pvpWatch.reset();
    }

    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case WorldTimeUpdateS2CPacket time -> {
                long nanoTime = System.nanoTime();

                float maxTPS = 20;
                float rawTPS = maxTPS * (1e9f / (nanoTime - timestamp));

                TPS = MathHelper.clamp(rawTPS, 0, maxTPS);
                timestamp = nanoTime;
            }
            default -> {}
        }
    }

    public String getServer() {
        if (PlayerInteractionHelper.nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) return "Vanilla";
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand().toLowerCase() : "";

        if (brand.contains("botfilter")) return "FunTime";
        else if (brand.contains("§6spooky§ccore")) return "SpookyTime";
        else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) return "CopyTime";
        else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        else if (serverIp.contains("gulpvp")) return "GulPvP";
        
        
        if (mc.world != null && mc.world.getScoreboard() != null) {
            ScoreboardObjective objective = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null) {
                String title = objective.getDisplayName().getString();
                if (title != null && (title.equals("HolyWorld.me") || title.equals("HolyWorld") || title.toLowerCase().contains("holyworld"))) {
                    return "HolyWorld";
                }
            }
        }
        
        return "Vanilla";
    }

    private int getAnarchyMode() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return -1;
        switch (server) {
            case "FunTime", "SpookyTime" -> {
                String title = objective.getDisplayName().getString();
                
                if (title.contains("Анархия-")) {
                    String[] parts = title.split("Анархия-");
                    if (parts.length > 1) {
                        try {
                            String numberStr = parts[1].trim().split(" ")[0];
                            return Integer.parseInt(numberStr);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                var entries = scoreboard.getScoreboardEntries(objective).stream()
                    .sorted((e1, e2) -> Integer.compare(e2.value(), e1.value()))
                    .toList();
                
                for (ScoreboardEntry entry : entries) {
                    String text = Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()).getString();
                    if (text.contains("Анархия-")) {
                        String[] parts = text.split("Анархия-");
                        if (parts.length > 1) {
                            try {
                                String numberStr = parts[1].trim().split(" ")[0];
                                return Integer.parseInt(numberStr);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            case "HolyWorld" -> {
                String title = objective.getDisplayName().getString();
                
                var entries = scoreboard.getScoreboardEntries(objective).stream()
                    .sorted((e1, e2) -> Integer.compare(e2.value(), e1.value()))
                    .toList();
                
                if (!entries.isEmpty()) {
                    ScoreboardEntry lastEntry = entries.get(entries.size() - 1);
                    String lastText = Team.decorateName(scoreboard.getScoreHolderTeam(lastEntry.owner()), lastEntry.name()).getString();
                    
                    if (!lastText.isEmpty() && lastText.contains("#")) {
                        String numberAfterHash = StringUtils.substringAfter(lastText, "#");
                        if (numberAfterHash != null && !numberAfterHash.isEmpty()) {
                            try {
                                StringBuilder numBuilder = new StringBuilder();
                                for (char c : numberAfterHash.trim().toCharArray()) {
                                    if (Character.isDigit(c)) {
                                        numBuilder.append(c);
                                    } else {
                                        break;
                                    }
                                }
                                if (numBuilder.length() > 0) {
                                    return Integer.parseInt(numBuilder.toString());
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                
                for (ScoreboardEntry scoreboardEntry : entries) {
                    String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                    if (text.isEmpty() || text.contains("Клан:") || text.contains("Трио:")) {
                        continue;
                    }
                    
                    if (text.contains("#")) {
                        String numberAfterHash = StringUtils.substringAfter(text, "#");
                        if (numberAfterHash != null && !numberAfterHash.isEmpty()) {
                            try {
                                StringBuilder numBuilder = new StringBuilder();
                                for (char c : numberAfterHash.trim().toCharArray()) {
                                    if (Character.isDigit(c)) {
                                        numBuilder.append(c);
                                    } else {
                                        break;
                                    }
                                }
                                if (numBuilder.length() > 0) {
                                    return Integer.parseInt(numBuilder.toString());
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                
                if (title != null && (title.equals("HolyWorld.me") || title.equals("HolyWorld"))) {
                    return -1;
                }
            }
            case "ReallyWorld" -> {
                String title = objective.getDisplayName().getString();
                String number = StringUtils.substringAfter(title, "#");
                if (number != null && !number.isEmpty()) {
                    try {
                        return Integer.parseInt(number.trim().split(" ")[0]);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return -1;
    }

    public boolean isPvp() {
        return !pvpWatch.finished(500);
    }

    private boolean inPvp() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
    }

    private boolean inPvpEnd() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase())
                .anyMatch(s -> (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1")));
    }

    public String getWorldType() {
        return mc.world.getRegistryKey().getValue().getPath();
    }

    public boolean isCopyTime() {return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");}
    public boolean isFunTime() {return server.equals("FunTime");}
    public boolean isReallyWorld() {return server.equals("ReallyWorld");}
    public boolean isGulPvP() {return server.equals("GulPvP");}
    public boolean isHolyWorld() {return server.equals("HolyWorld");}
    public boolean isSpookyTime() {return server.equals("SpookyTime");}
    public boolean isAresMine() {return server.equals("aresmine");}
    public boolean isVanilla() {return server.equals("Vanilla");}
}
