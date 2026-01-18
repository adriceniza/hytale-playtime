package com.zeni.playtime.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Gestiona los datos de tiempo de juego de todos los jugadores
 */
public class PlaytimeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, PlaytimeData> playerData;
    private final File dataFile;
    private final HytaleLogger logger;
    private Consumer<UUID> hudUpdateCallback;

    public PlaytimeManager(File dataFolder, HytaleLogger logger) {
        this.playerData = new ConcurrentHashMap<>();
        this.dataFile = new File(dataFolder, "playtime-data.json");
        this.logger = logger;

        // Crear directorio si no existe
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                logger.atWarning().log("No se pudo crear el directorio de datos");
            }
        }

        loadData();
    }

    public PlaytimeData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlaytimeData::new);
    }

    public Map<UUID, PlaytimeData> getAllPlayerData() {
        return new HashMap<>(playerData);
    }

    public void startSession(UUID playerId, UUID worldUuid) {
        PlaytimeData data = getPlayerData(playerId);
        data.startSession(worldUuid);
    }

    public void endSession(UUID playerId, UUID worldUuid) {
        PlaytimeData data = playerData.get(playerId);
        if (data != null) {
            data.endSession(worldUuid);
            saveData();
            notifyHudUpdate(playerId);
        }
    }

    public void endAllSessions(UUID playerId) {
        PlaytimeData data = playerData.get(playerId);
        if (data != null) {
            // Finalizar todas las sesiones activas
            for (UUID worldUuid : data.getWorldPlaytime().keySet()) {
                if (data.hasActiveSession(worldUuid)) {
                    data.endSession(worldUuid);
                }
            }
            saveData();
            notifyHudUpdate(playerId);
        }
    }

    /**
     * Establece el callback para notificar actualizaciones del HUD
     */
    public void setHudUpdateCallback(Consumer<UUID> callback) {
        this.hudUpdateCallback = callback;
    }

    /**
     * Notifica al HUD que los datos han sido actualizados
     */
    private void notifyHudUpdate(UUID playerId) {
        if (hudUpdateCallback != null) {
            hudUpdateCallback.accept(playerId);
        }
    }

    public void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            Map<String, Map<UUID, Long>> serializableData = new HashMap<>();
            for (Map.Entry<UUID, PlaytimeData> entry : playerData.entrySet()) {
                serializableData.put(entry.getKey().toString(), entry.getValue().getWorldPlaytime());
            }
            GSON.toJson(serializableData, writer);
        } catch (IOException e) {
            logger.atWarning().log("Error al guardar los datos de tiempo de juego: " + e.getMessage());
        }
    }

    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Map<UUID, Long>>>(){}.getType();
            Map<String, Map<UUID, Long>> loadedData = GSON.fromJson(reader, type);

            if (loadedData != null) {
                for (Map.Entry<String, Map<UUID, Long>> entry : loadedData.entrySet()) {
                    UUID playerId = UUID.fromString(entry.getKey());
                    PlaytimeData data = new PlaytimeData(playerId);
                    for (Map.Entry<UUID, Long> worldEntry : entry.getValue().entrySet()) {
                        data.addPlaytime(worldEntry.getKey(), worldEntry.getValue());
                    }
                    playerData.put(playerId, data);
                }
            }
        } catch (IOException e) {
            logger.atWarning().log("Error al cargar los datos de tiempo de juego: " + e.getMessage());
        }
    }

    public String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
