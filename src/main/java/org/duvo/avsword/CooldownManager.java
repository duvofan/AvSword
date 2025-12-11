package org.duvo.avsword;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<Integer, Long>> cooldowns = new HashMap<>();

    public long getRemainingSeconds(Player player, int modelData, int cooldownSeconds) {
        UUID uuid = player.getUniqueId();

        if (!cooldowns.containsKey(uuid)) return 0;

        Map<Integer, Long> playerCooldowns = cooldowns.get(uuid);
        if (!playerCooldowns.containsKey(modelData)) return 0;

        long startTime = playerCooldowns.get(modelData);
        long endTime = startTime + (cooldownSeconds * 1000L);
        long timeLeft = endTime - System.currentTimeMillis();

        return Math.max(0, timeLeft / 1000);
    }

    public void setCooldown(Player player, int modelData) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(modelData, System.currentTimeMillis());
    }

    public void removePlayer(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public void clearAll() {
        cooldowns.clear();
    }
}