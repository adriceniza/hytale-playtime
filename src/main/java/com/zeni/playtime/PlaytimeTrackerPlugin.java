package com.zeni.playtime;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.zeni.playtime.commands.PlaytimeCommand;
import com.zeni.playtime.data.HudPreferences;
import com.zeni.playtime.data.PlaytimeManager;
import com.zeni.playtime.listeners.PlayerConnectionListener;
import com.zeni.playtime.listeners.PlaytimeListener;

import javax.annotation.Nonnull;
import java.io.File;

public class PlaytimeTrackerPlugin extends JavaPlugin {
    private PlaytimeManager playtimeManager;
    private PlaytimeListener playtimeListener;
    private PlayerConnectionListener connectionListener;
    private HudPreferences hudPreferences;

    public PlaytimeTrackerPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        File dataFolder = new File("plugins/" + this.getManifest().getName());
        this.playtimeManager = new PlaytimeManager(dataFolder, this.getLogger());
        this.hudPreferences = new HudPreferences(dataFolder);

        this.playtimeListener = new PlaytimeListener(playtimeManager);

        this.connectionListener = new PlayerConnectionListener(playtimeManager, playtimeListener, hudPreferences);

        this.getCommandRegistry().registerCommand(new PlaytimeCommand(playtimeManager, connectionListener));

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerConnectionListener::onPlayerReadyEvent);

        this.getLogger().atInfo().log("PlaytimeTrackerPlugin started successfully");
    }

    @Override
    protected void shutdown() {
        if (connectionListener != null) {
            connectionListener.shutdown();
        }

        if (playtimeListener != null) {
            playtimeListener.shutdown();
        }

        if (playtimeManager != null) {
            playtimeManager.saveData();
            this.getLogger().atInfo().log("Playtime data saved successfully on shutdown.");
        }
    }
}