package me.adolfhustler.combatutils;

import me.adolfhustler.combatutils.commands.PvPCommand;
import me.adolfhustler.combatutils.commands.PvPStatus;
import me.adolfhustler.combatutils.commands.ReloadCommand;
import me.adolfhustler.combatutils.listeners.CombatListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CombatUtils extends JavaPlugin {

    private File pvpStatusFile;
    private FileConfiguration pvpStatusConfig;
    private static CombatUtils instance;
    private Map<Player, Player> combatPartners = new HashMap<>();
    private Map<Player, Long> combatEndTimes = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        setupPvPStatusFile();

        getCommand("pvp").setExecutor(new PvPCommand(this));
        getCommand("pvpstatus").setExecutor(new PvPStatus(this));
        getCommand("combatutilsreload").setExecutor(new ReloadCommand(this));

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getLogger().info("CombatUtils has been enabled.");
    }

    private void setupPvPStatusFile() {
        pvpStatusFile = new File(getDataFolder(), "pvpStatus.yml");

        if (!pvpStatusFile.exists()) {
            saveResource("pvpStatus.yml", false);
        }
        pvpStatusConfig = YamlConfiguration.loadConfiguration(pvpStatusFile);
    }

    public boolean getPvPStatus(Player player) {
        return pvpStatusConfig.getBoolean("pvpStatus." + player.getUniqueId(), true);
    }

    public void setPvPStatus(Player player, boolean status) {
        pvpStatusConfig.set("pvpStatus." + player.getUniqueId(), status);
        savePvPStatusConfig();
    }

    private void savePvPStatusConfig() {
        try {
            pvpStatusConfig.save(pvpStatusFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startCombat(Player player1, Player player2) {
        long combatTimeout = getConfig().getInt("general.combat-timeout", 15) * 1000L;
        long endTime = System.currentTimeMillis() + combatTimeout;

        combatEndTimes.put(player1, endTime);
        combatEndTimes.put(player2, endTime);

        combatPartners.put(player1, player2);
        combatPartners.put(player2, player1);
    }

    public void endCombat(Player player) {
        Player partner = combatPartners.get(player);

        combatEndTimes.remove(player);
        if (partner != null) {
            combatEndTimes.remove(partner);
        }

        getServer().getScheduler().cancelTasks(this);
        combatPartners.remove(player);
        if (partner != null) {
            combatPartners.remove(partner);
        }
    }


    public boolean isInCombat(Player player) {
        Long endTime = combatEndTimes.get(player);
        if (endTime != null) {
            if (System.currentTimeMillis() < endTime) {
                return true;
            } else {
                endCombat(player);
            }
        }
        return false;
    }

    public void markPlayerDeadOnDisconnect(Player player) {
        if (getConfig().getBoolean("combat-utils.punish-on-quit", true)) {
            getConfig().set("dead-players." + player.getUniqueId(), true);
            saveConfig();

            player.setHealth(0);
        }
    }

    public Player getCombatPartner(Player player) {
        return combatPartners.get(player);
    }

}
