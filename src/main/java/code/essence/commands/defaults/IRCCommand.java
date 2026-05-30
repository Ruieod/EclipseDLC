package code.essence.commands.defaults;

import code.essence.Essence;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.client.managers.api.command.Command;
import code.essence.utils.client.managers.api.command.argument.IArgConsumer;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class IRCCommand extends Command {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> messageCheckTask;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    public static boolean run = false;
    public static boolean turn = false;
    private static final Set<String> displayedMessages = new HashSet<>();
    private static long ircEnabledAtMs = 0;

    /** URL IRC API (как с автошахтами — тот же хост). POST /irc/send, GET /irc/messages */
    private static final String IRC_API_BASE = "http://85.208.139.128:8002";

    public IRCCommand() {
        super("irc");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            error();
            return;
        }

        String firstArg = args.peekString().toLowerCase();
        String nickname = getNickname();

        if (nickname == null || nickname.isEmpty()) {
            nickname = "хакер";
        }

        if ("on".equals(firstArg)) {
            args.getString();
            if (run) {
                ChatMessage.brandmessage("IRC уже включен!");
                return;
            }
            run = true;
            ChatMessage.brandmessage("IRC включен. Ваш ник: " + nickname);

                messageCheckTask = scheduler.scheduleAtFixedRate(() -> {
                    fetchMessages().exceptionally(e -> null);
                }, 0, 1, TimeUnit.MILLISECONDS);
        } else if ("off".equals(firstArg)) {
            args.getString();
            if (!run) {
                ChatMessage.brandmessage("IRC уже выключен!");
                return;
            }
            run = false;
            if (messageCheckTask != null) {
                messageCheckTask.cancel(false);
            }
            displayedMessages.clear();
            ChatMessage.brandmessage("IRC выключен.");
        } else {
            String message = args.rawRest();
            if (message.isEmpty()) {
                error();
                return;
            }
            sendMessage(nickname, message).exceptionally(e -> null);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return Stream.of("on", "off");
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Общение между игроками";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для общения между игроками через IRC",
                "",
                "Использование:",
                "> irc on - включить IRC",
                "> irc off - выключить IRC",
                "> irc <сообщение> - отправить сообщение в IRC"
        );
    }

    /** Ник из API (VMBridge / облачная авторизация). */
    private String getNickname() {
        return Essence.getInstance().getNativeUsername();
    }

    private CompletableFuture<Void> sendMessage(String nickname, String message) {
        try {
            String postData = "nickname=" + URLEncoder.encode(nickname, StandardCharsets.UTF_8)
                    + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(IRC_API_BASE + "/irc/send"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(postData))
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                    })
                    .exceptionally(e -> null);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> fetchMessages() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(IRC_API_BASE + "/irc/messages"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        String body = response.body();
                        if (body == null || body.isEmpty() || body.equals("[]")) return;

                        try {
                            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body).getAsJsonArray();
                            for (com.google.gson.JsonElement el : arr) {
                                var obj = el.getAsJsonObject();
                                String nickname = obj.has("nickname") ? obj.get("nickname").getAsString() : "";
                                String message = obj.has("message") ? obj.get("message").getAsString() : "";
                                long timestamp = obj.has("timestamp") ? obj.get("timestamp").getAsLong() : 0;
                                boolean isAdmin = obj.has("is_admin") && obj.get("is_admin").getAsBoolean();

                                String decodedMessage = URLDecoder.decode(message, StandardCharsets.UTF_8);
                                decodedMessage = decodeUnicode(decodedMessage);
                                if (!isValidUTF8(decodedMessage)) decodedMessage = "[Invalid]";

                                String messageKey = nickname + ":" + message + ":" + timestamp;
                                if (nickname.isEmpty() || message.isEmpty() || displayedMessages.contains(messageKey))
                                    continue;

                                displayedMessages.add(messageKey);

                                MutableText ircMessage = Text.literal("");
                                if (isAdmin) {
                                    ircMessage.append(Text.literal("[").formatted(Formatting.GRAY))
                                            .append(Text.literal("OWNER").formatted(Formatting.RED))
                                            .append(Text.literal("] ").formatted(Formatting.GRAY));
                                }
                                ircMessage.append(Text.literal(nickname).formatted(Formatting.WHITE))
                                        .append(Text.literal(" >> ").formatted(Formatting.DARK_GRAY))
                                        .append(Text.literal(decodedMessage).formatted(Formatting.WHITE));
                                ChatMessage.brandmessage(ircMessage);
                            }
                        } catch (Exception ignored) {
                        }
                    })
                    .exceptionally(e -> null);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private String decodeUnicode(String input) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(input);
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(input, lastEnd, matcher.start());
            String hex = matcher.group(1);
            result.append((char) Integer.parseInt(hex, 16));
            lastEnd = matcher.end();
        }
        result.append(input.substring(lastEnd));
        return result.toString();
    }

    private boolean isValidUTF8(String input) {
        try {
            new String(input.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void error() {
        MutableText errorMsg = Text.literal("Ошибка в использовании").formatted(Formatting.GRAY)
                .append(Text.literal(":").formatted(Formatting.WHITE));
        ChatMessage.brandmessage(errorMsg.getString());
        ChatMessage.brandmessage(Formatting.WHITE + ".irc on" + Formatting.GRAY + " - включить IRC");
        ChatMessage.brandmessage(Formatting.WHITE + ".irc off" + Formatting.GRAY + " - выключить IRC");
        ChatMessage.brandmessage(Formatting.WHITE + ".irc сообщение" + Formatting.GRAY + " - отправить сообщение в IRC");
    }
}

