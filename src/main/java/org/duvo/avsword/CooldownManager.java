package org.duvo.avsword;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();

    private String getKey(UUID uuid, int modelData) {
        return uuid.toString() + "_" + modelData;
    }

    public long getRemainingSeconds(Player player, int modelData, int cooldownSeconds) {
        String key = getKey(player.getUniqueId(), modelData);
        if (!cooldowns.containsKey(key)) return 0;

        long timeLeft = (cooldowns.get(key) + (cooldownSeconds * 1000L)) - System.currentTimeMillis();
        return Math.max(0, timeLeft / 1000);
    }

    public void setCooldown(Player player, int modelData) {
        String key = getKey(player.getUniqueId(), modelData);
        cooldowns.put(key, System.currentTimeMillis());
    }

    public void removePlayer(Player player) {
        String uuidPrefix = player.getUniqueId().toString();
        cooldowns.keySet().removeIf(key -> key.startsWith(uuidPrefix));
    }

    public void clearAll() {
        cooldowns.clear();
    }
}