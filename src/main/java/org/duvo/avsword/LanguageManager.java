package org.duvo.avsword;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;

public class LanguageManager {

    private final AvSword plugin;
    private FileConfiguration messagesConfig;
    private String prefix;

    public LanguageManager(AvSword plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        String lang = plugin.getConfig().getString("language", "en");
        String fileName = "messages_" + lang + ".yml";
        String resourcePath = "messages/" + fileName;

        if (plugin.getResource(resourcePath) == null) {
            plugin.getLogger().warning("Language file '" + resourcePath + "' not found. Falling back to English.");
            resourcePath = "messages/messages_en.yml";
        }

        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(file);
        prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&6AvSword &8» "));
        plugin.getLogger().info("Loaded language file: " + file.getPath());
    }

    public String getMessage(String key) {
        if (messagesConfig == null) return ChatColor.RED + "Language Error";

        String msg = messagesConfig.getString(key);
        if (msg == null) {
            return ChatColor.translateAlternateColorCodes('&', "&cMissing message: " + key);
        }

        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void sendMessage(Player player, String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String target = replacements[i];
                String replacement = replacements[i+1];
                if (target != null && replacement != null) {
                    msg = msg.replace(target, replacement);
                }
            }
        }
        player.sendMessage(prefix + msg);
    }

    public String getPrefix() { return prefix; }
}