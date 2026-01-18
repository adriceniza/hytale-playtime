package com.zeni.playtime.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Boolean> hudEnabled;
    private final File preferencesFile;

    public HudPreferences(File dataFolder) {
        this.hudEnabled = new ConcurrentHashMap<>();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.preferencesFile = new File(dataFolder, "hud-preferences.json");
        loadPreferences();
    }

    public boolean isHudEnabled(UUID playerId) {
        return hudEnabled.getOrDefault(playerId, false);
    }


    public void setHudEnabled(UUID playerId, boolean enabled) {
        hudEnabled.put(playerId, enabled);
        savePreferences();
    }


    private void loadPreferences() {
        if (!preferencesFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(preferencesFile)) {
            Type type = new TypeToken<HashMap<String, Boolean>>(){}.getType();
            Map<String, Boolean> data = GSON.fromJson(reader, type);

            if (data != null) {
                for (Map.Entry<String, Boolean> entry : data.entrySet()) {
                    try {
                        UUID playerId = UUID.fromString(entry.getKey());
                        hudEnabled.put(playerId, entry.getValue());
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    private void savePreferences() {
        try (FileWriter writer = new FileWriter(preferencesFile)) {
            Map<String, Boolean> data = new HashMap<>();
            for (Map.Entry<UUID, Boolean> entry : hudEnabled.entrySet()) {
                data.put(entry.getKey().toString(), entry.getValue());
            }
            GSON.toJson(data, writer);
        } catch (IOException e) {
        }
    }
}
