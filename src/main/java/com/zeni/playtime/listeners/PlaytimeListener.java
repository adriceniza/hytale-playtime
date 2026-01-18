package com.zeni.playtime.listeners;

import com.zeni.playtime.data.PlaytimeManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Trackea el tiempo de juego de los jugadores.
 * Usa un sistema de registro manual de jugadores para evitar APIs deprecated.
 */
public class PlaytimeListener {
    private final PlaytimeManager playtimeManager;
    private final ScheduledExecutorService scheduler;
    private final Map<UUID, UUID> activePlayerWorlds;

    public PlaytimeListener(PlaytimeManager playtimeManager) {
        this.playtimeManager = playtimeManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.activePlayerWorlds = new ConcurrentHashMap<>();
        startTracking();
    }

    /**
     * Registra un jugador como activo en un mundo específico.
     * Este método debe ser llamado cuando un jugador ejecuta un comando o se detecta su actividad.
     */
    public void registerPlayer(UUID playerId, UUID worldUuid) {
        UUID currentWorld = activePlayerWorlds.get(playerId);

        // Si el jugador cambió de mundo, finalizar la sesión anterior
        if (currentWorld != null && !currentWorld.equals(worldUuid)) {
            playtimeManager.endSession(playerId, currentWorld);
        }

        // Actualizar el mundo actual y comenzar nueva sesión si es necesario
        activePlayerWorlds.put(playerId, worldUuid);
        if (!playtimeManager.getPlayerData(playerId).hasActiveSession(worldUuid)) {
            playtimeManager.startSession(playerId, worldUuid);
        }
    }

    /**
     * Desregistra un jugador cuando se desconecta.
     */
    public void unregisterPlayer(UUID playerId) {
        UUID worldUuid = activePlayerWorlds.remove(playerId);
        if (worldUuid != null) {
            playtimeManager.endSession(playerId, worldUuid);
        }
    }

    /**
     * Obtiene los jugadores activos registrados.
     */
    public Map<UUID, UUID> getActivePlayers() {
        return new ConcurrentHashMap<>(activePlayerWorlds);
    }

    private void startTracking() {
        // Guardar datos periódicamente para los jugadores registrados
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Guardar datos de todos los jugadores activos
                playtimeManager.saveData();

            } catch (Exception e) {
                // Ignorar errores silenciosamente
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public void shutdown() {
        // Finalizar todas las sesiones activas
        for (Map.Entry<UUID, UUID> entry : activePlayerWorlds.entrySet()) {
            playtimeManager.endSession(entry.getKey(), entry.getValue());
        }
        activePlayerWorlds.clear();

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

