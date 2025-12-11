package org.duvo.avsword;

import org.bukkit.Sound;
import java.util.List;

public class SwordData {
    private final String key;
    private final boolean enabled;
    private final boolean targetEnemyOnly;
    private final int cooldown;
    private final int customModelData;
    private final List<String> effectTypes;
    private final int radius;
    private final int effectLevel;
    private final int effectDuration;
    private final int teleportDistance;
    private final int breathDuration;
    private final double creeperPower;
    private final double creeperDamage;
    private final double ghastPower;

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SwordData(String key, boolean enabled, boolean targetEnemyOnly, int cooldown, int customModelData,
                     List<String> effectTypes, int radius, int effectLevel, int effectDuration,
                     int teleportDistance, int breathDuration,
                     double creeperPower, double creeperDamage,
                     double ghastPower,
                     Sound sound, float volume, float pitch) {
        this.key = key;
        this.enabled = enabled;
        this.targetEnemyOnly = targetEnemyOnly;
        this.cooldown = cooldown;
        this.customModelData = customModelData;
        this.effectTypes = effectTypes;
        this.radius = radius;
        this.effectLevel = effectLevel;
        this.effectDuration = effectDuration;
        this.teleportDistance = teleportDistance;
        this.breathDuration = breathDuration;
        this.creeperPower = creeperPower;
        this.creeperDamage = creeperDamage;
        this.ghastPower = ghastPower;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public String getKey() { return key; }
    public boolean isEnabled() { return enabled; }
    public boolean isTargetEnemyOnly() { return targetEnemyOnly; }
    public int getCooldown() { return cooldown; }
    public int getCustomModelData() { return customModelData; }
    public List<String> getEffectTypes() { return effectTypes; }
    public int getRadius() { return radius; }
    public int getEffectLevel() { return effectLevel; }
    public int getEffectDuration() { return effectDuration; }
    public int getTeleportDistance() { return teleportDistance; }
    public int getBreathDuration() { return breathDuration; }
    public double getCreeperPower() { return creeperPower; }
    public double getCreeperDamage() { return creeperDamage; }
    public double getGhastPower() { return ghastPower; }

    public Sound getSound() { return sound; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
}