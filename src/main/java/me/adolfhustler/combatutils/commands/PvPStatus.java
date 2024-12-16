package me.adolfhustler.combatutils.commands;

import me.adolfhustler.combatutils.CombatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvPStatus implements CommandExecutor {

    private final CombatUtils plugin;

    public PvPStatus(CombatUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Only players can check their own PvP status.");
            return true;
        }

        if (args.length > 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /pvpstatus [player name]");
            return true;
        }

        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
        }

        boolean pvpStatus = plugin.getPvPStatus(target);

        String message = ChatColor.YELLOW + (target.equals(sender) ? "Your" : target.getName() + "'s") +
                " PvP status is " +
                (pvpStatus ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED") + ChatColor.YELLOW + ".";
        sender.sendMessage(message);

        return true;
    }
}
