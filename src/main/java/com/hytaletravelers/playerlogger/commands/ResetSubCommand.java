package com.hytaletravelers.playerlogger.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;
import com.hytaletravelers.playerlogger.data.PlayerDataManager.StatCategory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * /pl reset <category> [player] - Reset specific statistics.
 *
 * Usage:
 *   /pl reset kills           - Reset kills for ALL players
 *   /pl reset deaths          - Reset deaths for ALL players
 *   /pl reset combat          - Reset all combat stats for ALL players
 *   /pl reset kills <player>  - Reset kills for a specific player
 *
 * Categories: all, combat, blocks, playtime, kills, deaths, damage
 */
public class ResetSubCommand extends CommandBase {

    private final RequiredArg<String> categoryArg;
    private final OptionalArg<String> playerArg;

    public ResetSubCommand() {
        super("reset", "Reset player statistics");
        this.requirePermission("playerlogger.command.reset");
        this.categoryArg = withRequiredArg("category", "Stat category to reset", ArgTypes.STRING);
        this.playerArg = withOptionalArg("player", "Player name (omit for all players)", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String categoryName = context.get(categoryArg);
        String playerName = context.get(playerArg);

        if (categoryName == null || categoryName.isEmpty()) {
            sendUsage(context);
            return;
        }

        StatCategory category = StatCategory.fromString(categoryName);
        if (category == null) {
            context.sendMessage(Message.raw("Unknown category: " + categoryName));
            sendCategories(context);
            return;
        }

        PlayerDataManager manager = PlayerDataManager.getInstance();

        if (playerName != null && !playerName.isEmpty()) {
            // Reset for specific player
            boolean success = manager.resetPlayerStats(playerName, category);
            if (success) {
                context.sendMessage(Message.raw("Reset " + category.getDescription() + " for " + playerName));
            } else {
                context.sendMessage(Message.raw("Player not found: " + playerName));
            }
        } else {
            // Reset for all players
            int count = manager.resetAllPlayersStats(category);
            context.sendMessage(Message.raw("Reset " + category.getDescription() + " for " + count + " players"));
        }
    }

    private void sendUsage(CommandContext context) {
        context.sendMessage(Message.raw("Usage:"));
        context.sendMessage(Message.raw("  /pl reset <category>          - Reset for all players"));
        context.sendMessage(Message.raw("  /pl reset <category> <player> - Reset for one player"));
        sendCategories(context);
    }

    private void sendCategories(CommandContext context) {
        String categories = Arrays.stream(StatCategory.values())
                .map(c -> c.name().toLowerCase())
                .collect(Collectors.joining(", "));
        context.sendMessage(Message.raw("Categories: " + categories));
    }
}
