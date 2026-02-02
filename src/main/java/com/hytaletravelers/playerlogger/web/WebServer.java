package com.hytaletravelers.playerlogger.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hytaletravelers.playerlogger.data.PlayerData;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * Simple HTTP server exposing player data as JSON API.
 */
public class WebServer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final HttpServer server;
    private final Gson gson;
    private final String bindAddress;
    private final int port;

    public WebServer(String bindAddress, int port) throws IOException {
        this.bindAddress = bindAddress;
        this.port = port;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);

        server.createContext("/api/players", this::handlePlayers);
        server.createContext("/api/stats", this::handleStats);
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        LOGGER.at(Level.INFO).log("[PlayerLogger] Web API started on %s:%d", bindAddress, port);
        LOGGER.at(Level.INFO).log("[PlayerLogger] Endpoints: /api/players, /api/stats");
    }

    public void stop() {
        server.stop(0);
        LOGGER.at(Level.INFO).log("[PlayerLogger] Web API stopped");
    }

    public int getPort() {
        return port;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    private void handlePlayers(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        setCorsHeaders(exchange);

        PlayerDataManager manager = PlayerDataManager.getInstance();
        List<PlayerResponse> playerList = new ArrayList<>();

        for (PlayerData pd : manager.getAllPlayers()) {
            playerList.add(new PlayerResponse(
                pd.getUuid().toString(),
                pd.getUsername(),
                pd.getTotalWithCurrentSession(),
                pd.getFormattedPlaytime(),
                pd.isOnline(),
                pd.getDamageDealt(),
                pd.getPlayerKills(),
                pd.getMobKills(),
                pd.getDeathCount(),
                pd.getBlocksPlaced(),
                pd.getBlocksBroken()
            ));
        }

        playerList.sort((a, b) -> Long.compare(b.playtimeSeconds, a.playtimeSeconds));

        String json = gson.toJson(playerList);
        sendResponse(exchange, 200, json);
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        setCorsHeaders(exchange);

        PlayerDataManager manager = PlayerDataManager.getInstance();

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
        for (PlayerData pd : manager.getAllPlayers()) {
            totalPlaytime += pd.getTotalWithCurrentSession();
            totalDamage += pd.getDamageDealt();
            totalPlayerKills += pd.getPlayerKills();
            totalMobKills += pd.getMobKills();
            totalDeaths += pd.getDeathCount();
            totalBlocksPlaced += pd.getBlocksPlaced();
            totalBlocksBroken += pd.getBlocksBroken();
        }
        stats.put("totalPlaytimeSeconds", totalPlaytime);
        stats.put("totalDamageDealt", totalDamage);
        stats.put("totalPlayerKills", totalPlayerKills);
        stats.put("totalMobKills", totalMobKills);
        stats.put("totalDeaths", totalDeaths);
        stats.put("totalBlocksPlaced", totalBlocksPlaced);
        stats.put("totalBlocksBroken", totalBlocksBroken);

        String json = gson.toJson(stats);
        sendResponse(exchange, 200, json);
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static class PlayerResponse {
        final String uuid;
        final String username;
        final long playtimeSeconds;
        final String playtimeFormatted;
        final boolean online;
        final float damageDealt;
        final int playerKills;
        final int mobKills;
        final int deathCount;
        final int blocksPlaced;
        final int blocksBroken;

        PlayerResponse(String uuid, String username, long playtimeSeconds, String playtimeFormatted,
                       boolean online, float damageDealt, int playerKills, int mobKills, int deathCount,
                       int blocksPlaced, int blocksBroken) {
            this.uuid = uuid;
            this.username = username;
            this.playtimeSeconds = playtimeSeconds;
            this.playtimeFormatted = playtimeFormatted;
            this.online = online;
            this.damageDealt = damageDealt;
            this.playerKills = playerKills;
            this.mobKills = mobKills;
            this.deathCount = deathCount;
            this.blocksPlaced = blocksPlaced;
            this.blocksBroken = blocksBroken;
        }
    }
}
