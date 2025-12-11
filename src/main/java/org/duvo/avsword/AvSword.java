package org.duvo.avsword;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.duvo.avsword.Friend.FriendManager;

public class AvSword extends JavaPlugin {

    private static AvSword instance;
    private LanguageManager languageManager;
    private SwordManager swordManager;
    private CooldownManager cooldownManager;
    private SwordListener swordListener;
    private FriendManager friendManager;
    private boolean worldGuardEnabled = false;

    public String latestVersion = null;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.languageManager = new LanguageManager(this);
        this.cooldownManager = new CooldownManager();
        this.friendManager = new FriendManager(this);
        this.swordManager = new SwordManager(this);

        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.worldGuardEnabled = true;
            getLogger().info("WorldGuard detected. Region protection is active.");
        }

        this.swordListener = new SwordListener(this);
        getServer().getPluginManager().registerEvents(this.swordListener, this);

        getCommand("avsword").setExecutor(new SwordCommand(this));
        getCommand("avsword").setTabCompleter(new SwordCommand(this));

        if (getConfig().getBoolean("check-updates", true)) {
            new UpdateChecker(this, "avsword").getVersion(version -> {
                if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                    this.latestVersion = version;
                    getLogger().warning("A new update is available! (v" + version + ")");
                    getLogger().warning("Please check the Modrinth page: https://modrinth.com/project/avsword");
                }
            });
        }

        int pluginId = 28037;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new org.bstats.charts.SimplePie("chart_language", () ->
                getConfig().getString("language", "en")
        ));

        getLogger().info("AvSword enabled!");
    }

    public void reloadPlugin() {
        getLogger().info("Reloading AvSword plugin...");

        if (swordListener != null) {
            swordListener.cleanupTemporaryBlocks();
            getLogger().info("Temporary blocks cleaned up.");
        }

        if (friendManager != null) {
            friendManager.saveFriends();
        }

        reloadConfig();
        languageManager.loadMessages();

        if (friendManager != null) {
            friendManager.loadFriends();
            getLogger().info("Friend list reloaded.");
        }

        swordManager.loadSwords();

        if (swordListener != null) {
            swordListener.loadSafeZones();
            getLogger().info("Safe zones reloaded.");
        }

        if (cooldownManager != null) {
            cooldownManager.clearAll();
            getLogger().info("All cooldowns cleared.");
        }
        getLogger().info("AvSword successfully reloaded!");
    }

    @Override
    public void onDisable() {
        if (swordListener != null) {
            swordListener.cleanupTemporaryBlocks();
        }

        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }

        if (friendManager != null) {
            friendManager.saveFriends();
        }

        getLogger().info("AvSword disabled.");
    }

    public static AvSword getInstance() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public SwordManager getSwordManager() { return swordManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public boolean isWorldGuardEnabled() { return worldGuardEnabled; }
}
