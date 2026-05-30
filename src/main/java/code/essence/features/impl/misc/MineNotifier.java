package code.essence.features.impl.misc;

import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.math.time.StopWatch;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static code.essence.utils.display.interfaces.QuickImports.mc;

public class MineNotifier extends Module {
    private static final String API_URL = "http://85.208.139.128:8000/api/mines/nearest";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    private final StopWatch updateTimer = new StopWatch();
    private static final long UPDATE_INTERVAL_MS = 5000L; 
    
    private String lastMineInfo = "";
    
    public MineNotifier() {
        super("MineNotifier", "Mine Notifier", ModuleCategory.MISC);
        updateTimer.reset();
    }
    
    @Override
    public void activate() {
        super.activate();
        ChatMessage.brandmessage("MineNotifier включен. Проверка автошахт каждые 5 секунд...");
        lastMineInfo = "";
        updateTimer.reset();
        
        fetchNearestMine();
    }
    
    @Override
    public void deactivate() {
        super.deactivate();
        ChatMessage.brandmessage("MineNotifier выключен.");
    }
    
    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        
        
        if (!updateTimer.finished(UPDATE_INTERVAL_MS)) return;
        updateTimer.reset();
        
        
        fetchNearestMine();
    }
    
    private void fetchNearestMine() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                String responseBody = response.body();
                                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                                
                                boolean found = json.get("found").getAsBoolean();
                                if (found) {
                                    String mineInfo = json.get("mine").getAsString();
                                    
                                    
                                    if (!mineInfo.equals(lastMineInfo)) {
                                        lastMineInfo = mineInfo;
                                        
                                        mc.execute(() -> {
                                            ChatMessage.brandmessage(mineInfo);
                                        });
                                    }
                                }
                            } catch (Exception e) {
                                
                            }
                        }
                    })
                    .exceptionally(e -> {
                        
                        return null;
                    });
        } catch (Exception e) {
            
        }
    }
}

