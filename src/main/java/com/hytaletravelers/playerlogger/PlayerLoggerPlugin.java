package com.hytaletravelers.playerlogger;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hytaletravelers.playerlogger.commands.ListCommand;
import com.hytaletravelers.playerlogger.commands.PlayerLoggerPluginCommand;
import com.hytaletravelers.playerlogger.config.PluginConfig;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;
import com.hytaletravelers.playerlogger.listeners.PlayerListener;
import com.hytaletravelers.playerlogger.systems.BlockBreakTrackingSystem;
import com.hytaletravelers.playerlogger.systems.BlockPlaceTrackingSystem;
import com.hytaletravelers.playerlogger.systems.DamageTrackingSystem;
import com.hytaletravelers.playerlogger.systems.DeathTrackingSystem;
import com.hytaletravelers.playerlogger.update.UpdateChecker;
import com.hytaletravelers.playerlogger.web.DataPushService;
import com.hytaletravelers.playerlogger.web.WebServer;
import com.hytaletravelers.playerlogger.webhook.DiscordWebhookService;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * PlayerLogger - Track player statistics with cloud sync and web dashboard.
 *
 * Features:
 * - Automatic player tracking (playtime, kills, deaths, blocks)
 * - Cloud sync to HytaleTravelers API
 * - In-game dashboard UI
 * - Optional self-hosted API
 *
 * @see <a href="https://hytaletravelers.com/stats">Web Dashboard</a>
 * @see <a href="https://curseforge.com/hytale/mods/player-logger">CurseForge</a>
 */
public class PlayerLoggerPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static PlayerLoggerPlugin instance;

    private PluginConfig config;
    private DataPushService pushService;
    private WebServer webServer;
    private DiscordWebhookService webhookService;

    public PlayerLoggerPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static PlayerLoggerPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return config;
    }

    public DiscordWebhookService getWebhookService() {
        return webhookService;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[PlayerLogger] Initializing...");

        Path dataFolder = getDataDirectory();

        // Load configuration
        config = PluginConfig.load(dataFolder);

        // Initialize data manager
        PlayerDataManager.init(dataFolder);

        // Register commands
        getCommandRegistry().registerCommand(new PlayerLoggerPluginCommand());
        getCommandRegistry().registerCommand(new ListCommand());

        // Register event listeners
        new PlayerListener().register(getEventRegistry());

        // Register ECS systems for stat tracking
        registerTrackingSystems();

        // Start optional services
        startPushService();
        startWebServer();
        startWebhookService();

        LOGGER.at(Level.INFO).log("[PlayerLogger] Setup complete");
    }

    @Override
    protected void start() {
        int playerCount = PlayerDataManager.getInstance().getPlayerCount();
        LOGGER.at(Level.INFO).log("[PlayerLogger] Started v%s - Tracking %d players",
                UpdateChecker.getCurrentVersion(), playerCount);
        LOGGER.at(Level.INFO).log("[PlayerLogger] Commands: /pl help, /list");

        // Check for updates
        UpdateChecker.checkForUpdatesAsync();
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[PlayerLogger] Shutting down...");

        if (pushService != null) {
            pushService.stop();
        }

        if (webServer != null) {
            webServer.stop();
        }

        if (webhookService != null) {
            webhookService.stop();
        }

        PlayerDataManager.getInstance().save();
        instance = null;

        LOGGER.at(Level.INFO).log("[PlayerLogger] Goodbye!");
    }

    private void registerTrackingSystems() {
        getEntityStoreRegistry().registerSystem(new DamageTrackingSystem());
        getEntityStoreRegistry().registerSystem(new DeathTrackingSystem());
        getEntityStoreRegistry().registerSystem(new BlockBreakTrackingSystem());
        getEntityStoreRegistry().registerSystem(new BlockPlaceTrackingSystem());
    }

    private void startPushService() {
        if (!config.pushEnabled) {
            return;
        }

        pushService = new DataPushService(
                config.pushUrl,
                config.pushIntervalSeconds,
                config.serverName,
                config.publicListing
        );
        pushService.start();

        LOGGER.at(Level.INFO).log("[PlayerLogger] Cloud sync enabled -> %s", config.pushUrl);
    }

    private void startWebServer() {
        if (!config.webEnabled) {
            return;
        }

        try {
            webServer = new WebServer(config.webBindAddress, config.webPort);
            webServer.start();
            LOGGER.at(Level.INFO).log("[PlayerLogger] Local API: http://%s:%d/api/players",
                    config.webBindAddress, config.webPort);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log(
                    "[PlayerLogger] Failed to start web server on port %d", config.webPort);
        }
    }

    private void startWebhookService() {
        if (!config.webhookEnabled || config.webhookUrl.isEmpty()) {
            return;
        }

        webhookService = new DiscordWebhookService(config);
        webhookService.start();
    }
}
