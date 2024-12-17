package me.adolfhustler.combatutils;

import me.adolfhustler.combatutils.commands.PvPCommand;
import me.adolfhustler.combatutils.commands.PvPStatus;
import me.adolfhustler.combatutils.commands.ReloadCommand;
import me.adolfhustler.combatutils.listeners.CombatListener;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatUtils extends JavaPlugin {

    private File pvpStatusFile;
    private FileConfiguration pvpStatusConfig;
    private static CombatUtils instance;
    private Map<Player, Player> combatPartners = new HashMap<>();
    private Map<Player, Long> combatEndTimes = new HashMap<>();
    private final HashMap<UUID, Long> gracePeriodMap = new HashMap<>();

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
            combatPartners.remove(partner);
        }

        combatPartners.remove(player);
    }

    public boolean isInCombat(Player player) {
        Long endTime = combatEndTimes.get(player);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public Player getCombatPartner(Player player) {
        return combatPartners.get(player);
    }

    public void startGracePeriod(Player player) {
        if (!getConfig().getBoolean("grace-period.enabled")) {
            return;
        }

        int graceSeconds = getConfig().getInt("grace-period.seconds", 10);
        long graceEnd = System.currentTimeMillis() + (graceSeconds * 1000L);
        String graceMessage = getConfig().getString("messages.grace-period-start", "&aYou have a short grace period of PvP immunity!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', graceMessage));
        gracePeriodMap.put(player.getUniqueId(), graceEnd);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                long remaining = graceEnd - System.currentTimeMillis();

                if (remaining <= 0) {
                    endGracePeriod(player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            getConfig().getString("messages.grace-period-end", "&cYour PvP grace period has ended!")));
                    cancel(); // Stop the task
                    return;
                }

                if (isInCombat(player)) {
                    endGracePeriod(player);
                    cancel();
                    return;
                }

                String actionBarMessage = getConfig()
                        .getString("messages.grace-timer-message", "&aGrace Timer: {time}s")
                        .replace("{time}", String.valueOf((remaining + 999) / 1000));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.translateAlternateColorCodes('&', actionBarMessage)));
            }
        };

        task.runTaskTimer(this, 0L, 20L); // Runs every 1 second
    }


    public boolean isInGracePeriod(Player player) {
        long currentTime = System.currentTimeMillis();
        return gracePeriodMap.getOrDefault(player.getUniqueId(), 0L) > currentTime;
    }

    public void endGracePeriod(Player player) {
        gracePeriodMap.remove(player.getUniqueId());
    }
}
