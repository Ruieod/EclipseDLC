package code.essence.features.impl.movement.autoMystGlider.parsers;

import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.display.interfaces.QuickImports;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * @author nikitavodolaz
 * @since 04.02.2026
 */

@UtilityClass
public class SpookyParser implements QuickImports {
    private static final String API_URL = "http://85.208.139.128:8000/api/events";
    private static final int MIN_SECONDS = 120; // минимум 2 минуты до открытия

    // Кеш последнего результата из API
    private int cachedAnarchy = -1;
    private long cachedRemainingMs = 2000;

    /** Парсит номер текущей анархии из скорборда */
    public int parseAnarchy() {
        Scoreboard scoreboard = mc.player.getScoreboard();

        if (scoreboard != null) {
            for (ScoreboardObjective objective : scoreboard.getObjectives()) {
                String string = objective.getDisplayName().getString();

                if (string.contains("Анархия")) {
                    String formattedString = string.replaceAll("\\D", "");

                    try {
                        return Integer.parseInt(formattedString);
                    } catch (NumberFormatException e) {
                        e.fillInStackTrace();
                    }
                }
            }
        }

        return -1;
    }

    public boolean isAnarchy(int num) {
        return parseAnarchy() == num;
    }

    public boolean isAnarchy(String string) {
        int cur = parseAnarchy();
        if (cur == -1) return false;

        int parsed;
        try {
            parsed = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }

        return cur == parsed;
    }

    /** Парсит оставшееся время из боссбара (секунды). Возвращает задержку в мс (за 2 сек до открытия). */
    public long parseRemainingTime() {
        BossBarHud bossBarHud = mc.inGameHud.getBossBarHud();

        if (bossBarHud != null) {
            for (Map.Entry<UUID, ClientBossBar> entry : bossBarHud.bossBars.entrySet()) {
                ClientBossBar clientBossBar = entry.getValue();
                String time = clientBossBar.getName().getString().replaceAll("\\D", "");

                if (!time.isBlank()) {
                    try {
                        int seconds = Integer.parseInt(time);
                        return Math.max((seconds - 2) * 1000L, 1000);
                    } catch (NumberFormatException e) {
                        e.fillInStackTrace();
                    }
                }
            }
        }

        return 2000;
    }

    /**
     * Загружает ивенты с API, находит ивент с >= 2 минутами до открытия.
     * Кеширует номер анархии и время до захода (за 2 секунды до открытия).
     */
    public void fetchFromApi() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray events = json.getAsJsonArray("events");

                JsonObject bestEvent = null;
                int bestSeconds = Integer.MAX_VALUE;

                for (JsonElement element : events) {
                    JsonObject event = element.getAsJsonObject();
                    int timeSeconds = event.get("time_seconds").getAsInt();

                    if (timeSeconds >= MIN_SECONDS && timeSeconds < bestSeconds) {
                        bestSeconds = timeSeconds;
                        bestEvent = event;
                    }
                }

                if (bestEvent != null) {
                    cachedAnarchy = bestEvent.get("anarchy_number").getAsInt();
                    cachedRemainingMs = Math.max((bestSeconds - 2) * 1000L, 1000);
                    ChatMessage.brandmessage("API: Анархия " + cachedAnarchy + ", ивент через " + bestSeconds + " сек");
                } else {
                    cachedAnarchy = -1;
                    cachedRemainingMs = 30_000;
                    ChatMessage.brandmessage("API: Нет подходящих ивентов (>= 2 мин)");
                }
            }
        } catch (Exception e) {
            ChatMessage.brandmessage("API: Ошибка загрузки ивентов: " + e.getMessage());
            cachedAnarchy = -1;
            cachedRemainingMs = 30_000;
        }
    }

    public int getCachedAnarchy() {
        return cachedAnarchy;
    }

    public long getCachedRemainingMs() {
        return cachedRemainingMs;
    }
}
