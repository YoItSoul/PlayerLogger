package com.hytaletravelers.playerlogger.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytaletravelers.playerlogger.data.PlayerData;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * ECS system that tracks blocks placed by players.
 */
public class BlockPlaceTrackingSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public BlockPlaceTrackingSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event
    ) {
        if (event.isCancelled()) {
            return;
        }

        try {
            // Get entity reference from the archetype chunk
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            if (ref == null || !ref.isValid()) {
                return;
            }

            // Get the PlayerRef component to identify if this is a player
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null && playerRef.isValid()) {
                UUID uuid = playerRef.getUuid();
                if (uuid != null) {
                    PlayerData data = PlayerDataManager.getInstance().get(uuid);
                    if (data != null) {
                        data.incrementBlocksPlaced();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Error tracking block place");
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
