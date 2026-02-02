package com.hytaletravelers.playerlogger.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytaletravelers.playerlogger.PlayerLoggerPlugin;
import com.hytaletravelers.playerlogger.data.PlayerData;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;
import com.hytaletravelers.playerlogger.webhook.DiscordWebhookService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * ECS system that tracks player deaths using KillFeedEvent.DecedentMessage.
 * This event fires on the entity that died, providing reliable death tracking.
 */
public class DeathTrackingSystem extends EntityEventSystem<EntityStore, KillFeedEvent.DecedentMessage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public DeathTrackingSystem() {
        super(KillFeedEvent.DecedentMessage.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull KillFeedEvent.DecedentMessage event
    ) {
        if (event.isCancelled()) {
            return;
        }

        try {
            // Get the entity that died (this event fires ON the dead entity)
            Ref<EntityStore> deadEntityRef = archetypeChunk.getReferenceTo(index);
            if (deadEntityRef == null || !deadEntityRef.isValid()) {
                return;
            }

            // Check if the dead entity is a player
            PlayerRef playerRef = store.getComponent(deadEntityRef, PlayerRef.getComponentType());
            if (playerRef == null || !playerRef.isValid()) {
                return; // Not a player death, ignore
            }

            UUID playerUuid = playerRef.getUuid();
            if (playerUuid == null) {
                return;
            }

            PlayerData playerData = PlayerDataManager.getInstance().get(playerUuid);
            if (playerData != null) {
                playerData.incrementDeathCount();
                LOGGER.at(Level.INFO).log("[PlayerLogger] %s died (Deaths: %d)",
                    playerData.getUsername(), playerData.getDeathCount());

                // Send webhook notification
                DiscordWebhookService webhook = PlayerLoggerPlugin.getInstance().getWebhookService();
                if (webhook != null) {
                    webhook.onPlayerDeath(playerData.getUsername(), null);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Error tracking death");
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
