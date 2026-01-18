package com.zeni.playtime.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.zeni.playtime.data.PlaytimeData;
import com.zeni.playtime.data.PlaytimeManager;
import com.zeni.playtime.listeners.PlayerConnectionListener;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.*;

public class PlaytimeCommand extends AbstractPlayerCommand {
    private final PlaytimeManager playtimeManager;
    private final PlayerConnectionListener connectionListener;

    public PlaytimeCommand(PlaytimeManager playtimeManager, PlayerConnectionListener connectionListener) {
        super("playtime", "Displays time played", false);
        this.playtimeManager = playtimeManager;
        this.connectionListener = connectionListener;

        this.addSubCommand(new ToggleHUDCommand(connectionListener));
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(Message.raw("Can't use this command from console."));
            return;
        }

        Player player = commandContext.senderAs(Player.class);
        UUID playerId = player.getUuid();

        PlaytimeWorld currentWorld = determinePlayerWorld(commandContext);
        if (currentWorld != null) {
            connectionListener.registerPlayer(playerId, currentWorld.uuid);
        }

        PlaytimeData data = playtimeManager.getPlayerData(playerId);

        if (currentWorld != null) {
            long worldTotal = data.getWorldPlaytime().getOrDefault(currentWorld.uuid, 0L);
            long activeSession = data.hasActiveSession(currentWorld.uuid) ? data.getCurrentSessionTime(currentWorld.uuid) : 0L;

            long totalWithCurrentSession = worldTotal + activeSession;

            String formattedWorldTotal = playtimeManager.formatTime(totalWithCurrentSession);
            String formattedSession = playtimeManager.formatTime(activeSession);

            commandContext.sendMessage(Message.raw(currentWorld.displayName() + ": " + formattedWorldTotal));
            if (activeSession > 0) {
                commandContext.sendMessage(Message.raw("Session: " + formattedSession));
            }

            commandContext.sendMessage(Message.raw(""));
        }
    }

    public record PlaytimeWorld(String displayName, UUID uuid) {
    }

    private PlaytimeWorld determinePlayerWorld(CommandContext context) {
        try {
            Object sender = context.sender();
            try {
                Object world = sender.getClass().getMethod("getWorld").invoke(sender);
                if (world != null) {
                    WorldConfig worldConfig = (WorldConfig) world.getClass().getMethod("getWorldConfig").invoke(world);
                    String displayName = worldConfig.getDisplayName();
                    UUID uuid = worldConfig.getUuid();
                    return new PlaytimeWorld(displayName, uuid);
                }
            } catch (Exception _) {
            }

        } catch (Exception _) {
        }
        return null;
    }
}
