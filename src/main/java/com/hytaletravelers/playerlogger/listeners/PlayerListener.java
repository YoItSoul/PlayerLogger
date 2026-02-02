package com.hytaletravelers.playerlogger.listeners;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hytaletravelers.playerlogger.PlayerLoggerPlugin;
import com.hytaletravelers.playerlogger.data.PlayerData;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;
import com.hytaletravelers.playerlogger.webhook.DiscordWebhookService;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Tracks player connections and playtime.
 * Kill tracking is handled by DamageTrackingSystem.
 */
public class PlayerListener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public void register(EventRegistry eventBus) {
        eventBus.register(PlayerConnectEvent.class, this::onPlayerConnect);
        eventBus.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        LOGGER.at(Level.INFO).log("[PlayerLogger] Player tracking enabled");
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        try {
            PlayerRef ref = event.getPlayerRef();
            if (ref == null || !ref.isValid()) return;

            UUID uuid = ref.getUuid();
            String username = ref.getUsername();
            if (uuid == null || username == null) return;

            PlayerData data = PlayerDataManager.getInstance().getOrCreate(uuid, username);
            data.startSession();

            LOGGER.at(Level.INFO).log("[PlayerLogger] %s joined (Total: %s)", username, data.getFormattedPlaytime());

            // Send webhook notification
            DiscordWebhookService webhook = PlayerLoggerPlugin.getInstance().getWebhookService();
            if (webhook != null) {
                webhook.onPlayerJoin(username);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Error handling player connect");
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        try {
            PlayerRef ref = event.getPlayerRef();
            if (ref == null) return;

            UUID uuid = ref.getUuid();
            if (uuid == null) return;

            PlayerData data = PlayerDataManager.getInstance().get(uuid);
            if (data != null) {
                String sessionTime = data.getFormattedSessionTime();
                data.endSession();
                LOGGER.at(Level.INFO).log("[PlayerLogger] %s left (Total: %s)", data.getUsername(), data.getFormattedPlaytime());
                PlayerDataManager.getInstance().save();

                // Send webhook notification
                DiscordWebhookService webhook = PlayerLoggerPlugin.getInstance().getWebhookService();
                if (webhook != null) {
                    webhook.onPlayerLeave(data.getUsername(), sessionTime);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Error handling player disconnect");
        }
    }
}
