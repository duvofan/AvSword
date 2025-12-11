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
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SwordListener implements Listener {

    private final AvSword plugin;
    private final Map<String, List<Location>> safeZones = new HashMap<>();
    private final Map<UUID, Integer> dragonFireballDurations = new HashMap<>();
    private final Map<UUID, Long> elytraDisabled = new HashMap<>();
    private final Map<UUID, Long> antiSpam = new HashMap<>();
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();
    private final File tempBlocksFile;
    private final YamlConfiguration tempBlocksConfig;
    private boolean isTempBlocksDirty = false;

    public SwordListener(AvSword plugin) {
        this.plugin = plugin;

        this.tempBlocksFile = new File(plugin.getDataFolder(), "tempblocks.yml");
        if (!tempBlocksFile.exists()) {
            try { tempBlocksFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.tempBlocksConfig = YamlConfiguration.loadConfiguration(tempBlocksFile);

        restoreCrashBlocks();
        loadSafeZones();
        startAutoSaveTask();
    }

    private void startAutoSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isTempBlocksDirty) {
                    saveTempBlocksConfig();
                    isTempBlocksDirty = false;
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    private void restoreCrashBlocks() {
        if (!tempBlocksConfig.contains("blocks")) return;

        int count = 0;
        Set<String> keys = new HashSet<>();
        if (tempBlocksConfig.getConfigurationSection("blocks") != null) {
            keys = tempBlocksConfig.getConfigurationSection("blocks").getKeys(false);
        }

        for (String key : keys) {
            String path = "blocks." + key;
            try {
                String worldName = tempBlocksConfig.getString(path + ".world");
                if (worldName == null) continue;

                World world = plugin.getServer().getWorld(worldName);
                double x = tempBlocksConfig.getDouble(path + ".x");
                double y = tempBlocksConfig.getDouble(path + ".y");
                double z = tempBlocksConfig.getDouble(path + ".z");
                String typeName = tempBlocksConfig.getString(path + ".type");

                if (world != null && typeName != null) {
                    Material originalType = Material.valueOf(typeName);
                    Location loc = new Location(world, x, y, z);
                    if (loc.getBlock().getType() == Material.COBWEB) {
                        loc.getBlock().setType(originalType);
                    }
                }
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("Hata: Geçici blok geri yüklenemedi: " + key);
            }
        }

        tempBlocksConfig.set("blocks", null);
        saveTempBlocksConfig();
        isTempBlocksDirty = false;

        if (count > 0) {
            plugin.getLogger().info("Crash kurtarma: " + count + " adet geçici blok temizlendi.");
        }
    }

    private void saveBlockToDisk(Location loc, Material originalType) {
        String key = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        String path = "blocks." + key;
        tempBlocksConfig.set(path + ".world", loc.getWorld().getName());
        tempBlocksConfig.set(path + ".x", loc.getX());
        tempBlocksConfig.set(path + ".y", loc.getY());
        tempBlocksConfig.set(path + ".z", loc.getZ());
        tempBlocksConfig.set(path + ".type", originalType.name());

        isTempBlocksDirty = true;
    }

    private void removeBlockFromDisk(Location loc) {
        String key = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        tempBlocksConfig.set("blocks." + key, null);

        isTempBlocksDirty = true;
    }

    private void saveTempBlocksConfig() {
        try {
            tempBlocksConfig.save(tempBlocksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("TempBlocks dosyası kaydedilemedi: " + e.getMessage());
        }
    }

    public void cleanupTemporaryBlocks() {
        restoreCrashBlocks();
    }

    private void sendTimedMessage(Player victim, String messageKey) {
        long now = System.currentTimeMillis();
        long lastMsg = messageCooldowns.getOrDefault(victim.getUniqueId(), 0L);
        if (now - lastMsg > 2000) {
            victim.sendMessage(plugin.getLanguageManager().getMessage(messageKey));
            messageCooldowns.put(victim.getUniqueId(), now);
        }
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {
            if (plugin.latestVersion == null) {
                new UpdateChecker(plugin, "avsword").getVersion(version -> {
                    plugin.latestVersion = version;
                    String currentVersion = plugin.getDescription().getVersion();
                    if (!currentVersion.equalsIgnoreCase(version)) {
                        sendUpdateMessage(player, version);
                    }
                });
            } else {
                String currentVersion = plugin.getDescription().getVersion();
                if (!currentVersion.equalsIgnoreCase(plugin.latestVersion)) {
                    sendUpdateMessage(player, plugin.latestVersion);
                }
            }
        }
    }

    private void sendUpdateMessage(Player player, String version) {
        String lang = plugin.getConfig().getString("language", "en");
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.sendMessage("");
                if ("tr".equalsIgnoreCase(lang)) {
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.YELLOW + "Yeni bir güncelleme mevcut! " + ChatColor.GRAY + "(v" + version + ")");
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.AQUA + "" + ChatColor.UNDERLINE + "https://modrinth.com/project/avsword");
                } else {
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.YELLOW + "A new update is available! " + ChatColor.GRAY + "(v" + version + ")");
                    player.sendMessage(ChatColor.GOLD + "AvSword " + ChatColor.DARK_GRAY + "» " + ChatColor.AQUA + "" + ChatColor.UNDERLINE + "https://modrinth.com/project/avsword");
                }
                player.sendMessage("");
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCooldownManager().removePlayer(event.getPlayer());
        antiSpam.remove(event.getPlayer().getUniqueId());
        messageCooldowns.remove(event.getPlayer().getUniqueId());
    }

    public void loadSafeZones() {
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

        Player player = event.getPlayer();

        long lastClick = antiSpam.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        if (now - lastClick < 300) {
            return;
        }

        if (isVanished(player) && !player.hasPermission("avsword.bypass.vanish")) {
            plugin.getLanguageManager().sendMessage(player, "feature-disabled-vanish");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;

        int modelData = item.getItemMeta().getCustomModelData();

        SwordData sword = plugin.getSwordManager().getSwordByModel(modelData);
        if (sword == null) return;

        antiSpam.put(player.getUniqueId(), now);

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

        String swordKey = sword.getKey();
        boolean hasCustomMessage = plugin.getLanguageManager().hasMessage("success." + swordKey.toLowerCase());

        for (String effectType : sword.getEffectTypes()) {
            boolean currentSuccess = false;

            switch (effectType) {
                case "ENDERMAN": currentSuccess = handleEnderman(player, sword); break;
                case "DRAGON":   currentSuccess = handleDragon(player, sword); break;
                case "SPIDER":   currentSuccess = handleSpider(player, sword); break;
                case "PHANTOM":  currentSuccess = handlePhantom(player, sword); break;
                case "CREEPER":  currentSuccess = handleCreeper(player, sword); break;
                case "GHAST":    currentSuccess = handleGhast(player, sword); break;
                case "EVOKER":   currentSuccess = handleEvoker(player, sword); break;

                case "NONE":     break;
                default:         currentSuccess = handlePotionEffect(player, sword, effectType); break;
            }

            if (currentSuccess) {
                anySuccess = true;
                if (!hasCustomMessage) {
                    if (Arrays.asList("ENDERMAN", "DRAGON", "SPIDER", "PHANTOM", "CREEPER", "GHAST", "EVOKER").contains(effectType)) {
                        plugin.getLanguageManager().sendMessage(player, "success." + effectType.toLowerCase());
                    }
                }
            } else {
                if (!Arrays.asList("NONE", "ENDERMAN", "DRAGON", "CREEPER", "GHAST").contains(effectType)) {
                    targetingFailed = true;
                }
            }
        }

        if (anySuccess) {
            event.setCancelled(true);

            if (sword.getSound() != null) {
                player.playSound(player.getLocation(), sword.getSound(), sword.getVolume(), sword.getPitch());
            }

            if (hasCustomMessage) {
                plugin.getLanguageManager().sendMessage(player, "success." + swordKey.toLowerCase());
            }

            if (!player.hasPermission("avsword.bypass.cooldown")) {
                plugin.getCooldownManager().setCooldown(player, sword.getCustomModelData());
            }
        } else if (targetingFailed) {
            plugin.getLanguageManager().sendMessage(player, "no-target-general");
        }
    }

    private boolean handleGhast(Player player, SwordData sword) {
        LargeFireball ghastFireball = player.launchProjectile(LargeFireball.class);
        ghastFireball.setYield((float) sword.getGhastPower());
        ghastFireball.setIsIncendiary(false);
        ghastFireball.setMetadata("avsword_projectile", new FixedMetadataValue(plugin, true));
        return true;
    }

    private boolean handleEvoker(Player player, SwordData sword) {
        int r = sword.getRadius();
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity) e;

            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                if (targetPlayer.equals(player)) continue;
                if (plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) continue;
                if (isVanished(targetPlayer)) continue;
                if (isInSafeZone(targetPlayer.getLocation())) continue;
                if (isPvpDisabledInWG(targetPlayer)) continue;
            }
            targets.add(target);
        }

        if (targets.isEmpty()) return false;

        for (LivingEntity target : targets) {
            target.getWorld().spawnEntity(target.getLocation(), EntityType.EVOKER_FANGS);
        }

        return true;
    }

    private boolean handleCreeper(Player player, SwordData sword) {
        int radius = sword.getRadius();
        double launchPower = sword.getCreeperPower();
        double damageAmount = sword.getCreeperDamage();

        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);

        List<LivingEntity> validTargets = new ArrayList<>();

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity) e;

            if (target instanceof Player) {
                if (plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) continue;
                if (isVanished((Player) target)) continue;
                if (sword.isTargetEnemyOnly() && target.equals(player)) continue;
                if (isInSafeZone(target.getLocation())) continue;
                if (isPvpDisabledInWG((Player) target)) continue;
            }

            validTargets.add(target);
        }

        if (validTargets.isEmpty()) return false;

        for (LivingEntity target : validTargets) {
            if (damageAmount > 0) {
                target.damage(damageAmount, player);
            }

            Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector());
            direction.setY(0);

            if (direction.lengthSquared() < 0.0001) {
                direction = player.getLocation().getDirection().setY(0);
            }

            direction.normalize();
            direction.multiply(launchPower * 0.8);
            direction.setY(launchPower);

            final Vector finalVelocity = direction;

            if (target instanceof Player) {
                sendTimedMessage((Player) target, "effect_msg.creeper-victim");
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isValid() && !target.isDead()) {
                        target.setFallDistance(0);
                        target.setVelocity(finalVelocity);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }

        return true;
    }

    private boolean handleEnderman(Player player, SwordData sword) {

        Location playerLocBelow = player.getLocation().clone().subtract(0, 1, 0);
        if (!playerLocBelow.getBlock().getType().isSolid()) {
            plugin.getLanguageManager().sendMessage(player, "teleport-invalid");
            return false;
        }

        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();
        int maxDist = sword.getTeleportDistance();


        Location currentTarget = startLoc.clone();

        for (int i = 1; i <= maxDist; i++) {
            Location checkLoc = startLoc.clone().add(direction.clone().multiply(i));

            if (checkLoc.getBlock().getType() == Material.BARRIER) {
                currentTarget = startLoc.clone().add(direction.clone().multiply(i - 1));
                break;
            }


            currentTarget = checkLoc;
        }




        Location finalTeleportLoc = null;
        int searchDepth = 3;

        for (int yOffset = 0; yOffset <= searchDepth; yOffset++) {
            Location checkLoc = currentTarget.clone().subtract(0, yOffset, 0);

            Block blockBelow = checkLoc.clone().subtract(0, 1, 0).getBlock();
            Block blockCurrent = checkLoc.getBlock();
            Block blockAbove = checkLoc.clone().add(0, 1, 0).getBlock();

            if (blockBelow.getType().isSolid() &&
                    blockBelow.getType() != Material.BARRIER &&
                    (blockCurrent.getType() == Material.AIR || blockCurrent.isLiquid()) &&
                    (blockAbove.getType() == Material.AIR || blockAbove.isLiquid())) {

                finalTeleportLoc = checkLoc;

                finalTeleportLoc.setDirection(player.getLocation().getDirection());

                break;
            }
        }

        if (finalTeleportLoc != null) {
            player.teleport(finalTeleportLoc);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.getWorld().playSound(finalTeleportLoc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return true;
        } else {

            plugin.getLanguageManager().sendMessage(player, "teleport-invalid");
            return false;
        }
    }

    private Location findSafeLocation(Location loc) {
        if (isSafeSpot(loc)) return loc;
        if (isSafeSpot(loc.clone().add(0, 1, 0))) return loc.clone().add(0, 1, 0);
        if (isSafeSpot(loc.clone().add(0, 2, 0))) return loc.clone().add(0, 2, 0);
        if (isSafeSpot(loc.clone().add(0, -1, 0))) return loc.clone().add(0, -1, 0);
        if (isSafeSpot(loc.clone().add(0, -2, 0))) return loc.clone().add(0, -2, 0);

        return null;
    }

    private boolean isSafeSpot(Location loc) {
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        return !feet.getType().isSolid() && !head.getType().isSolid();
    }

    private boolean handleDragon(Player player, SwordData sword) {
        DragonFireball fireball = player.launchProjectile(DragonFireball.class);

        fireball.setVelocity(player.getLocation().getDirection().multiply(0.3));

        dragonFireballDurations.put(fireball.getUniqueId(), sword.getBreathDuration());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (dragonFireballDurations.containsKey(fireball.getUniqueId())) {
                    dragonFireballDurations.remove(fireball.getUniqueId());
                    if (fireball.isValid()) {
                        fireball.remove();
                    }
                }
            }
        }.runTaskLater(plugin, 1200L);

        return true;
    }

    private boolean handleSpider(Player player, SwordData sword) {
        int count = 0;
        int r = sword.getRadius();
        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Player)) continue;
            Player target = (Player) e;

            if (plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) continue;

            if (isVanished(target)) continue;
            if (sword.isTargetEnemyOnly() && target.equals(player)) continue;
            if (isInSafeZone(target.getLocation()) || isPvpDisabledInWG(target)) continue;

            Block b = target.getLocation().getBlock();

            if (isSafeToReplace(b.getType())) {
                Material original = b.getType();
                saveBlockToDisk(b.getLocation(), original);

                b.setType(Material.COBWEB);

                new BukkitRunnable() {
                    @Override public void run() {
                        if (b.getType() == Material.COBWEB) {
                            b.setType(original);
                        }
                        removeBlockFromDisk(b.getLocation());
                    }
                }.runTaskLater(plugin, sword.getEffectDuration());
                sendTimedMessage(target, "effect_msg.spider-victim");
                count++;
            }
        }
        return count > 0;
    }

    private boolean isSafeToReplace(Material mat) {
        return mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR ||
                mat == Material.GRASS || mat == Material.TALL_GRASS || mat == Material.SNOW ||
                mat == Material.FERN || mat == Material.LARGE_FERN || mat == Material.DEAD_BUSH;
    }

    private boolean handlePhantom(Player player, SwordData sword) {
        int count = 0;
        int r = sword.getRadius();
        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Player)) continue;
            Player target = (Player) e;

            if (plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) continue;

            if (isVanished(target)) continue;
            if (sword.isTargetEnemyOnly() && target.equals(player)) continue;
            if (isInSafeZone(target.getLocation()) || isPvpDisabledInWG(target)) continue;

            if (target.isGliding()) target.setGliding(false);
            elytraDisabled.put(target.getUniqueId(), System.currentTimeMillis() + (sword.getEffectDuration() * 50L));

            sendTimedMessage(target, "effect_msg.phantom-victim");
            count++;
        }
        return count > 0;
    }

    private boolean isVanished(Player player) {
        return player.hasMetadata("vanished");
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

            if (plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) continue;

            if (isVanished(target)) continue;
            if (target.equals(player)) continue;
            if (isInSafeZone(target.getLocation()) || isPvpDisabledInWG(target)) continue;

            target.addPotionEffect(effect);
            count++;
        }
        return count > 0;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball)) {
            return;
        }

        DragonFireball fireball = (DragonFireball) event.getEntity();
        UUID fireballId = fireball.getUniqueId();

        if (dragonFireballDurations.containsKey(fireballId)) {
            int duration = dragonFireballDurations.remove(fireballId);
            ProjectileSource shooter = fireball.getShooter();

            new BukkitRunnable() {
                @Override
                public void run() {
                    Location impactLocation = event.getHitBlock() != null ? event.getHitBlock().getLocation().add(0.5, 0.5, 0.5)
                            : event.getHitEntity() != null ? event.getHitEntity().getLocation() : fireball.getLocation();

                    impactLocation.getWorld().getNearbyEntities(impactLocation, 4, 4, 4, e -> e.getType() == EntityType.AREA_EFFECT_CLOUD).stream()
                            .filter(e -> e.getTicksLived() < 10)
                            .map(e -> (AreaEffectCloud) e)
                            .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(impactLocation)))
                            .ifPresent(cloud -> {
                                cloud.setDuration(duration);
                                if (shooter != null) {
                                    cloud.setSource(shooter);
                                }
                            });
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();

        if (event.getDamager() instanceof LargeFireball) {
            LargeFireball fireball = (LargeFireball) event.getDamager();

            if (fireball.hasMetadata("avsword_projectile")) {
                if (fireball.getShooter() instanceof Player) {
                    Player shooter = (Player) fireball.getShooter();
                    if (shooter.getUniqueId().equals(victim.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }
                    if (plugin.getFriendManager().isFriend(shooter.getUniqueId(), victim.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        if (event.getDamager() instanceof AreaEffectCloud) {
            AreaEffectCloud cloud = (AreaEffectCloud) event.getDamager();
            ProjectileSource source = cloud.getSource();

            if (source instanceof Player) {
                Player attacker = (Player) source;
                if (plugin.getFriendManager().isFriend(attacker.getUniqueId(), victim.getUniqueId())) {
                    event.setCancelled(true);
                }
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

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof LargeFireball) {
            if (event.getEntity().hasMetadata("avsword_projectile")) {
                event.blockList().clear();
            }
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() != null && event.getIgnitingEntity().hasMetadata("avsword_projectile")) {
            event.setCancelled(true);
        }
    }
}