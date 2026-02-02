package com.hytaletravelers.playerlogger.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Main command collection for PlayerLogger.
 *
 * Subcommands:
 *   /pl help              - Show available commands
 *   /pl ui                - Open the stats dashboard
 *   /pl wipe <player|all> - Remove player data
 *   /pl reset <category>  - Reset specific stats
 */
public class PlayerLoggerPluginCommand extends AbstractCommandCollection {

    public PlayerLoggerPluginCommand() {
        super("pl", "PlayerLogger commands");

        this.addSubCommand(new HelpSubCommand());
        this.addSubCommand(new UISubCommand());
        this.addSubCommand(new WipeSubCommand());
        this.addSubCommand(new ResetSubCommand());
    }

    @Override
    public boolean canGeneratePermission() {
        return false;
    }
}
