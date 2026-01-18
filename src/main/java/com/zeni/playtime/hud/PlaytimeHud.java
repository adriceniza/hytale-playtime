package com.zeni.playtime.hud;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.zeni.playtime.data.PlaytimeData;
import com.zeni.playtime.data.PlaytimeManager;

import javax.annotation.Nonnull;
import java.util.UUID;

public class PlaytimeHud extends CustomUIHud {

    private final PlaytimeManager playtimeManager;
    private String currentPlaytimeText = "0h 0m 0s";
    private final UUID playerId;

    public PlaytimeHud(@Nonnull PlayerRef playerRef, @Nonnull PlaytimeManager playtimeManager) {
        super(playerRef);
        this.playerId = playerRef.getUuid();
        this.playtimeManager = playtimeManager;
        this.currentPlaytimeText = formatPlaytime(this.playtimeManager.getPlayerData(this.playerId).getTotalPlaytime());
    }

    public void updatePlaytime() {
        try {
            PlaytimeData playerData = playtimeManager.getPlayerData(playerId);

            long totalPlaytime = playerData.getTotalPlaytime();

            for (UUID worldUuid : playerData.getWorldPlaytime().keySet()) {
                if (playerData.hasActiveSession(worldUuid)) {
                    totalPlaytime += playerData.getCurrentSessionTime(worldUuid);
                }
            }

            String newText = formatPlaytime(totalPlaytime);

            if (!newText.equals(currentPlaytimeText)) {
                currentPlaytimeText = newText;
                updateText(newText);
            }
        } catch (Exception e) {
        }
    }

    public void updateText(String newText) {
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        uiCommandBuilder.set("#MyLabel.TextSpans", Message.raw(newText));
        update(false, uiCommandBuilder);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder ui) {
        ui.append("Hud/PlaytimeHud.ui");
        ui.set("#MyLabel.TextSpans", Message.raw(currentPlaytimeText));
    }

    private String formatPlaytime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}