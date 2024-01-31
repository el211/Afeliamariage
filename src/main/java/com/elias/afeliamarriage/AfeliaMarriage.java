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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AfeliaMarriage extends org.bukkit.plugin.java.JavaPlugin implements Listener {

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

        new BukkitRunnable() {
            @Override
            public void run() {
                saveMarriages();
            }
        }.runTaskTimer(this, 1200L, 1200L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Map.Entry<UUID, UUID> entry : marriages.entrySet()) {
                UUID player1Id = entry.getKey();
                UUID player2Id = entry.getValue();

                Player player1 = Bukkit.getPlayer(player1Id);
                Player player2 = Bukkit.getPlayer(player2Id);

                applyMarriageEffects(player1, player2);
            }
        }, 100L, 100L);

        Objects.requireNonNull(getCommand("marriagegui")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                openMainGUI((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });

        Objects.requireNonNull(getCommand("marriagegui")).setTabCompleter((sender, command, alias, args) -> Collections.emptyList());

        Objects.requireNonNull(getCommand("divorce")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                handleDivorce((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });

        Objects.requireNonNull(getCommand("setmariagehome")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                setMarriageHome((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });

        Objects.requireNonNull(getCommand("mariagehome")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                teleportToMarriageHome((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            }
            return true;
        });
    }

    private void openMainGUI(Player player) {
        Inventory mainGUI = Bukkit.createInventory(new MarriageInventoryHolder(this), 45, ChatColor.BOLD + "Marriage Status");

        ItemStack proposeItem = new ItemStack(Material.CLAY_BALL);
        ItemMeta proposeItemMeta = proposeItem.getItemMeta();
        Objects.requireNonNull(proposeItemMeta).setDisplayName(ChatColor.AQUA + "Choisir un Partenaire!");
        proposeItemMeta.setCustomModelData(6);
        proposeItem.setItemMeta(proposeItemMeta);
        mainGUI.setItem(21, proposeItem);

        player.openInventory(mainGUI);
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
            player.sendMessage(ChatColor.RED + "Vous avez divorcé avec " + Objects.requireNonNull(partner).getName() + ".");
            partner.sendMessage(ChatColor.RED + "Vous avez divorcé avec " + player.getName() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Vous n'êtes pas marié.");
        }

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
            for (String key : Objects.requireNonNull(config.getConfigurationSection("marriages")).getKeys(false)) {
                UUID playerId = UUID.fromString(key);
                UUID partnerId = UUID.fromString(Objects.requireNonNull(config.getString("marriages." + key)));
                marriages.put(playerId, partnerId);
            }
        }

        if (config.contains("marriageHomes")) {
            marriageHomes.clear();
            for (String key : Objects.requireNonNull(config.getConfigurationSection("marriageHomes")).getKeys(false)) {
                UUID playerId = UUID.fromString(key);
                Location location = (Location) config.get("marriageHomes." + key);
                marriageHomes.put(playerId, location);
            }
        }
    }

    public void saveMarriages() {
        config.set("marriages", null);
        for (UUID playerId : marriages.keySet()) {
            UUID partnerId = marriages.get(playerId);
            config.set("marriages." + playerId, partnerId.toString());
        }

        config.set("marriageHomes", null);
        for (UUID playerId : marriageHomes.keySet()) {
            Location location = marriageHomes.get(playerId);
            config.set("marriageHomes." + playerId, location);
        }

        saveConfig();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (!(event.getInventory().getHolder() instanceof MarriageInventoryHolder)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || !clickedInventory.equals(player.getOpenInventory().getTopInventory())) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        MarriageInventoryHolder marriageHolder = (MarriageInventoryHolder) event.getInventory().getHolder();
        marriageHolder.handleMarriageInventoryClick(player, clickedItem);
    }

    // New method to provide access to the 'marriages' map
    public Map<UUID, UUID> getMarriages() {
        return marriages;
    }
}
