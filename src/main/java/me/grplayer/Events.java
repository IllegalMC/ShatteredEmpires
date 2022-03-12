package me.grplayer;

import me.grplayer.lib.discord.DiscordWebhook;
import me.grplayer.lib.naj0jerk.BrewingRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

public class Events implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void potionItemPlacer(final InventoryClickEvent e) {
        if (e.getClickedInventory() == null)
            return;
        if (e.getClickedInventory().getType() != InventoryType.BREWING)
            return;
        if (!(e.getClick() == ClickType.LEFT)) //Make sure we are placing an item
            return;
        final ItemStack is = e.getCurrentItem(); //We want to get the item in the slot
        final ItemStack is2 = e.getCursor().clone(); //And the item in the cursor
        if(is2 == null) //We make sure we got something in the cursor
            return;
        if(is2.getType() == Material.AIR)
            return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(ShatteredEmpires.getInstance(), new Runnable() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                e.setCursor(is);//Now we make the switch
                e.getClickedInventory().setItem(e.getSlot(), is2);
            }
        }, 1L);//(Delay in 1 tick)
        ((Player)e.getView().getPlayer()).updateInventory();//And we update the inventory
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void PotionListener(InventoryClickEvent e) {
        if(e.getClickedInventory() == null)
            return;
        if(e.getClickedInventory().getType() != InventoryType.BREWING)
            return;
        if(((BrewerInventory)e.getInventory()).getIngredient() == null)
            return;
        BrewingRecipe recipe = BrewingRecipe.getRecipe((BrewerInventory) e.getClickedInventory());
        if(recipe == null)
            return;
        recipe.startBrewing((BrewerInventory) e.getClickedInventory(), e.getViewers());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if(event.getMessage().startsWith("/") && event.getMessage().length() > 1) return;
        if(!event.getRecipients().containsAll(Bukkit.getServer().getOnlinePlayers())) return;

        String message = event.getMessage();

        ShatteredEmpires shatteredEmpires = ShatteredEmpires.getInstance();
        if(shatteredEmpires.getWebhook() == null) return;

        DiscordWebhook webhook = shatteredEmpires.getWebhook();
        // Let's not delay the chat message.
        Bukkit.getScheduler().runTaskAsynchronously(shatteredEmpires, () -> webhook.sendMessage(event.getPlayer(), message));
    }

}