package org.duvo.avsword;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SwordListener implements Listener {

    private final AvSword plugin;
    private final Map<String, List<Location>> safeZones = new HashMap<>();
    private final Map<UUID, Integer> dragonFireballDurations = new HashMap<>();
    private final Map<UUID, Long> elytraDisabled = new HashMap<>();

    private final Map<Location, Material> tempBlocks = new HashMap<>();

    public SwordListener(AvSword plugin) {
        this.plugin = plugin;
        loadSafeZones();
    }

    public void cleanupTemporaryBlocks() {
        if (tempBlocks.isEmpty()) return;

        for (Map.Entry<Location, Material> entry : tempBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            if (block.getType() == Material.COBWEB) {
                block.setType(entry.getValue());
            }
        }
        tempBlocks.clear();
        plugin.getLogger().info("Cleaned up temporary sword blocks.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.isOp() && plugin.latestVersion != null) {
            String currentVersion = plugin.getDescription().getVersion();

            if (!currentVersion.equalsIgnoreCase(plugin.latestVersion)) {

                String lang = plugin.getConfig().getString("language", "en");

                player.sendMessage("");

                if ("tr".equalsIgnoreCase(lang)) {
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.YELLOW + "Yeni bir güncelleme mevcut! " + ChatColor.GRAY + "(v" + plugin.latestVersion + ")");
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.AQUA + "" + ChatColor.UNDERLINE + "https://modrinth.com/project/avswords");
                } else {
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.YELLOW + "A new update is available! " + ChatColor.GRAY + "(v" + plugin.latestVersion + ")");
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.AQUA + "" + ChatColor.UNDERLINE + "https://modrinth.com/project/avswords");
                }

                player.sendMessage("");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCooldownManager().removePlayer(event.getPlayer());
    }

    private void loadSafeZones() {
        safeZones.clear();
        if (plugin.getConfig().contains("safe-zones")) {
            for (String key : plugin.getConfig().getConfigurationSection("safe-zones").getKeys(false)) {
                String coords = plugin.getConfig().getString("safe-zones." + key);
                if (coords == null) continue;
                String[] parts = coords.split(" ");
                if (parts.length != 3) continue;
                try {
                    String[] p1 = parts[0].split(",");
                    String[] p2 = parts[1].split(",");
                    World w = plugin.getServer().getWorld(parts[2]);
                    if (w == null) continue;
                    safeZones.put(key, Arrays.asList(
                            new Location(w, Double.parseDouble(p1[0]), Double.parseDouble(p1[1]), Double.parseDouble(p1[2])),
                            new Location(w, Double.parseDouble(p2[0]), Double.parseDouble(p2[1]), Double.parseDouble(p2[2]))
                    ));
                } catch (Exception e) {
                    plugin.getLogger().warning("Error parsing safe zone: " + key);
                }
            }
        }
    }

    private boolean isInSafeZone(Location loc) {
        if (safeZones.isEmpty()) return false;
        for (List<Location> zone : safeZones.values()) {
            Location l1 = zone.get(0);
            Location l2 = zone.get(1);
            if (!loc.getWorld().equals(l1.getWorld())) continue;
            double x = loc.getX(), y = loc.getY(), z = loc.getZ();
            if (x >= Math.min(l1.getX(), l2.getX()) && x <= Math.max(l1.getX(), l2.getX()) &&
                    y >= Math.min(l1.getY(), l2.getY()) && y <= Math.max(l1.getY(), l2.getY()) &&
                    z >= Math.min(l1.getZ(), l2.getZ()) && z <= Math.max(l1.getZ(), l2.getZ())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPvpDisabledInWG(Player player) {
        if (!plugin.isWorldGuardEnabled()) return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regions == null) return false;
            StateFlag.State pvpState = regions.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()).toVector().toBlockPoint())
                    .queryState(null, Flags.PVP);
            return pvpState == StateFlag.State.DENY;
        } catch (Exception e) { return false; }
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;

        int modelData = item.getItemMeta().getCustomModelData();
        Player player = event.getPlayer();

        SwordData sword = plugin.getSwordManager().getSwordByModel(modelData);
        if (sword == null) return;

        if (!sword.isEnabled()) {
            plugin.getLanguageManager().sendMessage(player, "feature-disabled");
            return;
        }

        if (!player.hasPermission("avsword.bypass.cooldown")) {
            if (isInSafeZone(player.getLocation())) {
                plugin.getLanguageManager().sendMessage(player, "safe-zone");
                return;
            }
            if (isPvpDisabledInWG(player)) {
                plugin.getLanguageManager().sendMessage(player, "safe-zone-pvp");
                return;
            }

            long secondsLeft = plugin.getCooldownManager().getRemainingSeconds(player, modelData, sword.getCooldown());
            if (secondsLeft > 0) {
                plugin.getLanguageManager().sendMessage(player, "cooldown-wait", "%seconds%", String.valueOf(secondsLeft));
                return;
            }
        }

        boolean anySuccess = false;
        boolean targetingFailed = false;

        for (String effectType : sword.getEffectTypes()) {
            boolean currentSuccess = false;

            switch (effectType) {
                case "ENDERMAN": currentSuccess = handleEnderman(player, sword); break;
                case "DRAGON":   currentSuccess = handleDragon(player, sword); break;
                case "SPIDER":   currentSuccess = handleSpider(player, sword); break;
                case "PHANTOM":  currentSuccess = handlePhantom(player, sword); break;
                case "NONE":     break;
                default:         currentSuccess = handlePotionEffect(player, sword, effectType); break;
            }

            if (currentSuccess) {
                anySuccess = true;
                if (Arrays.asList("ENDERMAN", "DRAGON", "SPIDER", "PHANTOM").contains(effectType)) {
                    plugin.getLanguageManager().sendMessage(player, "success." + effectType.toLowerCase());
                }
            } else {
                if (!Arrays.asList("NONE", "ENDERMAN", "DRAGON").contains(effectType)) {
                    targetingFailed = true;
                }
            }
        }

        if (anySuccess) {
            if (sword.getSound() != null) {
                player.playSound(player.getLocation(), sword.getSound(), sword.getVolume(), sword.getPitch());
            }
            if (!player.hasPermission("avsword.bypass.cooldown")) {
                plugin.getCooldownManager().setCooldown(player, sword.getCustomModelData());
            }
        } else if (targetingFailed) {
            plugin.getLanguageManager().sendMessage(player, "no-target-general");
        }
    }

    private boolean handleEnderman(Player player, SwordData sword) {
        int dist = sword.getTeleportDistance();
        if (dist <= 0) return false;

        Location eye = player.getEyeLocation();
        Location target = eye.clone().add(eye.getDirection().multiply(dist));

        Block block = target.getBlock();
        if (block.getType().isSolid()) {
            plugin.getLanguageManager().sendMessage(player, "teleport-invalid");
            return false;
        }
        player.teleport(target.setDirection(player.getLocation().getDirection()));
        return true;
    }

    private boolean handleDragon(Player player, SwordData sword) {
        DragonFireball fireball = player.launchProjectile(DragonFireball.class);
        dragonFireballDurations.put(fireball.getUniqueId(), sword.getBreathDuration());
        return true;
    }

    private boolean handleSpider(Player player, SwordData sword) {
        int count = 0;
        int r = sword.getRadius();
        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Player)) continue;
            Player target = (Player) e;
            if (sword.isTargetEnemyOnly() && target.equals(player)) continue;
            if (isInSafeZone(target.getLocation()) || isPvpDisabledInWG(target)) continue;

            Block b = target.getLocation().getBlock();
            if (b.getType() == Material.AIR) {
                tempBlocks.put(b.getLocation(), b.getType());
                b.setType(Material.COBWEB);

                new BukkitRunnable() {
                    @Override public void run() {
                        if (tempBlocks.containsKey(b.getLocation())) {
                            b.setType(tempBlocks.get(b.getLocation()));
                            tempBlocks.remove(b.getLocation());
                        }
                    }
                }.runTaskLater(plugin, sword.getEffectDuration());

                target.sendMessage(plugin.getLanguageManager().getMessage("effect_msg.spider-victim"));
                count++;
            }
        }
        return count > 0;
    }

    private boolean handlePhantom(Player player, SwordData sword) {
        int count = 0;
        int r = sword.getRadius();
        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Player)) continue;
            Player target = (Player) e;
            if (sword.isTargetEnemyOnly() && target.equals(player)) continue;
            if (isInSafeZone(target.getLocation()) || isPvpDisabledInWG(target)) continue;

            if (target.isGliding()) target.setGliding(false);
            elytraDisabled.put(target.getUniqueId(), System.currentTimeMillis() + (sword.getEffectDuration() * 50L));
            target.sendMessage(plugin.getLanguageManager().getMessage("effect_msg.phantom-victim"));
            count++;
        }
        return count > 0;
    }

    private boolean handlePotionEffect(Player player, SwordData sword, String effectName) {
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type == null) return false;

        int count = 0;
        int r = sword.getRadius();
        PotionEffect effect = new PotionEffect(type, sword.getEffectDuration(), Math.max(0, sword.getEffectLevel() - 1));

        if (!sword.isTargetEnemyOnly() && !isInSafeZone(player.getLocation())) {
            player.addPotionEffect(effect);
            count++;
        }

        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Player)) continue;
            Player target = (Player) e;
            if (target.equals(player)) continue;
            if (isInSafeZone(target.getLocation()) || isPvpDisabledInWG(target)) continue;

            target.addPotionEffect(effect);
            count++;
        }
        return count > 0;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof DragonFireball) {
            UUID id = event.getEntity().getUniqueId();
            if (dragonFireballDurations.containsKey(id)) {
                int duration = dragonFireballDurations.remove(id);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location loc = event.getHitBlock() != null ? event.getHitBlock().getLocation().add(0.5,0.5,0.5)
                                : event.getHitEntity() != null ? event.getHitEntity().getLocation() : event.getEntity().getLocation();

                        loc.getWorld().getNearbyEntities(loc, 4, 4, 4, e -> e.getType() == EntityType.AREA_EFFECT_CLOUD).stream()
                                .filter(e -> e.getTicksLived() < 10)
                                .map(e -> (AreaEffectCloud) e)
                                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
                                .ifPresent(cloud -> cloud.setDuration(duration));
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player) {
            UUID id = event.getEntity().getUniqueId();
            if (elytraDisabled.containsKey(id)) {
                if (System.currentTimeMillis() < elytraDisabled.get(id)) {
                    if (event.isGliding()) event.setCancelled(true);
                } else {
                    elytraDisabled.remove(id);
                }
            }
        }
    }
}