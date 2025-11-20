package org.duvo.avsword;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwordManager {

    private final Map<Integer, SwordData> swordsCache = new HashMap<>();
    private final AvSword plugin;

    public SwordManager(AvSword plugin) {
        this.plugin = plugin;
        loadSwords();
    }

    public void loadSwords() {
        swordsCache.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("swords");

        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "swords." + key + ".";
            try {
                int modelData = plugin.getConfig().getInt(path + "custom-model-data");
                boolean enabled = plugin.getConfig().getBoolean(path + "enabled");
                boolean enemyOnly = plugin.getConfig().getBoolean(path + "target-enemy-only");
                int cooldown = plugin.getConfig().getInt(path + "cooldown");

                List<String> effects = new ArrayList<>();
                if (plugin.getConfig().isList(path + "effects")) {

                    effects = plugin.getConfig().getStringList(path + "effects");
                } else if (plugin.getConfig().isString(path + "effect")) {

                    String singleEffect = plugin.getConfig().getString(path + "effect");
                    if (singleEffect != null) effects.add(singleEffect);
                } else {

                    effects.add("NONE");
                }

                effects.replaceAll(String::toUpperCase);


                int radius = plugin.getConfig().getInt(path + "radius");
                int level = plugin.getConfig().getInt(path + "effect-level");
                int duration = plugin.getConfig().getInt(path + "effect-duration") * 20;
                int tpDist = plugin.getConfig().getInt(path + "teleport-distance", 0);
                int breathDur = plugin.getConfig().getInt(path + "breath-duration", 0);

                String soundName = plugin.getConfig().getString(path + "sound.name", "");
                Sound sound = null;
                float vol = 1.0f;
                float pitch = 1.0f;
                if (!soundName.isEmpty()) {
                    try {
                        sound = Sound.valueOf(soundName.toUpperCase());
                        vol = (float) plugin.getConfig().getDouble(path + "sound.volume", 1.0);
                        pitch = (float) plugin.getConfig().getDouble(path + "sound.pitch", 1.0);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid sound for sword " + key + ": " + soundName);
                    }
                }

                SwordData data = new SwordData(key, enabled, enemyOnly, cooldown, modelData, effects, radius, level, duration, tpDist, breathDur, sound, vol, pitch);
                swordsCache.put(modelData, data);

            } catch (Exception e) {
                plugin.getLogger().severe("Error loading sword '" + key + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + swordsCache.size() + " swords into cache.");
    }

    public void createSword(String name, int modelData, String initialEffect) {
        String path = "swords." + name;
        initialEffect = initialEffect.toUpperCase();

        plugin.getConfig().set(path + ".enabled", true);
        plugin.getConfig().set(path + ".custom-model-data", modelData);
        plugin.getConfig().set(path + ".cooldown", 10);

        plugin.getConfig().set(path + ".effects", Collections.singletonList(initialEffect));

        plugin.getConfig().set(path + ".sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        plugin.getConfig().set(path + ".sound.volume", 1.0);
        plugin.getConfig().set(path + ".sound.pitch", 1.0);

        if (initialEffect.equals("ENDERMAN")) {
            plugin.getConfig().set(path + ".teleport-distance", 8);
            plugin.getConfig().set(path + ".target-enemy-only", false);
        }
        else if (initialEffect.equals("DRAGON")) {
            plugin.getConfig().set(path + ".breath-duration", 5);
            plugin.getConfig().set(path + ".target-enemy-only", true);
        }
        else {
            plugin.getConfig().set(path + ".target-enemy-only", true);
            plugin.getConfig().set(path + ".radius", 5);
            plugin.getConfig().set(path + ".effect-level", 1);
            plugin.getConfig().set(path + ".effect-duration", 5);
        }

        plugin.saveConfig();
        loadSwords();
    }

    public void updateSwordSetting(String swordName, String setting, Object value) {
        String path = "swords." + swordName + "." + setting;


        if (setting.equalsIgnoreCase("effect")) {
            path = "swords." + swordName + ".effects";
            List<String> list = new ArrayList<>();
            list.add(value.toString().toUpperCase());
            plugin.getConfig().set(path, list);
        }
        else if (setting.equalsIgnoreCase("effects")) {

            plugin.getConfig().set("swords." + swordName + ".effects", value);
        }
        else {
            plugin.getConfig().set(path, value);
        }

        plugin.saveConfig();
        loadSwords();
    }

    public SwordData getSwordByModel(int modelData) {
        return swordsCache.get(modelData);
    }
}