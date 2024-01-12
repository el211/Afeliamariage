package com.elias.afeliamarriage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AfeliaMarriage extends JavaPlugin implements Listener {

    private static final PotionEffect RESISTANCE = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 1);
    private static final PotionEffect STRENGTH_II = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 2);

    private final Map<UUID, UUID> marriages = new HashMap<>();
    private final Map<UUID, Location> marriageHomes = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        config = getConfig();
        loadMarriages();

        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Save marriages periodically to handle player disconnects
        new BukkitRunnable() {
            @Override
            public void run() {
                saveMarriages();
            }
        }.runTaskTimer(this, 1200L, 1200L); // Save every minute (20 ticks * 60 seconds)

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Map.Entry<UUID, UUID> entry : marriages.entrySet()) {
                UUID player1Id = entry.getKey();
                UUID player2Id = entry.getValue();

                Player player1 = Bukkit.getPlayer(player1Id);
                Player player2 = Bukkit.getPlayer(player2Id);

                applyMarriageEffects(player1, player2);
            }
        }, 100L, 100L); // Check every 5 seconds (20 ticks * 5 seconds)

        getCommand("marriagegui").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                openMainGUI((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });

        getCommand("divorce").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                handleDivorce((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });

        getCommand("setmariagehome").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                setMarriageHome((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });

        getCommand("mariagehome").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                teleportToMarriageHome((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });
    }

    private void openMainGUI(Player player) {
        Inventory mainGUI = Bukkit.createInventory(null, 45, ChatColor.BOLD + "Marriage Status");

        ItemStack proposeItem = new ItemStack(Material.DIAMOND);
        ItemMeta proposeItemMeta = proposeItem.getItemMeta();
        proposeItemMeta.setDisplayName(ChatColor.AQUA + "Choisir un Mari/Une Femme");
        proposeItem.setItemMeta(proposeItemMeta);
        mainGUI.setItem(21, proposeItem);

        player.openInventory(mainGUI);
    }

    private void openPlayerSelectionGUI(Player proposer) {
        Inventory playerSelectionGUI = Bukkit.createInventory(null, 45, ChatColor.BOLD + "Sélectionner un Joueur");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(proposer)) {
                ItemStack playerHeadItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) playerHeadItem.getItemMeta();
                skullMeta.setOwningPlayer(onlinePlayer);
                playerHeadItem.setItemMeta(skullMeta);

                playerSelectionGUI.addItem(playerHeadItem);
            }
        }

        proposer.openInventory(playerSelectionGUI);
    }

    private void proposeMarriage(Player proposer, Player selectedPlayer) {
        proposer.sendMessage(ChatColor.GREEN + "Vous avez proposé le mariage à " + selectedPlayer.getName() + " !");
        selectedPlayer.sendMessage(ChatColor.LIGHT_PURPLE + proposer.getName() + " vous a proposé le mariage !");
        marriages.put(proposer.getUniqueId(), selectedPlayer.getUniqueId());
        marriages.put(selectedPlayer.getUniqueId(), proposer.getUniqueId());

        // Set common home for the couple
        Location marriageHome = proposer.getLocation();
        marriageHomes.put(proposer.getUniqueId(), marriageHome);
        marriageHomes.put(selectedPlayer.getUniqueId(), marriageHome);

        // Add marriage effects (e.g., resistance and strength II) if needed
        applyMarriageEffects(proposer, selectedPlayer);

        // Save marriages after proposing
        saveMarriages();
    }

    private void applyMarriageEffects(Player player1, Player player2) {
        if (player1 == null || player2 == null) return;
        if (!player1.isOnline() || !player2.isOnline()) return;

        double distance = player1.getLocation().distance(player2.getLocation());
        if (distance > 4) return;

        boolean haveEffect =
                hasEffects(player1, RESISTANCE.getType(), STRENGTH_II.getType()) &&
                hasEffects(player2, RESISTANCE.getType(), STRENGTH_II.getType());

        if (!haveEffect) {
            player1.sendMessage(ChatColor.YELLOW + "Vous ressentez la présence de votre partenaire.");
            player2.sendMessage(ChatColor.YELLOW + "Vous ressentez la présence de votre partenaire.");
        }

        applyEffects(player1, RESISTANCE, STRENGTH_II);
        applyEffects(player2, RESISTANCE, STRENGTH_II);
    }

    private boolean hasEffects(Player player, PotionEffectType... effects) {
        List<PotionEffectType> list = List.of(effects);
        return list.stream().allMatch(player::hasPotionEffect);
    }

    private void applyEffects(Player player, PotionEffect... effects) {
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }

    private void handleDivorce(Player player) {
        UUID partnerId = marriages.get(player.getUniqueId());
        if (partnerId != null) {
            Player partner = Bukkit.getPlayer(partnerId);
            marriages.remove(player.getUniqueId());
            marriages.remove(partnerId);
            marriageHomes.remove(player.getUniqueId());
            marriageHomes.remove(partnerId);
            player.sendMessage(ChatColor.RED + "Vous avez divorcé avec " + partner.getName() + ".");
            partner.sendMessage(ChatColor.RED + "Vous avez divorcé avec " + player.getName() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Vous n'êtes pas marié.");
        }

        // Save marriages after divorcing
        saveMarriages();
    }

    private void setMarriageHome(Player player) {
        UUID playerId = player.getUniqueId();
        if (marriages.containsKey(playerId)) {
            UUID partnerId = marriages.get(playerId);
            Location playerLocation = player.getLocation();
            marriageHomes.put(playerId, playerLocation);
            marriageHomes.put(partnerId, playerLocation);
            player.sendMessage(ChatColor.GREEN + "Vous avez défini votre maison de mariage.");
        } else {
            player.sendMessage(ChatColor.RED + "Vous devez être marié(e) pour définir une maison de mariage.");
        }
    }

    private void teleportToMarriageHome(Player player) {
        UUID playerId = player.getUniqueId();
        if (marriageHomes.containsKey(playerId)) {
            Location marriageHome = marriageHomes.get(playerId);
            player.teleport(marriageHome);
            player.sendMessage(ChatColor.GREEN + "Téléportation à votre maison de mariage.");
        } else {
            player.sendMessage(ChatColor.RED + "Vous devez définir une maison de mariage avant de pouvoir vous y téléporter.");
        }
    }

    private void loadMarriages() {
        if (config.contains("marriages")) {
            marriages.clear();
            for (String key : config.getConfigurationSection("marriages").getKeys(false)) {
                UUID playerId = UUID.fromString(key);
                UUID partnerId = UUID.fromString(config.getString("marriages." + key));
                marriages.put(playerId, partnerId);
            }
        }

        if (config.contains("marriageHomes")) {
            marriageHomes.clear();
            for (String key : config.getConfigurationSection("marriageHomes").getKeys(false)) {
                UUID playerId = UUID.fromString(key);
                Location location = (Location) config.get("marriageHomes." + key);
                marriageHomes.put(playerId, location);
            }
        }
    }

    private void saveMarriages() {
        config.set("marriages", null); // Clear previous data
        for (UUID playerId : marriages.keySet()) {
            UUID partnerId = marriages.get(playerId);
            config.set("marriages." + playerId, partnerId.toString());
        }

        config.set("marriageHomes", null); // Clear previous data
        for (UUID playerId : marriageHomes.keySet()) {
            Location location = marriageHomes.get(playerId);
            config.set("marriageHomes." + playerId, location);
        }

        saveConfig();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || !clickedInventory.equals(player.getOpenInventory().getTopInventory())) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType() == Material.DIAMOND) {
            openPlayerSelectionGUI(player);
        } else if (clickedItem.getType() == Material.PLAYER_HEAD) {
            Player selectedPlayer = Bukkit.getPlayer(((SkullMeta) clickedItem.getItemMeta()).getOwningPlayer().getName());
            if (selectedPlayer != null && selectedPlayer.isOnline()) {
                proposeMarriage(player, selectedPlayer);
                player.closeInventory();
            } else {
                player.sendMessage(ChatColor.RED + "Joueur introuvable ou non connecté.");
            }
        }
    }
}
