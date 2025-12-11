package org.duvo.avsword;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;
import org.duvo.avsword.Friend.FriendSubCommand;

import java.util.*;
import java.util.stream.Collectors;

public class SwordCommand implements CommandExecutor, TabCompleter {

    private final AvSword plugin;
    private final FriendSubCommand friendSubCommand;

    private final List<String> SPECIAL_EFFECTS = Arrays.asList("ENDERMAN", "DRAGON", "SPIDER", "PHANTOM", "CREEPER", "GHAST", "EVOKER", "NONE");

    private final List<String> SETTINGS = Arrays.asList(
            "cooldown", "custom-model-data", "effect", "radius",
            "teleport-distance", "effect-level", "effect-duration",
            "enabled", "target-enemy-only", "breath-duration",
            "creeper-power", "creeper-damage", "ghast-power",
            "sound.volume", "sound.pitch"
    );

    public SwordCommand(AvSword plugin) {
        this.plugin = plugin;
        this.friendSubCommand = new FriendSubCommand(plugin);
    }

    private void sendMsgAny(CommandSender sender, String key, String... replacements) {
        if (sender instanceof Player) {
            plugin.getLanguageManager().sendMessage((Player)sender, key, replacements);
        } else {
            String msg = plugin.getLanguageManager().getMessage(key);
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) msg = msg.replace(replacements[i], replacements[i+1]);
            }
            sender.sendMessage(plugin.getLanguageManager().getPrefix() + msg);
        }
    }

    private String getMsgRaw(String key, String... replacements) {
        String msg = plugin.getLanguageManager().getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) msg = msg.replace(replacements[i], replacements[i+1]);
        }
        return msg;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private boolean isValidEffect(String effect) {
        if (SPECIAL_EFFECTS.contains(effect)) return true;
        return PotionEffectType.getByName(effect) != null;
    }

    private SwordData findSwordData(String name) {
        if (plugin.getConfig().getConfigurationSection("swords") == null) return null;
        for (String key : plugin.getConfig().getConfigurationSection("swords").getKeys(false)) {
            if (key.equalsIgnoreCase(name)) {
                int modelData = plugin.getConfig().getInt("swords." + key + ".custom-model-data");
                return plugin.getSwordManager().getSwordByModel(modelData);
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("avsword.admin")) {
            sendMsgAny(sender, "command.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload": case "yenile":
                plugin.reloadPlugin();
                sendMsgAny(sender, "command.reload");
                break;
            case "version": case "sürüm": case "surum":
                String version = plugin.getDescription().getVersion();
                String lang = plugin.getConfig().getString("language", "en");
                if ("tr".equalsIgnoreCase(lang)) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefix() + ChatColor.YELLOW + "Mevcut Sürüm: " + ChatColor.WHITE + "v" + version);
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getPrefix() + ChatColor.YELLOW + "Current Version: " + ChatColor.WHITE + "v" + version);
                }
                break;
            case "list": case "liste":
                sendMsgAny(sender, "command.list-header");
                if (plugin.getConfig().getConfigurationSection("swords") != null) {
                    plugin.getConfig().getConfigurationSection("swords").getKeys(false).forEach(key ->
                            sender.sendMessage(ChatColor.YELLOW + "- " + key));
                }
                break;
            case "give": case "ver": handleGive(sender, args); break;
            case "bind": case "bagla": handleBind(sender, args); break;
            case "create": case "olustur": handleCreate(sender, args); break;
            case "edit": case "duzenle": handleEdit(sender, args); break;
            case "info": case "bilgi": handleInfo(sender, args); break;
            case "friend":
            case "arkadas":
                friendSubCommand.handle(sender, args);
                break;
            default: sendHelp(sender); break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("avsword.admin")) return Collections.emptyList();

        List<String> suggestions = new ArrayList<>();
        List<String> completions = new ArrayList<>();
        String lang = plugin.getConfig().getString("language", "en");
        boolean isTr = "tr".equalsIgnoreCase(lang);
        if (args.length == 1) {
            if (isTr) {
                suggestions.addAll(Arrays.asList("ver", "bagla", "yenile", "liste", "olustur", "duzenle", "bilgi", "arkadas", "sürüm"));
            } else {
                suggestions.addAll(Arrays.asList("give", "bind", "reload", "list", "create", "edit", "info", "friend", "version"));
            }
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
            Collections.sort(completions);
            return completions;
        }

        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (Arrays.asList("give", "ver").contains(sub)) {
                return null;
            }

            if (Arrays.asList("info", "bilgi", "bind", "bagla", "edit", "duzenle").contains(sub)) {
                if (plugin.getConfig().getConfigurationSection("swords") != null) {
                    suggestions.addAll(plugin.getConfig().getConfigurationSection("swords").getKeys(false));
                }
            }
            else if (Arrays.asList("create", "olustur").contains(sub)) {
                suggestions.add(isTr ? "<isim>" : "<name>");
            }
            else if (sub.equals("friend") || sub.equals("arkadas")) {
                if (isTr) {
                    suggestions.addAll(Arrays.asList("ekle", "cikar", "liste"));
                } else {
                    suggestions.addAll(Arrays.asList("add", "remove", "list"));
                }
            }

            StringUtil.copyPartialMatches(args[1], suggestions, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 3) {
            if (Arrays.asList("give", "ver").contains(sub)) {
                if (plugin.getConfig().getConfigurationSection("swords") != null) {
                    suggestions.addAll(plugin.getConfig().getConfigurationSection("swords").getKeys(false));
                }
            }
            else if (Arrays.asList("edit", "duzenle").contains(sub)) {
                suggestions.addAll(SETTINGS);
            }
            else if (Arrays.asList("create", "olustur").contains(sub)) {
                suggestions.add("<model_id>");
            }
            else if (Arrays.asList("friend", "arkadas").contains(sub)) {
                String action = args[1].toLowerCase();
                if (Arrays.asList("add", "ekle", "remove", "cikar", "delete", "sil").contains(action)) {
                    return null;
                }
            }

            StringUtil.copyPartialMatches(args[2], suggestions, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 4) {
            if (Arrays.asList("create", "olustur").contains(sub)) {
                suggestions.addAll(SPECIAL_EFFECTS);
                for (PotionEffectType type : PotionEffectType.values()) suggestions.add(type.getName());
            }

            if (Arrays.asList("edit", "duzenle").contains(sub)) {
                String setting = args[2].toLowerCase();

                if (setting.equals("enabled") || setting.equals("target-enemy-only")) {
                    suggestions.addAll(Arrays.asList("true", "false"));
                }
                else if (setting.equals("effect")) {
                    String currentArg = args[3].toUpperCase();
                    String prefix = "";
                    String toMatch = currentArg;

                    if (currentArg.contains(",")) {
                        int lastComma = currentArg.lastIndexOf(",");
                        prefix = currentArg.substring(0, lastComma + 1);
                        toMatch = currentArg.substring(lastComma + 1);
                    }

                    List<String> allEffects = new ArrayList<>(SPECIAL_EFFECTS);
                    for (PotionEffectType type : PotionEffectType.values()) {
                        allEffects.add(type.getName());
                    }

                    for (String eff : allEffects) {
                        if (eff.startsWith(toMatch)) {
                            suggestions.add(prefix + eff);
                        }
                    }
                    return suggestions;
                }
                else if (setting.equals("custom-model-data")) {
                    suggestions.addAll(Arrays.asList("101", "102", "103", "104", "105"));
                }
                else if (setting.contains("volume") || setting.contains("pitch") || setting.contains("power") || setting.contains("damage")) {
                    suggestions.addAll(Arrays.asList("0.5", "1.0", "1.5", "2.0", "3.0", "5.0"));
                }
                else if (setting.contains("duration") || setting.contains("cooldown") || setting.contains("distance") || setting.contains("radius")) {
                    suggestions.addAll(Arrays.asList("5", "10", "15", "20", "30", "45", "60"));
                }
                else {
                    suggestions.addAll(Arrays.asList("<değer>", "10", "5"));
                }
            }

            StringUtil.copyPartialMatches(args[3], suggestions, completions);
            Collections.sort(completions);
            return completions;
        }

        return Collections.emptyList();
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMsgAny(sender, "command.sword-not-found", "%sword%", "???");
            return;
        }

        SwordData data = findSwordData(args[1]);
        if (data == null) {
            sendMsgAny(sender, "command.sword-not-found", "%sword%", args[1]);
            return;
        }

        String lang = plugin.getConfig().getString("language", "en");
        boolean isTr = "tr".equalsIgnoreCase(lang);

        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
        sender.sendMessage(ChatColor.GOLD + " AvSword Info: " + ChatColor.YELLOW + capitalize(data.getKey()));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Durum:" : "Status:") + " " + (data.isEnabled() ? ChatColor.GREEN + (isTr ? "Aktif" : "Enabled") : ChatColor.RED + (isTr ? "Pasif" : "Disabled")));
        sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Model ID:" : "Model ID:") + " " + ChatColor.AQUA + data.getCustomModelData());
        sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Bekleme Süresi:" : "Cooldown:") + " " + ChatColor.AQUA + data.getCooldown() + "s");
        sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Efektler:" : "Effects:") + " " + ChatColor.LIGHT_PURPLE + String.join(", ", data.getEffectTypes()));

        if (!data.getEffectTypes().contains("NONE")) {
            if (!data.getEffectTypes().contains("CREEPER") && !data.getEffectTypes().contains("GHAST")) {
                sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Süre/Seviye:" : "Duration/Level:") + " " + ChatColor.WHITE + (data.getEffectDuration() / 20) + "s / Lvl " + data.getEffectLevel());
            }
            if (data.getEffectTypes().contains("EVOKER")) {
                sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Yarıçap:" : "Radius:") + " " + ChatColor.WHITE + data.getRadius() + " blocks");
            } else if (!data.getEffectTypes().contains("GHAST") && !data.getEffectTypes().contains("CREEPER")) {
                sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Yarıçap:" : "Radius:") + " " + ChatColor.WHITE + data.getRadius() + " blocks");
            }
        }

        if (data.getEffectTypes().contains("ENDERMAN")) {
            sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Işınlanma:" : "Teleport:") + " " + ChatColor.GREEN + data.getTeleportDistance() + " blocks");
        }
        if (data.getEffectTypes().contains("DRAGON")) {
            sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Ejderha Nefesi:" : "Breath Time:") + " " + ChatColor.GREEN + data.getBreathDuration() + "s");
        }
        if (data.getEffectTypes().contains("CREEPER")) {
            sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Fırlatma:" : "Launch Power:") + " " + ChatColor.RED + data.getCreeperPower());
            sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Patlama Hasarı:" : "Explosion Dmg:") + " " + ChatColor.RED + data.getCreeperDamage());
        }
        if (data.getEffectTypes().contains("GHAST")) {
            sender.sendMessage(ChatColor.GRAY + " " + (isTr ? "Top Gücü:" : "Fireball Power:") + " " + ChatColor.RED + data.getGhastPower());
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) { sendMsgAny(sender, "command.create-usage"); return; }
        String name = args[1];
        if (plugin.getConfig().contains("swords." + name)) { sendMsgAny(sender, "command.create-exists", "%sword%", name); return; }
        int modelData;
        try {
            modelData = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Model ID numeric olmalıdır.");
            return;
        }
        String effect = args[3].toUpperCase();

        if (!isValidEffect(effect)) { sendMsgAny(sender, "command.invalid-effect"); return; }

        boolean success = plugin.getSwordManager().createSword(name, modelData, effect);

        if (success) {
            sendMsgAny(sender, "command.create-success", "%sword%", name, "%model%", String.valueOf(modelData));
        } else {
            sender.sendMessage(ChatColor.RED + "Hata: Bu Model ID (" + modelData + ") başka bir kılıç tarafından kullanılıyor!");
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (args.length < 4) { sendMsgAny(sender, "command.edit-usage"); return; }
        String name = args[1];
        SwordData data = findSwordData(name);
        if (data == null) { sendMsgAny(sender, "command.sword-not-found", "%sword%", name); return; }

        String setting = args[2].toLowerCase();
        String valueStr = args[3];

        List<String> currentEffects = data.getEffectTypes();

        if (setting.equals("teleport-distance") && !currentEffects.contains("ENDERMAN")) {
            sender.sendMessage(ChatColor.RED + "Hata: 'teleport-distance' ayarı için kılıçta ENDERMAN efekti olmalı!");
            return;
        }
        if (setting.equals("breath-duration") && !currentEffects.contains("DRAGON")) {
            sender.sendMessage(ChatColor.RED + "Hata: 'breath-duration' ayarı için kılıçta DRAGON efekti olmalı!");
            return;
        }
        if ((setting.equals("creeper-power") || setting.equals("creeper-damage")) && !currentEffects.contains("CREEPER")) {
            sender.sendMessage(ChatColor.RED + "Hata: '" + setting + "' ayarı için kılıçta CREEPER efekti olmalı!");
            return;
        }
        if (setting.equals("ghast-power") && !currentEffects.contains("GHAST")) {
            sender.sendMessage(ChatColor.RED + "Hata: 'ghast-power' ayarı için kılıçta GHAST efekti olmalı!");
            return;
        }

        Object finalValue = null;
        String unit = "";

        try {
            if (setting.equals("enabled") || setting.equals("target-enemy-only")) {
                if (!valueStr.equalsIgnoreCase("true") && !valueStr.equalsIgnoreCase("false")) {
                    sendMsgAny(sender, "command.edit-invalid-bool");
                    return;
                }
                finalValue = Boolean.parseBoolean(valueStr);
            }
            else if (setting.equals("effect")) {
                if (valueStr.contains(",")) {
                    String[] splits = valueStr.split(",");
                    List<String> newEffects = new ArrayList<>();
                    for(String s : splits) {
                        String clean = s.trim().toUpperCase();
                        if(isValidEffect(clean)) newEffects.add(clean);
                    }
                    if(newEffects.isEmpty()) { sendMsgAny(sender, "command.invalid-effect"); return; }
                    finalValue = newEffects;
                } else {
                    if (!isValidEffect(valueStr.toUpperCase())) { sendMsgAny(sender, "command.invalid-effect"); return; }
                    finalValue = valueStr.toUpperCase();
                }
            }
            else if (setting.contains("volume") || setting.contains("pitch")) {
                finalValue = Double.parseDouble(valueStr);
            }
            else if (setting.equals("creeper-power") || setting.equals("creeper-damage") || setting.equals("ghast-power")) {
                finalValue = Double.parseDouble(valueStr);
            }
            else if (SETTINGS.contains(setting)) {
                finalValue = Integer.parseInt(valueStr);
                if (setting.contains("duration") || setting.contains("cooldown")) unit = "s";
                else if (setting.contains("distance") || setting.contains("radius")) unit = " blok";
                else if (setting.contains("level")) unit = " lvl";
            } else {
                sendMsgAny(sender, "command.edit-unknown-setting", "%setting%", setting);
                return;
            }
        } catch (NumberFormatException e) {
            sendMsgAny(sender, "command.edit-invalid-number", "%setting%", setting);
            return;
        }

        plugin.getSwordManager().updateSwordSetting(data.getKey(), setting, finalValue);
        String displayValue = (finalValue instanceof List) ? finalValue.toString() : valueStr + unit;
        sendMsgAny(sender, "command.edit-success", "%sword%", name, "%setting%", setting, "%value%", displayValue);
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) { sendMsgAny(sender, "command.usage-give"); return; }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) { sendMsgAny(sender, "command.player-not-found"); return; }

        SwordData data = findSwordData(args[2]);
        if (data == null) { sendMsgAny(sender, "command.sword-not-found", "%sword%", args[2]); return; }

        ItemStack swordItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = swordItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data.getCustomModelData());
            meta.setDisplayName(getMsgRaw("item.name", "%sword%", capitalize(data.getKey())));
            meta.setLore(Arrays.asList(
                    getMsgRaw("item.ability", "%ability%", String.join(", ", data.getEffectTypes())),
                    getMsgRaw("item.cooldown", "%seconds%", String.valueOf(data.getCooldown()))
            ));
            swordItem.setItemMeta(meta);
        }

        HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(swordItem);

        if (!leftover.isEmpty()) {
            target.getWorld().dropItem(target.getLocation(), swordItem);
            if (plugin.getConfig().getString("language", "en").equalsIgnoreCase("tr")) {
                target.sendMessage(ChatColor.YELLOW + "Envanterin dolu olduğu için kılıç yere düştü!");
            } else {
                target.sendMessage(ChatColor.YELLOW + "Inventory full! Sword dropped on the ground.");
            }
        }

        sendMsgAny(sender, "command.give-success", "%sword%", capitalize(args[2]), "%player%", target.getName());
    }

    private void handleBind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sendMsgAny(sender, "command.only-player"); return; }
        Player player = (Player) sender;
        if (args.length < 2) { sendMsgAny(sender, "command.usage-bind"); return; }
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) { sendMsgAny(sender, "command.no-item"); return; }
        SwordData data = findSwordData(args[1]);
        if (data == null) { sendMsgAny(sender, "command.sword-not-found", "%sword%", args[1]); return; }
        ItemMeta meta = handItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data.getCustomModelData());
            handItem.setItemMeta(meta);
            sendMsgAny(sender, "command.bind-success", "%sword%", capitalize(args[1]));
        }
    }

    private void sendHelp(CommandSender sender) {
        sendMsgAny(sender, "command.help-header");
        sendMsgAny(sender, "command.help-give");
        sendMsgAny(sender, "command.help-bind");
        sendMsgAny(sender, "command.help-create");
        sendMsgAny(sender, "command.help-edit");
        sendMsgAny(sender, "command.help-reload");
        sendMsgAny(sender, "command.help-list");
        sender.sendMessage(ChatColor.GOLD + "/avsword info <name> " + ChatColor.GRAY + "- Show sword details.");
    }
}