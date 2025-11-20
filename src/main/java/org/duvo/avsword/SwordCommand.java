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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SwordCommand implements CommandExecutor, TabCompleter {

    private final AvSword plugin;
    private final List<String> SPECIAL_EFFECTS = Arrays.asList("ENDERMAN", "DRAGON", "SPIDER", "PHANTOM", "NONE");
    private final List<String> SETTINGS = Arrays.asList(
            "cooldown", "custom-model-data", "effect", "radius",
            "teleport-distance", "effect-level", "effect-duration",
            "enabled", "target-enemy-only", "breath-duration", "sound.volume", "sound.pitch"
    );

    public SwordCommand(AvSword plugin) {
        this.plugin = plugin;
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
            default: sendHelp(sender); break;
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("avsword.admin")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        String lang = plugin.getConfig().getString("language", "en");

        if (args.length == 1) {
            if (lang.equalsIgnoreCase("tr")) {
                suggestions.addAll(Arrays.asList("ver", "bagla", "yenile", "liste", "olustur", "duzenle"));
            } else {
                suggestions.addAll(Arrays.asList("give", "bind", "reload", "list", "create", "edit"));
            }
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
            Collections.sort(completions);
            return completions;
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (Arrays.asList("give", "ver").contains(sub)) return null;
            if (Arrays.asList("bind", "bagla", "edit", "duzenle").contains(sub)) {
                if (plugin.getConfig().getConfigurationSection("swords") != null) {
                    suggestions.addAll(plugin.getConfig().getConfigurationSection("swords").getKeys(false));
                }
            }
            if (Arrays.asList("create", "olustur").contains(sub)) suggestions.add("<isim>");

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
            if (Arrays.asList("edit", "duzenle").contains(sub)) suggestions.addAll(SETTINGS);
            if (Arrays.asList("create", "olustur").contains(sub)) suggestions.add("<model_id>");

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
                } else if (setting.equals("effect")) {
                    suggestions.addAll(SPECIAL_EFFECTS);
                    for (PotionEffectType type : PotionEffectType.values()) suggestions.add(type.getName());
                } else {
                    suggestions.addAll(Arrays.asList("<değer>", "10", "5"));
                }
            }

            StringUtil.copyPartialMatches(args[3], suggestions, completions);
            Collections.sort(completions);
            return completions;
        }

        return Collections.emptyList();
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
        plugin.getSwordManager().createSword(name, modelData, effect);
        sendMsgAny(sender, "command.create-success", "%sword%", name, "%model%", String.valueOf(modelData));
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
                        if(isValidEffect(s.toUpperCase())) newEffects.add(s.toUpperCase());
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
        target.getInventory().addItem(swordItem);
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
            meta.setDisplayName(getMsgRaw("item.name", "%sword%", capitalize(data.getKey())));
            meta.setLore(Arrays.asList(
                    getMsgRaw("item.ability", "%ability%", String.join(", ", data.getEffectTypes())),
                    getMsgRaw("item.cooldown", "%seconds%", String.valueOf(data.getCooldown()))
            ));
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
    }
}