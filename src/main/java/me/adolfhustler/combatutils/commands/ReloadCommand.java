package me.adolfhustler.combatutils.commands;

import me.adolfhustler.combatutils.CombatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final CombatUtils plugin;

    public ReloadCommand(CombatUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("combatutils.reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "CombatUtils configuration reloaded!");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        return true;
    }
}
