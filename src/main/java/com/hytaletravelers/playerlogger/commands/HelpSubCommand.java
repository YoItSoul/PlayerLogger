package com.hytaletravelers.playerlogger.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/**
 * Displays available PlayerLogger commands.
 */
public class HelpSubCommand extends CommandBase {

    public HelpSubCommand() {
        super("help", "Show available commands");
    }

    @Override
    public boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("========== PlayerLogger =========="));
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("General:"));
        context.sendMessage(Message.raw("  /pl help  - Show this message"));
        context.sendMessage(Message.raw("  /pl ui    - Open stats dashboard"));
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("Admin:"));
        context.sendMessage(Message.raw("  /pl wipe <player>  - Remove a player"));
        context.sendMessage(Message.raw("  /pl wipe all       - Wipe all data"));
        context.sendMessage(Message.raw("  /pl reset <stat>   - Reset stat for all"));
        context.sendMessage(Message.raw("  /pl reset <stat> <player>"));
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("Stat categories:"));
        context.sendMessage(Message.raw("  all, combat, blocks, playtime,"));
        context.sendMessage(Message.raw("  kills, deaths, damage"));
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("=================================="));
    }
}
