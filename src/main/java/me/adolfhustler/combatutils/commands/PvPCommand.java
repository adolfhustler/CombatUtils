package me.adolfhustler.combatutils.commands;

import me.adolfhustler.combatutils.CombatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvPCommand implements CommandExecutor {

    private final CombatUtils plugin;

    public PvPCommand(CombatUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean currentStatus = plugin.getPvPStatus(player);
            boolean newStatus = !currentStatus;

            plugin.setPvPStatus(player, newStatus);

            String message;
            if (newStatus) {
                message = plugin.getConfig().getString("messages.pvp-enabled-self", "&aPvP has been enabled for you!");
            } else {
                message = plugin.getConfig().getString("messages.pvp-disabled-self", "&cPvP has been disabled for you!");
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Only players can use this command.");
        return false;
    }
}
