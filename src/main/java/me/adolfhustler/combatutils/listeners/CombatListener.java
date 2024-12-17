package me.adolfhustler.combatutils.listeners;

import me.adolfhustler.combatutils.CombatUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class CombatListener implements Listener {

    private final CombatUtils plugin;
    public final HashMap<UUID, BukkitRunnable> combatTasks;

    public CombatListener(CombatUtils plugin) {
        this.plugin = plugin;
        this.combatTasks = new HashMap<>();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            if(plugin.isInGracePeriod(victim)){
                event.setCancelled(true);
                String graceAttackMessage = plugin.getConfig().getString("messages.grace-period-attack", "&cThat player is in a grace period and cannot be attacked!");
                attacker.sendMessage(ChatColor.translateAlternateColorCodes('&', graceAttackMessage));
                return;
            }
            if(plugin.isInGracePeriod(attacker)){
                event.setCancelled(true);
                String graceAttackMessageSelf = plugin.getConfig().getString("messages.grace-period-attack-self", "&cYou cannot attack players when you are in a grace period!");
                attacker.sendMessage(ChatColor.translateAlternateColorCodes('&', graceAttackMessageSelf));
                return;
            }

            if (!plugin.getPvPStatus(attacker)) {
                event.setCancelled(true);
                String customMessage = plugin.getConfig().getString("messages.pvp-disabled-attacker", "&cYou cannot attack this player because you have PvP disabled.");
                if(customMessage != null){
                    customMessage = ChatColor.translateAlternateColorCodes('&', customMessage);
                }
                attacker.sendMessage(customMessage);
                return;
            }
            if (!plugin.getPvPStatus(victim)) {
                event.setCancelled(true);
                String customMessage = plugin.getConfig().getString("messages.pvp-disabled-victim", "&cYou cannot attack this player because they have PvP disabled.");
                if(customMessage != null){
                    customMessage = ChatColor.translateAlternateColorCodes('&', customMessage);
                }
                attacker.sendMessage(customMessage);
                return;
            }

            plugin.startCombat(attacker, victim);
            startCombatTimer(attacker);
            startCombatTimer(victim);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player disconnectedPlayer = event.getPlayer();

        if (plugin.isInCombat(disconnectedPlayer)) {
            Player opponent = plugin.getCombatPartner(disconnectedPlayer);

            plugin.endCombat(disconnectedPlayer);
            if (opponent != null) {
                plugin.endCombat(opponent);
            }

            plugin.getConfig().set("dead-players." + disconnectedPlayer.getUniqueId(), true);
            plugin.saveConfig();

            disconnectedPlayer.setHealth(0);
        }
    }





    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("dead-players." + player.getUniqueId(), false)) {
            plugin.getConfig().set("dead-players." + player.getUniqueId(), null);
            plugin.saveConfig();
        }
        plugin.startGracePeriod(player);
        //startGracePeriod(player)


    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event){
        Player player = event.getPlayer();
        if(!plugin.isInCombat(player)){
            plugin.startGracePeriod(player);
            //startGraceTimer(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();

        if (plugin.getConfig().getBoolean("dead-players." + deadPlayer.getUniqueId(), false)) {
            String customMessage = plugin.getConfig().getString("custom-death-messages.disconnect-death-message", "&c{player} was killed for disconnecting during combat!").replace("{player}", deadPlayer.getName());
            event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', customMessage));
        }

        if (plugin.isInCombat(deadPlayer)) {
            Player opponent = plugin.getCombatPartner(deadPlayer);


            plugin.endCombat(deadPlayer);
            if (opponent != null) {
                plugin.endCombat(opponent);
            }

        }
    }

   /* private void startGraceTimer(Player player) {
        BukkitRunnable existingTask = combatTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        int graceSeconds = plugin.getConfig().getInt("grace-period.seconds", 10);
        long graceEnd = System.currentTimeMillis() + (graceSeconds * 1000L);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                long remaining = graceEnd - System.currentTimeMillis();
                if(plugin.isInCombat(player)){
                    cancel();
                    return;
                }
                if (remaining <= 0) {
                    cancel();
                    return;
                }

                long roundedRemaining = (long) Math.ceil(remaining / 1000.0);
                String actionBarMessage = plugin.getConfig().getString("messages.grace-timer-message", "&aGrace Timer: {time}s");
                actionBarMessage = actionBarMessage.replace("{time}", String.valueOf(roundedRemaining));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', actionBarMessage)));
            }
        };

        task.runTaskTimer(plugin, 0L, 20L);
        combatTasks.put(player.getUniqueId(), task);
    }

     */


    private void startCombatTimer(Player player) {
        BukkitRunnable existingTask = combatTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        long combatTimeout = plugin.getConfig().getInt("general.combat-timeout", 15) * 1000L;
        long endTime = System.currentTimeMillis() + combatTimeout;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                long remaining = endTime - System.currentTimeMillis();

                long roundedRemaining = (long) Math.ceil(remaining / 1000.0);

                if (roundedRemaining <= 0) {
                    plugin.endCombat(player);


                    String combatEndMessage = plugin.getConfig().getString("messages.combat-timer-ended", "&aYou are no longer in combat.");

                    if (combatEndMessage != null) {
                        combatEndMessage = ChatColor.translateAlternateColorCodes('&', combatEndMessage);

                        player.sendMessage(combatEndMessage);
                    } else {
                        plugin.getLogger().warning("Failed to load 'combat-timer-ended' message from config.");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYou are no longer in combat."));
                    }

                    cancel();
                } else {
                    String actionBarMessage = plugin.getConfig().getString("messages.combat-timer-message", "&cCombat Timer: {time}s");

                    actionBarMessage = actionBarMessage.replace("{time}", String.valueOf(roundedRemaining));

                    actionBarMessage = ChatColor.translateAlternateColorCodes('&', actionBarMessage);

                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMessage));
                }
            }
        };

        task.runTaskTimer(plugin, 0, 20);
        combatTasks.put(player.getUniqueId(), task);
    }



}

