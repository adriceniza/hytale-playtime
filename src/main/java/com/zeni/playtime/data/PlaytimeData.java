package com.zeni.playtime.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representa los datos de tiempo jugado de un jugador
 */
public class PlaytimeData {
    private final UUID playerId;
    private final Map<UUID, Long> worldPlaytime; // worldName -> tiempo en milisegundos
    private final Map<UUID, Long> sessionStartTime; // worldName -> timestamp de inicio de sesión

    public PlaytimeData(UUID playerId) {
        this.playerId = playerId;
        this.worldPlaytime = new HashMap<>();
        this.sessionStartTime = new HashMap<>();
    }


    public Map<UUID, Long> getWorldPlaytime() {
        return new HashMap<>(worldPlaytime);
    }

    public long getTotalPlaytime() {
        return worldPlaytime.values().stream().mapToLong(Long::longValue).sum();
    }

    public void startSession(UUID uuid) {
        sessionStartTime.put(uuid, System.currentTimeMillis());
    }

    public void endSession(UUID uuid) {
        Long startTime = sessionStartTime.remove(uuid);
        if (startTime != null) {
            long sessionDuration = System.currentTimeMillis() - startTime;
            worldPlaytime.merge(uuid, sessionDuration, Long::sum);
        }
    }

    public void addPlaytime(UUID uuid, long milliseconds) {
        worldPlaytime.merge(uuid, milliseconds, Long::sum);
    }

    public boolean hasActiveSession(UUID uuid) {
        return sessionStartTime.containsKey(uuid);
    }

    public long getCurrentSessionTime(UUID uuid) {
        Long startTime = sessionStartTime.get(uuid);
        if (startTime == null) {
            return 0L;
        }
        return System.currentTimeMillis() - startTime;
    }
}
