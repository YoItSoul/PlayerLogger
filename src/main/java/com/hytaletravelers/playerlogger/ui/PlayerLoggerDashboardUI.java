package com.hytaletravelers.playerlogger.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytaletravelers.playerlogger.PlayerLoggerPlugin;
import com.hytaletravelers.playerlogger.config.PluginConfig;
import com.hytaletravelers.playerlogger.data.PlayerData;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;
import com.hytaletravelers.playerlogger.update.UpdateChecker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Interactive dashboard UI for viewing player statistics.
 */
public class PlayerLoggerDashboardUI extends InteractiveCustomUIPage<PlayerLoggerDashboardUI.DashboardEventData> {

    // Path relative to Common/UI/Custom/
    private static final String LAYOUT = "playerlogger/Dashboard.ui";
    private static final String PLAYER_ENTRY = "playerlogger/PlayerEntry.ui";
    private static final String CURSEFORGE_URL = "https://www.curseforge.com/hytale/mods/player-logger";
    private static final String DISCORD_URL = "https://discord.gg/CnJuHve8gn";

    @Nonnull
    private final PlayerRef playerRef;

    @Nonnull
    private String searchQuery = "";
    @Nonnull
    private SortMode sortMode = SortMode.PLAYTIME;
    @Nullable
    private String selectedPlayer = null;

    public PlayerLoggerDashboardUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, DashboardEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        // Load the layout file
        cmd.append(LAYOUT);

        // Bind search input
        evt.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#SearchInput",
                EventData.of("@SearchQuery", "#SearchInput.Value"),
                false
        );

        // Bind sort buttons
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SortPlaytime", EventData.of("Sort", "playtime"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SortKills", EventData.of("Sort", "kills"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SortDeaths", EventData.of("Sort", "deaths"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SortOnline", EventData.of("Sort", "online"));

        // Bind back button
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Back", "true"));

        // Bind CurseForge button
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CurseForgeButton", EventData.of("CurseForge", "true"));

        // Bind Discord button
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DiscordButton", EventData.of("Discord", "true"));

        // Set header title from config
        PluginConfig config = PlayerLoggerPlugin.getInstance().getPluginConfig();
        String title = (config.serverName != null && !config.serverName.isEmpty())
                ? config.serverName
                : "PlayerLogger";
        cmd.set("#HeaderTitle.Text", title);

        // Set version info
        cmd.set("#VersionLabel.Text", "v" + UpdateChecker.getCurrentVersion());
        updateVersionStatus(cmd);

        // Build initial content
        updateHeader(cmd);
        buildPlayerList(cmd, evt);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull DashboardEventData data
    ) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();

        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
            this.selectedPlayer = null;
            buildPlayerList(cmd, evt);
            updateHeader(cmd);
            sendUpdate(cmd, evt, false);

        } else if (data.sort != null) {
            this.sortMode = SortMode.fromString(data.sort);
            this.selectedPlayer = null;
            buildPlayerList(cmd, evt);
            sendUpdate(cmd, evt, false);

        } else if (data.selectPlayer != null) {
            this.selectedPlayer = data.selectPlayer;
            showPlayerDetail(cmd);
            sendUpdate(cmd, evt, false);

        } else if (data.back != null) {
            this.selectedPlayer = null;
            cmd.set("#DetailView.Visible", false);
            cmd.set("#ListView.Visible", true);
            buildPlayerList(cmd, evt);
            sendUpdate(cmd, evt, false);

        } else if (data.curseForge != null) {
            // Send clickable link message to the player
            playerRef.sendMessage(
                    Message.raw("PlayerLogger on CurseForge: ")
                            .insert(Message.raw(CURSEFORGE_URL).link(CURSEFORGE_URL).color("#f16436"))
            );
            // Close the UI so they can click the link
            this.close();

        } else if (data.discord != null) {
            // Send clickable Discord link to the player
            playerRef.sendMessage(
                    Message.raw("PlayerLogger Discord: ")
                            .insert(Message.raw(DISCORD_URL).link(DISCORD_URL).color("#5865F2"))
            );
            // Close the UI so they can click the link
            this.close();
        }
    }

    private void updateHeader(@Nonnull UICommandBuilder cmd) {
        PlayerDataManager manager = PlayerDataManager.getInstance();
        int total = manager.getPlayerCount();
        int online = manager.getOnlineCount();
        cmd.set("#HeaderStats.Text", String.format("%d players tracked  |  %d online now", total, online));
    }

    private void updateVersionStatus(@Nonnull UICommandBuilder cmd) {
        UpdateChecker.UpdateStatus status = UpdateChecker.getStatus();
        cmd.set("#VersionStatus.Text", status.getDisplayText());
    }

    private void buildPlayerList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#PlayerList");
        cmd.set("#DetailView.Visible", false);
        cmd.set("#ListView.Visible", true);
        cmd.set("#ColumnHeaders.Visible", true);
        cmd.set("#SortRow.Visible", true);

        PlayerDataManager manager = PlayerDataManager.getInstance();
        List<PlayerData> players = new ArrayList<>(manager.getAllPlayers());

        // Apply search filter
        if (!searchQuery.isEmpty()) {
            players.removeIf(p -> !p.getUsername().toLowerCase().contains(searchQuery));
        }

        // Apply sorting
        players.sort(getSortComparator());

        if (players.isEmpty()) {
            cmd.appendInline("#PlayerList",
                    "Label { Text: \"No players found.\"; Anchor: (Height: 50); Style: (FontSize: 14, TextColor: #6e7da1, HorizontalAlignment: Center, VerticalAlignment: Center); }");
            return;
        }

        // Build player entries
        for (int i = 0; i < players.size(); i++) {
            PlayerData player = players.get(i);

            // Append template
            cmd.append("#PlayerList", PLAYER_ENTRY);

            // Set individual column values
            String selector = "#PlayerList[" + i + "]";
            cmd.set(selector + " #PlayerName.Text", player.getUsername());
            cmd.set(selector + " #Playtime.Text", player.getFormattedPlaytime());
            cmd.set(selector + " #Kills.Text", String.valueOf(player.getKillCount()));
            cmd.set(selector + " #Deaths.Text", String.valueOf(player.getDeathCount()));
            cmd.set(selector + " #StatusOnline.Visible", player.isOnline());
            cmd.set(selector + " #StatusOffline.Visible", !player.isOnline());

            // Bind click event
            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    EventData.of("SelectPlayer", player.getUsername()),
                    false
            );
        }
    }

    private void showPlayerDetail(@Nonnull UICommandBuilder cmd) {
        if (selectedPlayer == null) return;

        PlayerDataManager manager = PlayerDataManager.getInstance();
        PlayerData player = manager.getByUsername(selectedPlayer);

        if (player == null) {
            this.selectedPlayer = null;
            return;
        }

        cmd.set("#ListView.Visible", false);
        cmd.set("#DetailView.Visible", true);
        cmd.set("#ColumnHeaders.Visible", false);
        cmd.set("#SortRow.Visible", false);

        cmd.set("#DetailUsername.Text", player.getUsername());
        cmd.set("#DetailStatusOnline.Visible", player.isOnline());
        cmd.set("#DetailStatusOffline.Visible", !player.isOnline());
        cmd.set("#DetailPlaytime.Text", player.getFormattedPlaytime());
        cmd.set("#DetailPvPKills.Text", String.valueOf(player.getPlayerKills()));
        cmd.set("#DetailMobKills.Text", String.valueOf(player.getMobKills()));
        cmd.set("#DetailDeaths.Text", String.valueOf(player.getDeathCount()));
        cmd.set("#DetailDamage.Text", String.format("%.0f", player.getDamageDealt()));

        float kd = player.getDeathCount() > 0
                ? (float) player.getPlayerKills() / player.getDeathCount()
                : player.getPlayerKills();
        cmd.set("#DetailKDRatio.Text", String.format("%.2f", kd));

        cmd.set("#DetailBlocksBroken.Text", String.valueOf(player.getBlocksBroken()));
        cmd.set("#DetailBlocksPlaced.Text", String.valueOf(player.getBlocksPlaced()));
    }

    private Comparator<PlayerData> getSortComparator() {
        return switch (sortMode) {
            case PLAYTIME -> Comparator.comparingLong(PlayerData::getTotalWithCurrentSession).reversed();
            case KILLS -> Comparator.comparingInt(PlayerData::getKillCount).reversed();
            case DEATHS -> Comparator.comparingInt(PlayerData::getDeathCount).reversed();
            case ONLINE -> Comparator.comparing(PlayerData::isOnline).reversed()
                    .thenComparing(Comparator.comparingLong(PlayerData::getTotalWithCurrentSession).reversed());
        };
    }

    public enum SortMode {
        PLAYTIME, KILLS, DEATHS, ONLINE;

        public static SortMode fromString(String s) {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return PLAYTIME;
            }
        }
    }

    public static class DashboardEventData {
        public static final BuilderCodec<DashboardEventData> CODEC = BuilderCodec.builder(
                DashboardEventData.class, DashboardEventData::new
        )
                .append(new KeyedCodec<>("@SearchQuery", Codec.STRING),
                        (e, v) -> e.searchQuery = v, e -> e.searchQuery)
                .add()
                .append(new KeyedCodec<>("Sort", Codec.STRING),
                        (e, v) -> e.sort = v, e -> e.sort)
                .add()
                .append(new KeyedCodec<>("SelectPlayer", Codec.STRING),
                        (e, v) -> e.selectPlayer = v, e -> e.selectPlayer)
                .add()
                .append(new KeyedCodec<>("Back", Codec.STRING),
                        (e, v) -> e.back = v, e -> e.back)
                .add()
                .append(new KeyedCodec<>("CurseForge", Codec.STRING),
                        (e, v) -> e.curseForge = v, e -> e.curseForge)
                .add()
                .append(new KeyedCodec<>("Discord", Codec.STRING),
                        (e, v) -> e.discord = v, e -> e.discord)
                .add()
                .build();

        @Nullable String searchQuery;
        @Nullable String sort;
        @Nullable String selectPlayer;
        @Nullable String back;
        @Nullable String curseForge;
        @Nullable String discord;

        public DashboardEventData() {}
    }
}
