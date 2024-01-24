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
import org.bukkit.plugin.java.JavaPlugin;

public class MarriageInventoryHolder implements org.bukkit.inventory.InventoryHolder {

    private org.bukkit.Bukkit Bukkit;

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }

    public void handleMarriageInventoryClick(Player player, ItemStack clickedItem) {
        if (clickedItem.getType() == Material.CLAY_BALL) {
            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (itemMeta != null && itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == 6) {
                openPlayerSelectionGUI(player);
            }
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
        proposer.sendMessage(ChatColor.GREEN + "Vous avez proposé le mariage à " + selectedPlayer.getName() + " !");
        selectedPlayer.sendMessage(ChatColor.LIGHT_PURPLE + proposer.getName() + " vous a proposé le mariage !");
        // Add your marriage logic here
    }
}
