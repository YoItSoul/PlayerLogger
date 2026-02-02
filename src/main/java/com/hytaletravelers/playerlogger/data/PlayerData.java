package com.hytaletravelers.playerlogger.data;

import java.util.UUID;

/**
 * Represents a tracked player with their statistics.
 * Stores playtime, combat stats, and block interaction data.
 */
public class PlayerData {

    private final UUID uuid;
    private final String username;

    // Playtime tracking
    private long totalPlaytimeSeconds;
    private long sessionStartTime;

    // Combat stats
    private float damageDealt;
    private int playerKills;
    private int mobKills;
    private int deathCount;

    // Block stats
    private int blocksPlaced;
    private int blocksBroken;

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        resetAllStats();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void startSession() {
        this.sessionStartTime = System.currentTimeMillis();
    }

    public void endSession() {
        if (sessionStartTime > 0) {
            long sessionSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000;
            totalPlaytimeSeconds += sessionSeconds;
            sessionStartTime = 0;
        }
    }

    public boolean isOnline() {
        return sessionStartTime > 0;
    }

    public long getTotalPlaytimeSeconds() {
        return totalPlaytimeSeconds;
    }

    public void setTotalPlaytimeSeconds(long seconds) {
        this.totalPlaytimeSeconds = seconds;
    }

    public long getCurrentSessionSeconds() {
        if (sessionStartTime > 0) {
            return (System.currentTimeMillis() - sessionStartTime) / 1000;
        }
        return 0;
    }

    public long getTotalWithCurrentSession() {
        return totalPlaytimeSeconds + getCurrentSessionSeconds();
    }

    public String getFormattedPlaytime() {
        long total = getTotalWithCurrentSession();
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    public String getFormattedSessionTime() {
        long session = getCurrentSessionSeconds();
        long hours = session / 3600;
        long minutes = (session % 3600) / 60;
        long seconds = session % 60;
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public float getDamageDealt() {
        return damageDealt;
    }

    public void setDamageDealt(float damageDealt) {
        this.damageDealt = damageDealt;
    }

    public void addDamageDealt(float amount) {
        this.damageDealt += amount;
    }

    public int getPlayerKills() {
        return playerKills;
    }

    public void setPlayerKills(int playerKills) {
        this.playerKills = playerKills;
    }

    public void incrementPlayerKills() {
        this.playerKills++;
    }

    public int getMobKills() {
        return mobKills;
    }

    public void setMobKills(int mobKills) {
        this.mobKills = mobKills;
    }

    public void incrementMobKills() {
        this.mobKills++;
    }

    public int getKillCount() {
        return playerKills + mobKills;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
    }

    public void incrementDeathCount() {
        this.deathCount++;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }

    public void incrementBlocksPlaced() {
        this.blocksPlaced++;
    }

    public int getBlocksBroken() {
        return blocksBroken;
    }

    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = blocksBroken;
    }

    public void incrementBlocksBroken() {
        this.blocksBroken++;
    }

    /**
     * Reset all statistics to zero.
     */
    public void resetAllStats() {
        this.totalPlaytimeSeconds = 0;
        this.sessionStartTime = 0;
        this.damageDealt = 0;
        this.playerKills = 0;
        this.mobKills = 0;
        this.deathCount = 0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
    }

    /**
     * Reset combat-related statistics (kills, deaths, damage).
     */
    public void resetCombatStats() {
        this.damageDealt = 0;
        this.playerKills = 0;
        this.mobKills = 0;
        this.deathCount = 0;
    }

    /**
     * Reset block-related statistics (placed, broken).
     */
    public void resetBlockStats() {
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
    }

    /**
     * Reset playtime statistics.
     */
    public void resetPlaytime() {
        this.totalPlaytimeSeconds = 0;
    }
}
