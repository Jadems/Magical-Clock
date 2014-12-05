package com.jade.magicalclock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * A simple Magical Clock plugin
 * Created by Jadems on 12/4/2014.
 */
public class MagicalClockPlugin extends JavaPlugin implements Listener {
	private HashSet<String> enabledPlayers;
	private HashMap<String, Long> toggleDelay;

	private boolean opsAlwaysVisible;
	private String alwaysVisibilePermission;

	private boolean giveClockOnLogin;
	private boolean giveClockOnRespawn;
	private boolean preventDroppingClock;
	private boolean allowNormalClock;
	private long toggleDelayMillis;
	private String antiSpamMessage;

	private String clockName;
	private List<String> clockLore; //Each element represents a line of lore on the clock

	private ItemStack clockItem;

	private static MagicalClockPlugin instance;

	@Override
	public void onEnable() {
		super.onEnable();
		saveDefaultConfig();

		FileConfiguration config = getConfig();
		this.opsAlwaysVisible = config.getBoolean("AlwaysShowOps", true);
		this.alwaysVisibilePermission = "magicalclock.alwaysvisible";

		this.giveClockOnLogin = config.getBoolean("GiveClockOnLogin", true);
		this.giveClockOnRespawn = config.getBoolean("GiveClockOnRespawn", true);
		this.preventDroppingClock = config.getBoolean("PreventDroppingClock", true);
		this.allowNormalClock = config.getBoolean("AllowNormalClockUse", false);
		this.toggleDelayMillis = config.getLong("ToggleDelayMillis", 1000);
		this.antiSpamMessage = ChatColor.RED + "You are doing that too quickly!";

		clockName = ChatColor.AQUA + "Magical Clock";

		clockLore = new ArrayList(1);
		clockLore.add("Right click to toggle visibility of other players!");

		clockItem = new ItemStack(Material.WATCH);
		ItemMeta clockItemMeta = clockItem.getItemMeta();
		clockItemMeta.setDisplayName(clockName);
		clockItemMeta.setLore(clockLore);
		clockItem.setItemMeta(clockItemMeta);

		enabledPlayers = new HashSet();
		toggleDelay = new HashMap();

		Bukkit.getPluginManager().registerEvents(this, this);
		MagicalClockPlugin.instance = this;
	}

	private boolean isPlayerSpamming(Player player) {
		Long delayExpireTime = toggleDelay.get(player.getName());

		if(delayExpireTime == null)
			delayExpireTime = 0L;

		if(System.currentTimeMillis() > delayExpireTime) {
			return false;
		}

		return true;
	}

	private void addToggleDelay(Player player, long toggleDelayMillis) {
		toggleDelay.put(player.getName(), System.currentTimeMillis() + toggleDelayMillis);
	}

	public void toggleClockForPlayer(Player player) {
		String playerName = player.getName();

		if(isPlayerSpamming(player)) {
			player.sendMessage(antiSpamMessage);
			return;
		}

		if(enabledPlayers.contains(playerName)) {
			enabledPlayers.remove(playerName);
			player.sendMessage(ChatColor.GREEN + "Player visibility is now toggled on.");
			for(Player p : Bukkit.getOnlinePlayers()) {
				player.showPlayer(p);
			}
		} else {
			enabledPlayers.add(playerName);
			player.sendMessage(ChatColor.GREEN + "Player visibility is now toggled off.");
			for(Player p : Bukkit.getOnlinePlayers()) {
				if(!p.equals(player)) {
					if(!shouldAlwaysShowPlayer(p)) {
						player.hidePlayer(p);
					}
				}
			}
		}

		addToggleDelay(player, toggleDelayMillis);
	}

	private boolean itemNameContains(ItemStack i, String string) {
		if(i == null)
			return false;
		return(i.getItemMeta().hasDisplayName() && i.getItemMeta().getDisplayName().contains(string));
	}

	private boolean playerHasMagicalClock(Player player) {
		for (ItemStack i : player.getInventory()) {
			if (allowNormalClock)
				if (i.getType() == Material.WATCH)
					return true;
			if (isMagicalClock(i))
				return true;
		}
		return false;
	}

	private boolean isMagicalClock(ItemStack i) {
		if(i == null)
			return false;

		if(i.getType() == Material.WATCH) {
			if(itemNameContains(i, clockName))
				return true;
		}
		return false;
	}

	private void givePlayerClock(Player player) {
		player.getInventory().addItem(clockItem);
	}

	private boolean shouldAlwaysShowPlayer(Player player) {
		return (opsAlwaysVisible && player.isOp()) || player.hasPermission(alwaysVisibilePermission);
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		if(giveClockOnRespawn) {
			final Player player = e.getPlayer();
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
				@Override
				public void run() {
					if(!playerHasMagicalClock(player)) {
						givePlayerClock(player);
					}
				}
			}, 2);
		}
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		if(preventDroppingClock) {
			ItemStack i = e.getItemDrop().getItemStack();
			if(i.getType() == Material.WATCH) {
				if(itemNameContains(i, clockName)) {
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		if(preventDroppingClock) {
			Iterator<ItemStack> iter = e.getDrops().iterator();
			while(iter.hasNext()) {
				ItemStack i = iter.next();
				if(isMagicalClock(i))
					iter.remove();
			}
		}
	}

	@EventHandler
	public void onPlayerLogout(PlayerQuitEvent e) { //cleanup
		String playerName = e.getPlayer().getName();

		enabledPlayers.remove(playerName);
		toggleDelay.remove(playerName);
	}

	@EventHandler
	public void onPlayerRightClickClock(PlayerInteractEvent e) {
		if(!(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK))
			return;

		ItemStack itemInHand = e.getPlayer().getItemInHand();

		if(itemInHand.getType() != Material.WATCH)
			return;

		if(allowNormalClock) {
			toggleClockForPlayer(e.getPlayer());
		} else {
			if(isMagicalClock(itemInHand)) {
				toggleClockForPlayer(e.getPlayer());
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player joiningPlayer = e.getPlayer();

		if(!shouldAlwaysShowPlayer(joiningPlayer)) {
			for(String s : enabledPlayers) {
				Player p = Bukkit.getPlayer(s);
				if(p != null)
					p.hidePlayer(e.getPlayer());
			}
		}

		if(giveClockOnLogin && !joiningPlayer.isDead()) {
			if(!playerHasMagicalClock(joiningPlayer)) {
				givePlayerClock(joiningPlayer);
			}
		}
	}

	public static MagicalClockPlugin getInstance() {
		return instance;
	}
}
