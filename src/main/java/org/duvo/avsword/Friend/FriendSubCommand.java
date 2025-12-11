package org.duvo.avsword.Friend;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.duvo.avsword.AvSword;

import java.util.Set;
import java.util.UUID;

public class FriendSubCommand {

    private final AvSword plugin;

    public FriendSubCommand(AvSword plugin) {
        this.plugin = plugin;
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMsg(sender, "command.only-player");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("avsword.friend")) {
            sendMsg(player, "command.no-permission");
            return;
        }

        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add":
            case "ekle":
                if (args.length < 3) {
                    sendMsg(player, "friend.usage-add");
                    return;
                }
                modifyFriend(player, args[2], true);
                break;

            case "remove":
            case "cikar":
            case "çıkar":
            case "sil":
                if (args.length < 3) {
                    sendMsg(player, "friend.usage-remove");
                    return;
                }
                modifyFriend(player, args[2], false);
                break;

            case "list":
            case "liste":
                showList(player);
                break;

            default:
                sendHelp(player);
                break;
        }
    }

    private void modifyFriend(Player owner, String targetName, boolean add) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            Player target = Bukkit.getPlayer(targetName);
            UUID targetId;
            String realName;
            boolean found = false;

            if (target != null) {
                targetId = target.getUniqueId();
                realName = target.getName();
                found = true;
            } else {
                OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(targetName);
                if (offPlayer.hasPlayedBefore()) {
                    targetId = offPlayer.getUniqueId();
                    realName = offPlayer.getName();
                    found = true;
                } else {
                    targetId = null;
                    realName = null;
                }
            }
            final UUID finalTargetId = targetId;
            final String finalRealName = realName;
            final boolean finalFound = found;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!finalFound) {
                    sendMsg(owner, "command.player-not-found");
                    return;
                }

                if (owner.getUniqueId().equals(finalTargetId)) {
                    sendMsg(owner, "friend.self-error");
                    return;
                }

                if (add) {
                    int limit = plugin.getConfig().getInt("friend-limit", 30);
                    int currentFriends = plugin.getFriendManager().getFriends(owner.getUniqueId()).size();

                    if (currentFriends >= limit && !owner.hasPermission("avsword.friend.bypasslimit")) {
                        sendMsg(owner, "friend.limit-reached", "%limit%", String.valueOf(limit));
                        return;
                    }

                    if (plugin.getFriendManager().addFriend(owner.getUniqueId(), finalTargetId)) {
                        sendMsg(owner, "friend.added", "%player%", finalRealName);
                    } else {
                        sendMsg(owner, "friend.already-added", "%player%", finalRealName);
                    }
                } else {
                    if (plugin.getFriendManager().removeFriend(owner.getUniqueId(), finalTargetId)) {
                        sendMsg(owner, "friend.removed", "%player%", finalRealName);
                    } else {
                        sendMsg(owner, "friend.not-in-list", "%player%", finalRealName);
                    }
                }
            });
        });
    }

    private void showList(Player player) {
        Set<UUID> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            sendMsg(player, "friend.list-empty");
            return;
        }

        int limit = plugin.getConfig().getInt("friend-limit", 30);
        int current = friends.size();

        sendMsg(player, "friend.list-header", "%current%", String.valueOf(current), "%max%", String.valueOf(limit));

        for (UUID id : friends) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(id);
            String name = (p.getName() != null) ? p.getName() : "Unknown";
            player.sendMessage(ChatColor.YELLOW + "- " + name);
        }
    }

    private void sendHelp(Player player) {
        sendMsg(player, "friend.help-header");
        sendMsg(player, "friend.help-add");
        sendMsg(player, "friend.help-remove");
        sendMsg(player, "friend.help-list");
    }

    private void sendMsg(CommandSender sender, String key, String... replacements) {
        if (sender instanceof Player) {
            plugin.getLanguageManager().sendMessage((Player) sender, key, replacements);
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage(key));
        }
    }
}