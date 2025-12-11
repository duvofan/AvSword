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
        File file = new File(plugin.getDataFolder(), "messages/" + fileName);

        if (!file.exists()) {
            try {
                plugin.saveResource("messages/" + fileName, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Dil dosyası '" + fileName + "' bulunamadı. Varsayılan dile (English) dönülüyor.");

                lang = "en";
                fileName = "messages_en.yml";
                file = new File(plugin.getDataFolder(), "messages/" + fileName);
                if (!file.exists()) {
                    try {
                        plugin.saveResource("messages/messages_en.yml", false);
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Varsayılan dil dosyası (messages_en.yml) oluşturulamadı!");
                    }
                }
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(file);
        prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&6AvSword &8» "));
        plugin.getLogger().info("Dil dosyası yüklendi: " + fileName);
    }

    public boolean hasMessage(String key) {
        return messagesConfig != null && messagesConfig.contains(key);
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

        if (replacements != null && replacements.length > 0) {
            int loopLength = (replacements.length % 2 == 0) ? replacements.length : replacements.length - 1;

            if (replacements.length % 2 != 0) {
                plugin.getLogger().warning("Language Warning: '" + key + "' için eksik parametre girildi. Son argüman yoksayıldı.");
            }

            for (int i = 0; i < loopLength; i += 2) {
                String target = replacements[i];
                String replacement = replacements[i + 1];
                if (target != null && replacement != null) {
                    msg = msg.replace(target, replacement);
                }
            }
        }
        player.sendMessage(prefix + msg);
    }

    public String getPrefix() { return prefix; }
}