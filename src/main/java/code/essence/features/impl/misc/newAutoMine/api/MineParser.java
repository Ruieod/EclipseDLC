package code.essence.features.impl.misc.newAutoMine.api;

import code.essence.features.impl.misc.newAutoMine.MiningAnarchyState;
import code.essence.features.impl.player.autoMine.api.MineType;
import code.essence.utils.display.interfaces.QuickImports;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MineParser implements QuickImports, MineApi {
    MiningAnarchyState anarchyState = null;

    public static int formatMMSS(String string) {
        String[] mmss = string.split(":");

        if (mmss.length > 1) {
            return Integer.parseInt(mmss[0]) * 60 + Integer.parseInt(mmss[1]);
        } else {
            return Integer.parseInt(mmss[0]);
        }
    }

    public int getTimeToMine() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStandEntity) {
                String string = armorStandEntity.getName().getString();

                if (string.contains("Обновление через")) {
                    int id = armorStandEntity.getId();

                    Entity nextArmorEntity = mc.world.getEntityById(id + 1);
                    if (nextArmorEntity != null) {
                        String duration = nextArmorEntity.getName().getString().replaceAll("[^0-9:]", "");
                        return formatMMSS(duration);
                    }
                }
            }
        }

        return 0;
    }

    public CompletableFuture<Void> sendRequest() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        readResponse(response.body());
                    }
                })
                .exceptionally(e -> null);
    }

    public void readResponse(String json) {
        JsonObject mainObject = GSON.fromJson(json, JsonObject.class);

        if (!mainObject.has("mines")) return;

        String bestAnarchy = null;
        String bestType = null;
        int bestTime = 0;

        for (JsonElement jsonElement : mainObject.get("mines").getAsJsonArray()) {
            String[] anTypeTime = jsonElement.getAsString().split(" ");

            String anarchyString = anTypeTime[0];
            String typeString = anTypeTime[1];
            String timeString = anTypeTime[2];

            int time = formatMMSS(timeString);

            if (typeString.equals("Легендарная") && time < bestTime) {
                bestTime = time;
                bestAnarchy = anarchyString;
                bestType = typeString;
            }
        }

        if (bestAnarchy != null && bestType != null) {
            MiningAnarchyState anarchyState1 = new MiningAnarchyState(bestAnarchy, getTypeFromString(bestType), bestTime);

            if (!anarchyState.equals(anarchyState1)) {
                anarchyState = anarchyState1;
            }
        }
    }

    private MineType getTypeFromString(String string) {
        if (string == null) return null;
        if (string.equals("Легендарная")) return MineType.LEGENDARY;
        else if (string.equals("Мифическая")) return MineType.MYTHICAL;
        return null;
    }
}
