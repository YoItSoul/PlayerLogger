package com.hytaletravelers.playerlogger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Plugin configuration loaded from config.json
 */
public class PluginConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_CONFIG_VERSION = 2;

    // Config version for migration tracking
    public String _configVersion = "Internal version number - do not modify";
    public int configVersion = CURRENT_CONFIG_VERSION;

    // Cloud sync - pushes player data to HytaleTravelers API
    public String _pushEnabled = "Enable cloud sync to HytaleTravelers (allows web dashboard)";
    public boolean pushEnabled = true;

    public String _pushUrl = "API endpoint for cloud sync (don't change unless self-hosting)";
    public String pushUrl = "https://api.hytaletravelers.com";

    public String _pushIntervalSeconds = "How often to sync data to the cloud (in seconds)";
    public int pushIntervalSeconds = 30;

    // Custom server identifier (optional)
    public String _serverName = "Custom display name for your server (e.g., 'play.myserver.com'). Leave empty to use your IP.";
    public String serverName = "";

    // Public server listing
    public String _publicListing = "Show your server on the public 'All Servers' page. Set false for privacy.";
    public boolean publicListing = true;

    // Local Web API (optional - only if you want to self-host)
    public String _webEnabled = "Host your own local JSON API (requires open port). Most users don't need this.";
    public boolean webEnabled = false;

    public String _webPort = "Port for local API server";
    public int webPort = 8080;

    public String _webBindAddress = "Bind address for local API (0.0.0.0 = all interfaces)";
    public String webBindAddress = "0.0.0.0";

    // Discord Webhook
    public String _webhookEnabled = "Enable Discord webhook notifications";
    public boolean webhookEnabled = false;

    public String _webhookUrl = "Discord webhook URL (get from Discord channel settings -> Integrations -> Webhooks)";
    public String webhookUrl = "";

    public String _webhookPlayerJoin = "Send notification when a player joins";
    public boolean webhookPlayerJoin = true;

    public String _webhookPlayerLeave = "Send notification when a player leaves";
    public boolean webhookPlayerLeave = true;

    public String _webhookPlayerDeath = "Send notification when a player dies";
    public boolean webhookPlayerDeath = true;

    public String _webhookPlayerKill = "Send notification when a player gets a PvP kill";
    public boolean webhookPlayerKill = true;

    public String _webhookDailyLeaderboard = "Send daily leaderboard summary";
    public boolean webhookDailyLeaderboard = true;

    public String _webhookDailyLeaderboardHour = "Hour to send daily leaderboard (0-23, server timezone)";
    public int webhookDailyLeaderboardHour = 12;

    public String _webhookShowBranding = "Show 'Powered by PlayerLogger' footer with CurseForge link in embeds";
    public boolean webhookShowBranding = true;

    /**
     * Load config from file, or create default if not exists.
     */
    public static PluginConfig load(Path dataFolder) {
        Path configFile = dataFolder.resolve("config.json");

        try {
            Files.createDirectories(dataFolder);

            if (Files.exists(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile)) {
                    PluginConfig config = GSON.fromJson(reader, PluginConfig.class);

                    if (config == null) {
                        config = new PluginConfig();
                    }

                    boolean needsMigration = config.configVersion < CURRENT_CONFIG_VERSION;
                    config.configVersion = CURRENT_CONFIG_VERSION;
                    config.save(configFile);

                    if (needsMigration) {
                        LOGGER.at(Level.INFO).log("[PlayerLogger] Config migrated to version %d", CURRENT_CONFIG_VERSION);
                    } else {
                        LOGGER.at(Level.INFO).log("[PlayerLogger] Loaded config");
                    }
                    return config;
                }
            } else {
                // Create default config
                PluginConfig config = new PluginConfig();
                config.save(configFile);
                LOGGER.at(Level.INFO).log("[PlayerLogger] Created default config.json");
                return config;
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Failed to load config, using defaults");
            PluginConfig config = new PluginConfig();
            try {
                config.save(configFile);
            } catch (Exception ignored) {}
            return config;
        }
    }

    public void save(Path configFile) {
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            writer.write("{\n");

            writer.write("  \"_configVersion\": \"" + _configVersion + "\",\n");
            writer.write("  \"configVersion\": " + configVersion + ",\n\n");

            writer.write("  \"_pushEnabled\": \"" + _pushEnabled + "\",\n");
            writer.write("  \"pushEnabled\": " + pushEnabled + ",\n\n");

            writer.write("  \"_pushUrl\": \"" + _pushUrl + "\",\n");
            writer.write("  \"pushUrl\": \"" + pushUrl + "\",\n\n");

            writer.write("  \"_pushIntervalSeconds\": \"" + _pushIntervalSeconds + "\",\n");
            writer.write("  \"pushIntervalSeconds\": " + pushIntervalSeconds + ",\n\n");

            writer.write("  \"_serverName\": \"" + _serverName + "\",\n");
            writer.write("  \"serverName\": \"" + serverName + "\",\n\n");

            writer.write("  \"_publicListing\": \"" + _publicListing + "\",\n");
            writer.write("  \"publicListing\": " + publicListing + ",\n\n");

            writer.write("  \"_webEnabled\": \"" + _webEnabled + "\",\n");
            writer.write("  \"webEnabled\": " + webEnabled + ",\n\n");

            writer.write("  \"_webPort\": \"" + _webPort + "\",\n");
            writer.write("  \"webPort\": " + webPort + ",\n\n");

            writer.write("  \"_webBindAddress\": \"" + _webBindAddress + "\",\n");
            writer.write("  \"webBindAddress\": \"" + webBindAddress + "\",\n\n");

            writer.write("  \"_webhookEnabled\": \"" + _webhookEnabled + "\",\n");
            writer.write("  \"webhookEnabled\": " + webhookEnabled + ",\n\n");

            writer.write("  \"_webhookUrl\": \"" + _webhookUrl + "\",\n");
            writer.write("  \"webhookUrl\": \"" + webhookUrl + "\",\n\n");

            writer.write("  \"_webhookPlayerJoin\": \"" + _webhookPlayerJoin + "\",\n");
            writer.write("  \"webhookPlayerJoin\": " + webhookPlayerJoin + ",\n\n");

            writer.write("  \"_webhookPlayerLeave\": \"" + _webhookPlayerLeave + "\",\n");
            writer.write("  \"webhookPlayerLeave\": " + webhookPlayerLeave + ",\n\n");

            writer.write("  \"_webhookPlayerDeath\": \"" + _webhookPlayerDeath + "\",\n");
            writer.write("  \"webhookPlayerDeath\": " + webhookPlayerDeath + ",\n\n");

            writer.write("  \"_webhookPlayerKill\": \"" + _webhookPlayerKill + "\",\n");
            writer.write("  \"webhookPlayerKill\": " + webhookPlayerKill + ",\n\n");

            writer.write("  \"_webhookDailyLeaderboard\": \"" + _webhookDailyLeaderboard + "\",\n");
            writer.write("  \"webhookDailyLeaderboard\": " + webhookDailyLeaderboard + ",\n\n");

            writer.write("  \"_webhookDailyLeaderboardHour\": \"" + _webhookDailyLeaderboardHour + "\",\n");
            writer.write("  \"webhookDailyLeaderboardHour\": " + webhookDailyLeaderboardHour + ",\n\n");

            writer.write("  \"_webhookShowBranding\": \"" + _webhookShowBranding + "\",\n");
            writer.write("  \"webhookShowBranding\": " + webhookShowBranding + "\n");

            writer.write("}\n");
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Failed to save config");
        }
    }
}
