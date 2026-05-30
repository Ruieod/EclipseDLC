package code.essence.features.impl.misc.newAutoMine.api;

import com.google.gson.Gson;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

public interface MineApi {
    static String URL = "http://85.208.139.128:8000/api/mines";
    static Gson GSON = new Gson();
    static HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
}
