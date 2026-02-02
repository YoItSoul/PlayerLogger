package com.hytaletravelers.playerlogger.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hytaletravelers.playerlogger.data.PlayerData;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Pushes player data to an external URL periodically.
 * Works behind NAT/firewalls - no open ports needed.
 */
public class DataPushService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final String pushUrl;
    private final int intervalSeconds;
    private final String serverName;
    private final boolean publicListing;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    public DataPushService(String pushUrl, int intervalSeconds, String serverName, boolean publicListing) {
        this.pushUrl = pushUrl;
        this.intervalSeconds = intervalSeconds;
        this.serverName = serverName;
        this.publicListing = publicListing;
        this.gson = new GsonBuilder().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlayerLogger-DataPush");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (pushUrl == null || pushUrl.isEmpty()) {
            LOGGER.at(Level.WARNING).log("[PlayerLogger] Push mode enabled but no pushUrl configured!");
            LOGGER.at(Level.WARNING).log("[PlayerLogger] Set pushUrl in config.json to enable data sync");
            return;
        }

        LOGGER.at(Level.INFO).log("[PlayerLogger] Push mode started - syncing every %d seconds", intervalSeconds);
        LOGGER.at(Level.INFO).log("[PlayerLogger] Pushing to: %s", pushUrl);

        scheduler.scheduleAtFixedRate(this::pushData, 5, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.at(Level.INFO).log("[PlayerLogger] Push service stopped");
    }

    private void pushData() {
        try {
            PlayerDataManager manager = PlayerDataManager.getInstance();
            if (manager == null) {
                return;
            }

            // Build the payload
            Map<String, Object> payload = new LinkedHashMap<>();

            // Stats
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalPlayers", manager.getPlayerCount());
            stats.put("onlinePlayers", manager.getOnlineCount());

            long totalPlaytime = 0;
            float totalDamage = 0;
            int totalPlayerKills = 0;
            int totalMobKills = 0;
            int totalDeaths = 0;
            int totalBlocksPlaced = 0;
            int totalBlocksBroken = 0;
            List<Map<String, Object>> playerList = new ArrayList<>();

            for (PlayerData pd : manager.getAllPlayers()) {
                totalPlaytime += pd.getTotalWithCurrentSession();
                totalDamage += pd.getDamageDealt();
                totalPlayerKills += pd.getPlayerKills();
                totalMobKills += pd.getMobKills();
                totalDeaths += pd.getDeathCount();
                totalBlocksPlaced += pd.getBlocksPlaced();
                totalBlocksBroken += pd.getBlocksBroken();

                Map<String, Object> playerEntry = getStringObjectMap(pd);
                playerList.add(playerEntry);
            }

            // Sort by playtime descending
            playerList.sort((a, b) -> Long.compare(
                (Long) b.get("playtimeSeconds"),
                (Long) a.get("playtimeSeconds")
            ));

            stats.put("totalPlaytimeSeconds", totalPlaytime);
            stats.put("totalDamageDealt", totalDamage);
            stats.put("totalPlayerKills", totalPlayerKills);
            stats.put("totalMobKills", totalMobKills);
            stats.put("totalDeaths", totalDeaths);
            stats.put("totalBlocksPlaced", totalBlocksPlaced);
            stats.put("totalBlocksBroken", totalBlocksBroken);

            payload.put("stats", stats);
            payload.put("players", playerList);
            payload.put("lastUpdated", System.currentTimeMillis());

            // Include custom server name if configured
            if (serverName != null && !serverName.isEmpty()) {
                payload.put("serverName", serverName);
            }

            // Include public listing preference
            payload.put("publicListing", publicListing);

            // Send the data
            String json = gson.toJson(payload);
            sendPost(json);

        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Failed to push data");
        }
    }

    @NonNullDecl
    private static Map<String, Object> getStringObjectMap(PlayerData pd) {
        Map<String, Object> playerEntry = new LinkedHashMap<>();
        playerEntry.put("uuid", pd.getUuid().toString());
        playerEntry.put("username", pd.getUsername());
        playerEntry.put("playtimeSeconds", pd.getTotalWithCurrentSession());
        playerEntry.put("playtimeFormatted", pd.getFormattedPlaytime());
        playerEntry.put("online", pd.isOnline());
        playerEntry.put("damageDealt", pd.getDamageDealt());
        playerEntry.put("playerKills", pd.getPlayerKills());
        playerEntry.put("mobKills", pd.getMobKills());
        playerEntry.put("deathCount", pd.getDeathCount());
        playerEntry.put("blocksPlaced", pd.getBlocksPlaced());
        playerEntry.put("blocksBroken", pd.getBlocksBroken());
        return playerEntry;
    }

    private void sendPost(String json) throws IOException {
        sendPostWithRedirect(pushUrl, json, 0);
    }

    private void sendPostWithRedirect(String urlString, String json, int redirectCount) throws IOException {
        if (redirectCount > 5) {
            LOGGER.at(Level.WARNING).log("[PlayerLogger] Too many redirects");
            return;
        }

        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setInstanceFollowRedirects(false); // Handle manually
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "PlayerLogger/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            // Handle redirects
            if (responseCode == 301 || responseCode == 302 || responseCode == 307 || responseCode == 308) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null) {
                    LOGGER.at(Level.INFO).log("[PlayerLogger] Following redirect to: %s", newUrl);
                    conn.disconnect();
                    sendPostWithRedirect(newUrl, json, redirectCount + 1);
                    return;
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                LOGGER.at(Level.FINE).log("[PlayerLogger] Data pushed successfully");
            } else {
                LOGGER.at(Level.WARNING).log("[PlayerLogger] Push failed with status: %d", responseCode);
            }

        } finally {
            conn.disconnect();
        }
    }

    public String getPushUrl() {
        return pushUrl;
    }
}
