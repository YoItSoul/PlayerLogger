package com.hytaletravelers.playerlogger.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hytaletravelers.playerlogger.config.PluginConfig;
import com.hytaletravelers.playerlogger.data.PlayerData;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Service for sending Discord webhook notifications.
 */
public class DiscordWebhookService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().create();
    private static final int EMBED_COLOR_GREEN = 0x2ecc71;
    private static final int EMBED_COLOR_RED = 0xe74c3c;
    private static final int EMBED_COLOR_ORANGE = 0xf39c12;
    private static final int EMBED_COLOR_BLUE = 0x3498db;
    private static final int EMBED_COLOR_PURPLE = 0x9b59b6;

    private static final String CURSEFORGE_URL = "https://www.curseforge.com/hytale/mods/player-logger";

    private final PluginConfig config;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final String serverName;

    public DiscordWebhookService(@Nonnull PluginConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlayerLogger-Webhook");
            t.setDaemon(true);
            return t;
        });
        this.serverName = config.serverName.isEmpty() ? "Server" : config.serverName;
    }

    public void start() {
        if (!config.webhookEnabled || config.webhookUrl.isEmpty()) {
            return;
        }

        LOGGER.at(Level.INFO).log("[PlayerLogger] Discord webhook enabled");

        // Schedule daily leaderboard
        if (config.webhookDailyLeaderboard) {
            scheduleDailyLeaderboard();
        }
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private void scheduleDailyLeaderboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.toLocalDate().atTime(LocalTime.of(config.webhookDailyLeaderboardHour, 0));

        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        long initialDelay = Duration.between(now, nextRun).toMinutes();

        scheduler.scheduleAtFixedRate(
                this::sendDailyLeaderboard,
                initialDelay,
                TimeUnit.DAYS.toMinutes(1),
                TimeUnit.MINUTES
        );

        LOGGER.at(Level.INFO).log("[PlayerLogger] Daily leaderboard scheduled for %02d:00",
                config.webhookDailyLeaderboardHour);
    }

    public void onPlayerJoin(@Nonnull String username) {
        if (!config.webhookEnabled || !config.webhookPlayerJoin) return;

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(EMBED_COLOR_GREEN)
                .setTimestamp();
        applyLinks(builder, username + " Joined");

        sendEmbed(builder.build());
    }

    public void onPlayerLeave(@Nonnull String username, @Nullable String sessionTime) {
        if (!config.webhookEnabled || !config.webhookPlayerLeave) return;

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(EMBED_COLOR_ORANGE)
                .setTimestamp();

        if (sessionTime != null) {
            builder.setDescription("Session: " + sessionTime);
        }

        applyLinks(builder, username + " Left");
        sendEmbed(builder.build());
    }

    public void onPlayerDeath(@Nonnull String username, @Nullable String cause) {
        if (!config.webhookEnabled || !config.webhookPlayerDeath) return;

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(EMBED_COLOR_RED)
                .setTimestamp();

        if (cause != null && !cause.isEmpty()) {
            builder.setDescription("Cause: " + cause);
        }

        applyLinks(builder, username + " Died");
        sendEmbed(builder.build());
    }

    public void onPlayerKill(@Nonnull String killer, @Nonnull String victim) {
        if (!config.webhookEnabled || !config.webhookPlayerKill) return;

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(EMBED_COLOR_PURPLE)
                .setTimestamp();

        applyLinks(builder, killer + " killed " + victim);
        sendEmbed(builder.build());
    }

    public void sendDailyLeaderboard() {
        if (!config.webhookEnabled || !config.webhookDailyLeaderboard) return;

        PlayerDataManager manager = PlayerDataManager.getInstance();
        List<PlayerData> players = new ArrayList<>(manager.getAllPlayers());

        if (players.isEmpty()) {
            return;
        }

        // Sort by playtime
        players.sort(Comparator.comparingLong(PlayerData::getTotalPlaytimeSeconds).reversed());

        StringBuilder leaderboard = new StringBuilder();
        int count = Math.min(10, players.size());

        for (int i = 0; i < count; i++) {
            PlayerData p = players.get(i);
            String medal = switch (i) {
                case 0 -> ":first_place:";
                case 1 -> ":second_place:";
                case 2 -> ":third_place:";
                default -> "**#" + (i + 1) + "**";
            };
            leaderboard.append(medal)
                    .append(" **").append(p.getUsername()).append("**")
                    .append(" - ").append(p.getFormattedPlaytime())
                    .append(" | K: ").append(p.getKillCount())
                    .append(" D: ").append(p.getDeathCount())
                    .append("\n");
        }

        int totalPlayers = manager.getPlayerCount();
        int onlineNow = manager.getOnlineCount();

        EmbedBuilder builder = new EmbedBuilder()
                .setDescription(leaderboard.toString())
                .addField("Total Players", String.valueOf(totalPlayers), true)
                .addField("Online Now", String.valueOf(onlineNow), true)
                .setColor(EMBED_COLOR_BLUE)
                .setTimestamp();

        applyLinks(builder, "Daily Leaderboard");
        sendEmbed(builder.build());
    }

    private String getStatsUrl() {
        String encodedName = serverName.replace(" ", "%20");
        return "https://hytaletravelers.com/stats/" + encodedName;
    }

    private EmbedBuilder applyLinks(EmbedBuilder builder, String title) {
        builder.setTitle(title);
        builder.setUrl(getStatsUrl());

        if (config.webhookShowBranding) {
            builder.setAuthor("Powered by PlayerLogger", CURSEFORGE_URL);
        }

        builder.setFooter(serverName);
        return builder;
    }

    private void sendEmbed(@Nonnull Map<String, Object> embed) {
        if (config.webhookUrl.isEmpty()) return;

        scheduler.execute(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("embeds", List.of(embed));

                String json = GSON.toJson(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    LOGGER.at(Level.WARNING).log("[PlayerLogger] Webhook failed: HTTP %d", response.statusCode());
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Failed to send webhook");
            }
        });
    }

    private static class EmbedBuilder {
        private final Map<String, Object> embed = new HashMap<>();
        private final List<Map<String, Object>> fields = new ArrayList<>();

        public EmbedBuilder setTitle(String title) {
            embed.put("title", title);
            return this;
        }

        public EmbedBuilder setUrl(String url) {
            embed.put("url", url);
            return this;
        }

        public EmbedBuilder setDescription(String description) {
            embed.put("description", description);
            return this;
        }

        public EmbedBuilder setColor(int color) {
            embed.put("color", color);
            return this;
        }

        public EmbedBuilder addField(String name, String value, boolean inline) {
            Map<String, Object> field = new HashMap<>();
            field.put("name", name);
            field.put("value", value);
            field.put("inline", inline);
            fields.add(field);
            return this;
        }

        public EmbedBuilder setFooter(String text) {
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", text);
            embed.put("footer", footer);
            return this;
        }

        public EmbedBuilder setFooter(String text, String iconUrl) {
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", text);
            if (iconUrl != null && !iconUrl.isEmpty()) {
                footer.put("icon_url", iconUrl);
            }
            embed.put("footer", footer);
            return this;
        }

        public EmbedBuilder setAuthor(String name, String url) {
            Map<String, Object> author = new HashMap<>();
            author.put("name", name);
            if (url != null && !url.isEmpty()) {
                author.put("url", url);
            }
            embed.put("author", author);
            return this;
        }

        public EmbedBuilder setTimestamp() {
            embed.put("timestamp", java.time.Instant.now().toString());
            return this;
        }

        public Map<String, Object> build() {
            if (!fields.isEmpty()) {
                embed.put("fields", fields);
            }
            return embed;
        }
    }
}
