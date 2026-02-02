package com.hytaletravelers.playerlogger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.hytaletravelers.playerlogger.ui.PlayerLoggerDashboardUI;

import javax.annotation.Nonnull;

/**
 * Standalone command to open the player stats dashboard.
 * Aliases: /players
 */
public class ListCommand extends AbstractPlayerCommand {

    public ListCommand() {
        super("list", "Open the player list");
        this.addAliases(new String[]{"players"});
    }

    @Override
    public boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Error: Could not open UI"));
            return;
        }

        PlayerLoggerDashboardUI ui = new PlayerLoggerDashboardUI(playerRef);
        player.getPageManager().openCustomPage(ref, store, ui);
    }
}
