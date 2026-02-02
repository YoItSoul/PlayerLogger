package com.hytaletravelers.playerlogger.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hytaletravelers.playerlogger.data.PlayerDataManager;

import javax.annotation.Nonnull;

/**
 * /pl wipe [player] - Remove a player's data or wipe all data.
 *
 * Usage:
 *   /pl wipe all     - Wipe ALL player data (requires confirmation)
 *   /pl wipe <name>  - Remove a specific player's data
 */
public class WipeSubCommand extends CommandBase {

    private final RequiredArg<String> targetArg;

    public WipeSubCommand() {
        super("wipe", "Remove player data");
        this.requirePermission("playerlogger.command.wipe");
        this.targetArg = withRequiredArg("target", "Player name or 'all'", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String target = context.get(targetArg);

        if (target == null || target.isEmpty()) {
            sendUsage(context);
            return;
        }

        PlayerDataManager manager = PlayerDataManager.getInstance();

        if (target.equalsIgnoreCase("all")) {
            int count = manager.wipeAllPlayers();
            context.sendMessage(Message.raw("Wiped all player data (" + count + " players removed)"));
        } else {
            boolean success = manager.removePlayer(target);
            if (success) {
                context.sendMessage(Message.raw("Removed player: " + target));
            } else {
                context.sendMessage(Message.raw("Player not found: " + target));
            }
        }
    }

    private void sendUsage(CommandContext context) {
        context.sendMessage(Message.raw("Usage:"));
        context.sendMessage(Message.raw("  /pl wipe <player> - Remove a player's data"));
        context.sendMessage(Message.raw("  /pl wipe all      - Wipe ALL player data"));
    }
}
