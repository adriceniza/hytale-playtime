package com.zeni.playtime.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.zeni.playtime.listeners.PlayerConnectionListener;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public class ToggleHUDCommand extends AbstractPlayerCommand {
    private final PlayerConnectionListener connectionListener;

    public ToggleHUDCommand(PlayerConnectionListener connectionListener) {
        super("hud", "Toggle playtime HUD display", false);
        this.connectionListener = connectionListener;
    }


    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        Player player = commandContext.senderAs(Player.class);

        try {
            UUID playerId = player.getUuid();

            if(!connectionListener.isHudEnabled(playerId)){
                connectionListener.enableHudForPlayer(playerId, playerRef, player);
                commandContext.sendMessage(Message.raw("Playtime HUD enabled."));
            } else {
                connectionListener.disableHudForPlayer(playerId, playerRef, player);
                commandContext.sendMessage(Message.raw("Playtime HUD disabled."));
            }
        } catch (Exception e){
            commandContext.sendMessage(Message.raw("An error occurred while toggling the HUD: " + e.getMessage()));
        }
    }
}
