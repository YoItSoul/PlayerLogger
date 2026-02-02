package com.hytaletravelers.playerlogger.update;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for plugin updates via the HytaleTravelers API.
 */
public class UpdateChecker {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static String CURRENT_VERSION = loadVersion();

    /**
     * Set the current version (called by plugin if manifest reading fails).
     */
    public static void setCurrentVersion(String version) {
        if (version != null && !version.isEmpty()) {
            CURRENT_VERSION = version;
        }
    }

    private static final String VERSION_CHECK_URL = "https://api.hytaletravelers.com/plugins/playerlogger/version";

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final AtomicReference<UpdateStatus> cachedStatus = new AtomicReference<>(UpdateStatus.UNKNOWN);
    private static volatile String latestVersion = null;
    private static volatile long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 3600000;

    /**
     * Update status enum.
     */
    public enum UpdateStatus {
        LATEST("(LATEST)", "#2ecc71"),            // Green - matches CurseForge
        DEV_BUILD("(+)", "#3498db"),              // Blue - ahead of CurseForge
        OUTDATED("(OUTDATED)", "#f39c12"),        // Orange - behind CurseForge
        UNKNOWN("(?)", "#6e7da1"),                // Gray - couldn't reach API
        CHECKING("...", "#6e7da1");               // Gray

        private final String displayText;
        private final String color;

        UpdateStatus(String displayText, String color) {
            this.displayText = displayText;
            this.color = color;
        }

        public String getDisplayText() {
            return displayText;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * Get the cached update status (non-blocking).
     */
    @Nonnull
    public static UpdateStatus getStatus() {
        // Trigger a check if we haven't checked recently
        long now = System.currentTimeMillis();
        if (now - lastCheckTime > CHECK_INTERVAL_MS) {
            checkForUpdatesAsync();
        }
        return cachedStatus.get();
    }

    /**
     * Get the latest version string (may be null if unknown).
     */
    @Nullable
    public static String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Get the current installed version.
     */
    @Nonnull
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    /**
     * Check for updates asynchronously.
     */
    public static void checkForUpdatesAsync() {
        lastCheckTime = System.currentTimeMillis();
        cachedStatus.set(UpdateStatus.CHECKING);

        CompletableFuture.runAsync(() -> {
            try {
                // Try the HytaleTravelers API first
                String latest = fetchLatestVersion();

                if (latest != null) {
                    latestVersion = latest;
                    int comparison = compareVersions(CURRENT_VERSION, latest);

                    if (comparison == 0) {
                        // Current matches latest
                        cachedStatus.set(UpdateStatus.LATEST);
                    } else if (comparison > 0) {
                        // Current is newer than latest (dev build)
                        cachedStatus.set(UpdateStatus.DEV_BUILD);
                        LOGGER.at(Level.INFO).log("[PlayerLogger] Running dev build: %s (latest: %s)", CURRENT_VERSION, latest);
                    } else {
                        // Current is older than latest (outdated)
                        cachedStatus.set(UpdateStatus.OUTDATED);
                        LOGGER.at(Level.INFO).log("[PlayerLogger] Update available: %s (current: %s)", latest, CURRENT_VERSION);
                    }
                } else {
                    LOGGER.at(Level.WARNING).log("[PlayerLogger] Could not fetch latest version from API");
                    cachedStatus.set(UpdateStatus.UNKNOWN);
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("[PlayerLogger] Failed to check for updates: %s", e.getMessage());
                cachedStatus.set(UpdateStatus.UNKNOWN);
            }
        });
    }

    /**
     * Fetch the latest version from the API.
     */
    @Nullable
    private static String fetchLatestVersion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERSION_CHECK_URL))
                    .header("User-Agent", "PlayerLogger/" + CURRENT_VERSION)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                if (json != null && json.has("version")) {
                    return json.get("version").getAsString();
                }
            }
        } catch (Exception e) {
            // API not available, that's fine
        }

        return null;
    }

    /**
     * Compare version strings (semver-like comparison).
     * Returns: positive if current > latest, 0 if equal, negative if current < latest
     */
    private static int compareVersions(String current, String latest) {
        try {
            int[] currentParts = parseVersion(current);
            int[] latestParts = parseVersion(latest);

            for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
                int currentPart = i < currentParts.length ? currentParts[i] : 0;
                int latestPart = i < latestParts.length ? latestParts[i] : 0;

                if (currentPart > latestPart) {
                    return 1;  // current is newer
                } else if (currentPart < latestPart) {
                    return -1; // current is older
                }
            }
            return 0; // Same version
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Parse a version string into integer parts.
     */
    private static int[] parseVersion(String version) {
        // Remove common prefixes like 'v' or 'V'
        version = version.replaceFirst("^[vV]", "");

        // Split by dots and parse
        String[] parts = version.split("\\.");
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            // Extract just the numeric portion (handles things like "1.0.0-beta")
            Matcher m = Pattern.compile("^(\\d+)").matcher(parts[i]);
            if (m.find()) {
                result[i] = Integer.parseInt(m.group(1));
            }
        }

        return result;
    }

    /**
     * Force an immediate update check (blocking).
     */
    public static void checkNow() {
        checkForUpdatesAsync();
    }

    /**
     * Load the plugin version from build-info.properties.
     * This file is generated by Gradle with the full version number.
     */
    private static String loadVersion() {
        try (InputStream is = UpdateChecker.class.getClassLoader().getResourceAsStream("build-info.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String ver = props.getProperty("version");
                if (ver != null && !ver.isEmpty() && !ver.contains("$")) {
                    return ver;
                }
            }
        } catch (Exception e) {
            // Fall back to unknown
        }
        return "unknown";
    }
}
