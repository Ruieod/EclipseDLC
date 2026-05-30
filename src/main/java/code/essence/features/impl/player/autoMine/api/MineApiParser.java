package code.essence.features.impl.player.autoMine.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсит API шахт: http://85.208.139.128:8000/api/mines
 * Формат: {"mines":["/an503 Обычная 00:06","/an215 Легендарная 05:06",...]}
 * Выбирает лучшую ЛЕГЕНДАРНУЮ шахту (с наименьшим временем).
 *
 * @author nikitavodolaz
 * @since 11.02.2026
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MineApiParser {
    static final String API_URL = "http://85.208.139.128:8000/api/mines";
    static final Gson GSON = new Gson();
    static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    static final Pattern MINE_PATTERN = Pattern.compile("(/an\\d+)\\s+(\\S+)\\s+(\\d{2}):(\\d{2})");

    // Фильтр типа шахты
    static final String LEGENDARY_TYPE = "Легендарная";

    // Лучшая легендарная шахта
    String bestAnarchyCommand = null;  // например "/an215"
    String bestMineType = null;        // "Легендарная"
    int bestTimeSeconds = -1;          // время в секундах
    boolean found = false;
    long lastFetchTime = 0;

    // Интервал между запросами (мс)
    static final long FETCH_INTERVAL_MS = 2000;

    /**
     * Асинхронно запрашивает данные с API.
     */
    public CompletableFuture<Void> fetchAsync() {
        if (System.currentTimeMillis() - lastFetchTime < FETCH_INTERVAL_MS) {
            return CompletableFuture.completedFuture(null);
        }

        lastFetchTime = System.currentTimeMillis();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        parseResponse(response.body());
                    }
                })
                .exceptionally(e -> {
                    // Ошибка сети - игнорируем
                    return null;
                });
    }

    /**
     * Синхронно запрашивает данные (блокирует поток).
     */
    public void fetchSync() {
        if (System.currentTimeMillis() - lastFetchTime < FETCH_INTERVAL_MS) {
            return;
        }

        lastFetchTime = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                parseResponse(response.body());
            }
        } catch (Exception e) {
            // Ошибка сети - игнорируем
        }
    }

    private void parseResponse(String json) {
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);

            if (!obj.has("mines")) {
                found = false;
                return;
            }

            JsonArray mines = obj.getAsJsonArray("mines");

            String bestCmd = null;
            String bestType = null;
            int bestTime = Integer.MAX_VALUE;

            for (JsonElement element : mines) {
                String mineStr = element.getAsString();
                Matcher matcher = MINE_PATTERN.matcher(mineStr);

                if (matcher.find()) {
                    String cmd = matcher.group(1);
                    String type = matcher.group(2);
                    int minutes = Integer.parseInt(matcher.group(3));
                    int seconds = Integer.parseInt(matcher.group(4));
                    int totalSeconds = (minutes * 60) + seconds;

               //     if (type.equals("Легендарная") || type.equals("Мифическая")) {
                        if (totalSeconds < bestTime) {
                            bestCmd = cmd;
                            bestType = type;
                            bestTime = totalSeconds;
                        }
                    }
                }


            if (bestCmd != null) {
                bestAnarchyCommand = bestCmd;
                bestMineType = bestType;
                bestTimeSeconds = bestTime;
                found = true;
            } else {
                // Легендарных нет
                found = false;
                bestAnarchyCommand = null;
                bestMineType = null;
                bestTimeSeconds = -1;
            }

        } catch (Exception e) {
            // Ошибка парсинга - игнорируем
            found = false;
        }
    }

    /**
     * Проверяет, готова ли шахта для захода.
     * Шахта готова если время < threshold секунд (скоро обновится — нужно заходить).
     */
    public boolean isMineReady(int thresholdSeconds) {
        return found && bestTimeSeconds >= 0 && bestTimeSeconds < thresholdSeconds;
    }

    /**
     * Получает команду для захода на анархию (например "/an215").
     */
    public String getAnarchyCommand() {
        return bestAnarchyCommand;
    }

    /**
     * Получает тип шахты.
     */
    public String getMineType() {
        return bestMineType;
    }

    /**
     * Получает оставшееся время в секундах.
     */
    public int getTimeSeconds() {
        return bestTimeSeconds;
    }

    /**
     * Форматирует время для отображения.
     */
    public String getFormattedTime() {
        if (bestTimeSeconds < 0) return "--:--";
        int min = bestTimeSeconds / 60;
        int sec = bestTimeSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Проверяет, найдена ли легендарная шахта.
     */
    public boolean isFound() {
        return found;
    }

    /**
     * Сбрасывает состояние.
     */
    public void reset() {
        bestAnarchyCommand = null;
        bestMineType = null;
        bestTimeSeconds = -1;
        found = false;
    }
}
