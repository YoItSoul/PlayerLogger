package com.hytaletravelers.playerlogger.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Manages player data storage, retrieval, and persistence.
 * Thread-safe for concurrent access.
 */
public class PlayerDataManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static PlayerDataManager instance;

    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final Path dataFile;
    private final Gson gson;

    private PlayerDataManager(Path pluginDataFolder) {
        this.dataFile = pluginDataFolder.resolve("players.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public static void init(Path pluginDataFolder) {
        instance = new PlayerDataManager(pluginDataFolder);
    }

    public static PlayerDataManager getInstance() {
        return instance;
    }

    /**
     * Get or create player data for the given UUID.
     */
    public PlayerData getOrCreate(UUID uuid, String username) {
        return players.computeIfAbsent(uuid, id -> new PlayerData(id, username));
    }

    /**
     * Get player data by UUID, or null if not found.
     */
    public PlayerData get(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * Get player data by username (case-insensitive).
     */
    public PlayerData getByUsername(String username) {
        return players.values().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all tracked players.
     */
    public Collection<PlayerData> getAllPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    /**
     * Get total number of tracked players.
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * Get number of currently online players.
     */
    public int getOnlineCount() {
        return (int) players.values().stream().filter(PlayerData::isOnline).count();
    }

    /**
     * Remove a player completely from tracking.
     */
    public boolean removePlayer(UUID uuid) {
        PlayerData removed = players.remove(uuid);
        if (removed != null) {
            save();
            LOGGER.at(Level.INFO).log("[PlayerLogger] Removed player: %s", removed.getUsername());
            return true;
        }
        return false;
    }

    /**
     * Remove a player by username.
     */
    public boolean removePlayer(String username) {
        PlayerData player = getByUsername(username);
        if (player != null) {
            return removePlayer(player.getUuid());
        }
        return false;
    }

    /**
     * Reset specific stats for a player.
     */
    public boolean resetPlayerStats(String username, StatCategory category) {
        PlayerData player = getByUsername(username);
        if (player != null) {
            applyStatReset(player, category);
            save();
            LOGGER.at(Level.INFO).log("[PlayerLogger] Reset %s stats for %s", category.name().toLowerCase(), username);
            return true;
        }
        return false;
    }

    /**
     * Reset specific stats for all players.
     */
    public int resetAllPlayersStats(StatCategory category) {
        int count = 0;
        for (PlayerData player : players.values()) {
            applyStatReset(player, category);
            count++;
        }
        if (count > 0) {
            save();
            LOGGER.at(Level.INFO).log("[PlayerLogger] Reset %s stats for %d players", category.name().toLowerCase(), count);
        }
        return count;
    }

    /**
     * Wipe all player data.
     */
    public int wipeAllPlayers() {
        int count = players.size();
        players.clear();
        save();
        LOGGER.at(Level.INFO).log("[PlayerLogger] Wiped all player data (%d players)", count);
        return count;
    }

    private void applyStatReset(PlayerData player, StatCategory category) {
        switch (category) {
            case ALL -> player.resetAllStats();
            case COMBAT -> player.resetCombatStats();
            case BLOCKS -> player.resetBlockStats();
            case PLAYTIME -> player.resetPlaytime();
            case KILLS -> {
                player.setPlayerKills(0);
                player.setMobKills(0);
            }
            case DEATHS -> player.setDeathCount(0);
            case DAMAGE -> player.setDamageDealt(0);
        }
    }

    /**
     * Save all player data to disk.
     */
    public void save() {
        try {
            Files.createDirectories(dataFile.getParent());

            List<SavedPlayer> toSave = new ArrayList<>();
            for (PlayerData pd : players.values()) {
                toSave.add(SavedPlayer.from(pd));
            }

            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                gson.toJson(toSave, writer);
            }

            LOGGER.at(Level.INFO).log("[PlayerLogger] Saved %d players", toSave.size());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Failed to save player data");
        }
    }

    private void load() {
        if (!Files.exists(dataFile)) {
            LOGGER.at(Level.INFO).log("[PlayerLogger] No existing player data found");
            return;
        }

        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Type listType = new TypeToken<List<SavedPlayer>>() {}.getType();
            List<SavedPlayer> loaded = gson.fromJson(reader, listType);

            if (loaded != null) {
                int migrated = 0;
                for (SavedPlayer sp : loaded) {
                    if (sp == null || sp.uuid == null) continue;

                    try {
                        PlayerData pd = sp.toPlayerData();
                        players.put(pd.getUuid(), pd);

                            if (sp.needsMigration()) {
                            migrated++;
                        }
                    } catch (Exception e) {
                        LOGGER.at(Level.WARNING).log("[PlayerLogger] Skipping corrupted player entry");
                    }
                }

                LOGGER.at(Level.INFO).log("[PlayerLogger] Loaded %d players", players.size());

                if (migrated > 0) {
                    save();
                    LOGGER.at(Level.INFO).log("[PlayerLogger] Migrated %d player records to new format", migrated);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[PlayerLogger] Failed to load player data");
        }
    }

    /**
     * Categories of statistics that can be reset.
     */
    public enum StatCategory {
        ALL("all stats"),
        COMBAT("combat stats (kills, deaths, damage)"),
        BLOCKS("block stats (placed, broken)"),
        PLAYTIME("playtime"),
        KILLS("kills (pvp and pve)"),
        DEATHS("deaths"),
        DAMAGE("damage dealt");

        private final String description;

        StatCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static StatCategory fromString(String name) {
            try {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * DTO for JSON serialization. Missing fields default to zero.
     */
    private static class SavedPlayer {
        private static final int CURRENT_VERSION = 1;

        Integer version;  // Nullable to detect old data without version field
        String uuid;
        String username;
        long playtimeSeconds;
        float damageDealt;
        int playerKills;
        int mobKills;
        int blocksPlaced;
        int blocksBroken;
        int deathCount;

        static SavedPlayer from(PlayerData pd) {
            SavedPlayer sp = new SavedPlayer();
            sp.version = CURRENT_VERSION;
            sp.uuid = pd.getUuid().toString();
            sp.username = pd.getUsername();
            sp.playtimeSeconds = pd.getTotalWithCurrentSession();
            sp.damageDealt = pd.getDamageDealt();
            sp.playerKills = pd.getPlayerKills();
            sp.mobKills = pd.getMobKills();
            sp.blocksPlaced = pd.getBlocksPlaced();
            sp.blocksBroken = pd.getBlocksBroken();
            sp.deathCount = pd.getDeathCount();
            return sp;
        }

        PlayerData toPlayerData() {
            UUID id = UUID.fromString(uuid);
            PlayerData pd = new PlayerData(id, username != null ? username : "Unknown");

            pd.setTotalPlaytimeSeconds(playtimeSeconds);
            pd.setDamageDealt(damageDealt);
            pd.setPlayerKills(playerKills);
            pd.setMobKills(mobKills);
            pd.setBlocksPlaced(blocksPlaced);
            pd.setBlocksBroken(blocksBroken);
            pd.setDeathCount(deathCount);

            return pd;
        }

        /**
         * Check if this record needs migration (missing version or old version).
         */
        boolean needsMigration() {
            return version == null || version < CURRENT_VERSION;
        }
    }
}
