package com.hytaletravelers.playerlogger.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
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
 * ECS system that tracks damage dealt by players and detects kills.
 */
public class DamageTrackingSystem extends EntityEventSystem<EntityStore, Damage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public DamageTrackingSystem() {
        super(Damage.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage event
    ) {
        if (event.isCancelled()) {
            return;
        }

        Damage.Source source = event.getSource();

        if (source instanceof Damage.EntitySource entitySource) {
            try {
                var attackerRef = entitySource.getRef();
                if (attackerRef == null || !attackerRef.isValid()) {
                    return;
                }

                // Get the PlayerRef component from the entity that dealt damage
                PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
                if (attackerPlayerRef != null && attackerPlayerRef.isValid()) {
                    UUID attackerUuid = attackerPlayerRef.getUuid();
                    if (attackerUuid != null) {
                        PlayerData attackerData = PlayerDataManager.getInstance().get(attackerUuid);
                        if (attackerData == null) {
                            return;
                        }

                        // Track damage dealt
                        float damageAmount = event.getAmount();
                        attackerData.addDamageDealt(damageAmount);

                        // Get the victim entity ref
                        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
                        if (victimRef == null || !victimRef.isValid()) {
                            return;
                        }

                        // Check if victim is a player
                        PlayerRef victimPlayerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
                        boolean victimIsPlayer = (victimPlayerRef != null && victimPlayerRef.isValid());

                        // Check if this damage is lethal (will kill the entity)
                        EntityStatMap victimStats = store.getComponent(victimRef, EntityStatMap.getComponentType());
                        if (victimStats != null) {
                            int healthIndex = DefaultEntityStatTypes.getHealth();
                            EntityStatValue healthStat = victimStats.get(healthIndex);
                            if (healthStat != null) {
                                float currentHealth = healthStat.get();

                                if (damageAmount >= currentHealth) {
                                    if (victimIsPlayer) {
                                        attackerData.incrementPlayerKills();
                                        String victimName = victimPlayerRef.getUsername();
                                        LOGGER.at(Level.INFO).log("[PlayerLogger] %s killed a PLAYER! (PvP: %d, PvE: %d)",
                                            attackerData.getUsername(), attackerData.getPlayerKills(), attackerData.getMobKills());

                                        // Send webhook notification for PvP kill
                                        DiscordWebhookService webhook = PlayerLoggerPlugin.getInstance().getWebhookService();
                                        if (webhook != null && victimName != null) {
                                            webhook.onPlayerKill(attackerData.getUsername(), victimName);
                                        }
                                    } else {
                                        attackerData.incrementMobKills();
                                        LOGGER.at(Level.INFO).log("[PlayerLogger] %s killed a MOB! (PvP: %d, PvE: %d)",
                                            attackerData.getUsername(), attackerData.getPlayerKills(), attackerData.getMobKills());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Error tracking damage");
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
