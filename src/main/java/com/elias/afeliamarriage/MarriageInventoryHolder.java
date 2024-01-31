package com.elias.afeliamarriage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.UUID;
import java.util.Map;



public class MarriageInventoryHolder implements org.bukkit.inventory.InventoryHolder {

    private final AfeliaMarriage plugin;

    public MarriageInventoryHolder(AfeliaMarriage plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }

    public void handleMarriageInventoryClick(Player player, ItemStack clickedItem) {
        System.out.println("Handling marriage inventory click for player: " + player.getName());

        if (clickedItem.getType() == Material.CLAY_BALL) {
            System.out.println("Player clicked on propose item");

            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (itemMeta != null && itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == 6) {
                System.out.println("Custom model data matches. Opening player selection GUI.");
                openPlayerSelectionGUI(player);
            }
        } else if (clickedItem.getType() == Material.PLAYER_HEAD) {
            System.out.println("Player clicked on player head item");

            Player selectedPlayer = Bukkit.getPlayer(((SkullMeta) clickedItem.getItemMeta()).getOwningPlayer().getName());
            if (selectedPlayer != null && selectedPlayer.isOnline()) {
                System.out.println("Selected player: " + selectedPlayer.getName());
                proposeMarriage(player, selectedPlayer);
                player.closeInventory();
            } else {
                System.out.println("Selected player not found or not online.");
                player.sendMessage(ChatColor.RED + "Joueur introuvable ou non connecté.");
            }
        }
    }

    private void openPlayerSelectionGUI(Player proposer) {
        Inventory playerSelectionGUI = Bukkit.createInventory(this, 45, ChatColor.BOLD + "Sélectionner un Joueur");

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
        Map<UUID, UUID> marriages = plugin.getMarriages();

        proposer.sendMessage(ChatColor.GREEN + "Vous avez proposé le mariage à " + selectedPlayer.getName() + " !");
        selectedPlayer.sendMessage(ChatColor.LIGHT_PURPLE + proposer.getName() + " vous a proposé le mariage !");

        marriages.put(proposer.getUniqueId(), selectedPlayer.getUniqueId());
        marriages.put(selectedPlayer.getUniqueId(), proposer.getUniqueId());

        // Optional: You may want to trigger additional events or actions related to marriage here.
    }
}
