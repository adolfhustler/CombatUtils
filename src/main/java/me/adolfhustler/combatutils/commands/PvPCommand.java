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
        if (!(sender instanceof Player)) {
            String consoleMessage = "&cOnly players can use this command.";
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', consoleMessage));
            return false;
        }

        Player player = (Player) sender;

        if (plugin.isInCombat(player)) {
            String combatMessage = "&cYou cannot toggle PvP while in combat!";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', combatMessage));
            return false;
        }

        boolean currentStatus = plugin.getPvPStatus(player);
        boolean newStatus = !currentStatus;
        plugin.setPvPStatus(player, newStatus);

        String messageKey = newStatus ? "messages.pvp-enabled-self" : "messages.pvp-disabled-self";
        String message = plugin.getConfig().getString(messageKey, "&aPvP status has been updated.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        return true;
    }
}
