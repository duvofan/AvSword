package org.duvo.avsword.Friend;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.duvo.avsword.AvSword;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FriendManager {

    private final AvSword plugin;
    private final Map<UUID, Set<UUID>> friendMap = new HashMap<>();
    private File file;

    public FriendManager(AvSword plugin) {
        this.plugin = plugin;
        loadFriends();
    }

    public boolean isFriend(UUID ownerId, UUID targetId) {
        if (ownerId.equals(targetId)) return true;
        synchronized (friendMap) {
            return friendMap.containsKey(ownerId) && friendMap.get(ownerId).contains(targetId);
        }
    }

    public boolean addFriend(UUID ownerId, UUID targetId) {
        boolean added;
        synchronized (friendMap) {
            friendMap.computeIfAbsent(ownerId, k -> new HashSet<>());
            added = friendMap.get(ownerId).add(targetId);
        }

        if (added) {
            saveFriendsAsync();
        }
        return added;
    }

    public boolean removeFriend(UUID ownerId, UUID targetId) {
        boolean removed = false;
        synchronized (friendMap) {
            if (friendMap.containsKey(ownerId)) {
                removed = friendMap.get(ownerId).remove(targetId);
                if (removed && friendMap.get(ownerId).isEmpty()) {
                    friendMap.remove(ownerId);
                }
            }
        }

        if (removed) {
            saveFriendsAsync();
        }
        return removed;
    }

    public Set<UUID> getFriends(UUID ownerId) {
        synchronized (friendMap) {
            if (friendMap.containsKey(ownerId)) {
                return new HashSet<>(friendMap.get(ownerId));
            }
            return Collections.emptySet();
        }
    }

    public void loadFriends() {
        file = new File(plugin.getDataFolder(), "friends.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("friends.yml oluşturulamadı!");
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        synchronized (friendMap) {
            friendMap.clear();

            if (config.contains("friends")) {
                for (String key : config.getConfigurationSection("friends").getKeys(false)) {
                    try {
                        UUID ownerId = UUID.fromString(key);
                        List<String> targetStrings = config.getStringList("friends." + key);
                        Set<UUID> targets = new HashSet<>();
                        for (String t : targetStrings) {
                            targets.add(UUID.fromString(t));
                        }
                        friendMap.put(ownerId, targets);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("friends.yml yüklenirken hata oluştu (UUID formatı bozuk): " + key);
                    }
                }
            }
            plugin.getLogger().info("Arkadaş listesi yüklendi: " + friendMap.size() + " oyuncunun kaydı var.");
        }
    }

    public void saveFriends() {
        Map<UUID, Set<UUID>> snapshot;
        synchronized (friendMap) {
            snapshot = new HashMap<>();
            for (Map.Entry<UUID, Set<UUID>> entry : friendMap.entrySet()) {
                snapshot.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
        }
        saveToDisk(snapshot);
    }

    private void saveFriendsAsync() {
        final Map<UUID, Set<UUID>> snapshot;
        synchronized (friendMap) {
            snapshot = new HashMap<>();
            for (Map.Entry<UUID, Set<UUID>> entry : friendMap.entrySet()) {
                snapshot.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            saveToDisk(snapshot);
        });
    }

    private void saveToDisk(Map<UUID, Set<UUID>> data) {
        if (file == null) return;

        YamlConfiguration safeConfig = new YamlConfiguration();

        for (Map.Entry<UUID, Set<UUID>> entry : data.entrySet()) {
            List<String> uuidList = new ArrayList<>();
            for (UUID id : entry.getValue()) {
                uuidList.add(id.toString());
            }
            safeConfig.set("friends." + entry.getKey().toString(), uuidList);
        }

        try {
            safeConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("friends.yml asenkron kaydedilemedi: " + e.getMessage());
        }
    }
}