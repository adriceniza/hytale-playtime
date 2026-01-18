package com.zeni.playtime.listeners;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.zeni.playtime.data.HudPreferences;
import com.zeni.playtime.data.PlaytimeManager;
import com.zeni.playtime.hud.PlaytimeHud;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerConnectionListener {
    private static PlayerConnectionListener instance;

    private final PlaytimeManager playtimeManager;
    private final PlaytimeListener playtimeListener;
    private final ScheduledExecutorService scheduler;
    private final Set<UUID> trackedPlayers;
    private final Map<UUID, PlaytimeHud> activeHuds;
    private final HudPreferences hudPreferences;

    public PlayerConnectionListener(PlaytimeManager playtimeManager, PlaytimeListener playtimeListener, HudPreferences hudPreferences) {
        this.playtimeManager = playtimeManager;
        this.playtimeListener = playtimeListener;
        this.hudPreferences = hudPreferences;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.trackedPlayers = ConcurrentHashMap.newKeySet();
        this.activeHuds = new ConcurrentHashMap<>();
        instance = this;

        playtimeManager.setHudUpdateCallback(this::updatePlayerHud);

        startHudUpdates();
    }


    private void startHudUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Map.Entry<UUID, PlaytimeHud> entry : activeHuds.entrySet()) {
                    entry.getValue().updatePlaytime();
                }
            } catch (Exception e) {
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static void onPlayerReadyEvent(PlayerReadyEvent event){
        if (instance == null) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getWorld() == null) {
            return;
        }

        UUID playerId = player.getUuid(); // don't touch
        UUID worldUuid = player.getWorld().getWorldConfig().getUuid();

        instance.registerPlayer(playerId, worldUuid);

        // Intentar restaurar el HUD si el jugador tenía la preferencia guardada
        if (instance.hudPreferences.isHudEnabled(playerId)) {
            // Crear PlayerRef desde el mundo del jugador
            try {
                instance.enableHudForPlayer(playerId, player.getPlayerRef(), player);
            } catch (Exception e) {
                // Si falla, se restaurará cuando ejecute un comando
            }
        }
    }

    public void registerPlayer(UUID playerId, UUID worldUuid) {
        if (!trackedPlayers.contains(playerId)) {
            trackedPlayers.add(playerId);
            playtimeListener.registerPlayer(playerId, worldUuid);
        }
    }

    public void unregisterPlayer(UUID playerId) {
        if (trackedPlayers.remove(playerId)) {
            playtimeListener.unregisterPlayer(playerId);
            activeHuds.remove(playerId);
        }
    }


    public void enableHudForPlayer(UUID playerId, PlayerRef playerRef, Player player) {
        try {
            PlaytimeHud playtimeHud = new PlaytimeHud(playerRef, playtimeManager);
            player.getHudManager().setCustomHud(playerRef, playtimeHud);
            registerHud(playerId, playtimeHud);
            hudPreferences.setHudEnabled(playerId, true);
        } catch (Exception e) {
        }
    }


    public void disableHudForPlayer(UUID playerId, PlayerRef playerRef, Player player) {
        try {
            unregisterHud(playerId);
            player.getHudManager().setCustomHud(playerRef, new CustomUIHud(playerRef) {
                @Override
                protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {

                }
            });
            hudPreferences.setHudEnabled(playerId, false);
        } catch (Exception e) {
        }
    }


    public boolean isHudEnabled(UUID playerId) {
        return activeHuds.containsKey(playerId);
    }


    public boolean hasHudPreference(UUID playerId) {
        return hudPreferences.isHudEnabled(playerId);
    }


    public void tryRestoreHud(UUID playerId, PlayerRef playerRef, Player player) {
        if (!isHudEnabled(playerId) && hasHudPreference(playerId)) {
            enableHudForPlayer(playerId, playerRef, player);
        }
    }


    public void registerHud(UUID playerId, PlaytimeHud hud) {
        activeHuds.put(playerId, hud);
    }


    public void unregisterHud(UUID playerId) {
        activeHuds.remove(playerId);
    }


    private void updatePlayerHud(UUID playerId) {
        PlaytimeHud hud = activeHuds.get(playerId);
        if (hud != null) {
            hud.updatePlaytime();
        }
    }

    public void shutdown() {
        for (UUID playerId : new HashSet<>(trackedPlayers)) {
            unregisterPlayer(playerId);
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}

