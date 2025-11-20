package org.duvo.avsword;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class AvSword extends JavaPlugin {

    private static AvSword instance;
    private LanguageManager languageManager;
    private SwordManager swordManager;
    private CooldownManager cooldownManager;
    private SwordListener swordListener;
    private boolean worldGuardEnabled = false;


    public String latestVersion = null;


    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.languageManager = new LanguageManager(this);
        this.cooldownManager = new CooldownManager();
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

            new UpdateChecker(this, "avswords").getVersion(version -> {
                if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                    this.latestVersion = version;
                    getLogger().warning("A new update is available! (v" + version + ")");
                    getLogger().warning("Please check the Modrinth page: https://modrinth.com/project/avswords");
                }
            });
        }


        int pluginId = 28037;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new org.bstats.charts.SimplePie("chart_language", () ->
                getConfig().getString("language", "en")
        ));

        getLogger().info("AvSword enabled with bStats (ID: 28037)!");
    }

    public void reloadPlugin() {
        reloadConfig();
        languageManager.loadMessages();
        swordManager.loadSwords();
        getLogger().info("AvSword configuration reloaded.");
    }

    @Override
    public void onDisable() {

        if (swordListener != null) {
            swordListener.cleanupTemporaryBlocks();
        }

        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
        getLogger().info("AvSword disabled.");
    }

    public static AvSword getInstance() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public SwordManager getSwordManager() { return swordManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public boolean isWorldGuardEnabled() { return worldGuardEnabled; }
}