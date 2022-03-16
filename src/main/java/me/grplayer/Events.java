package me.grplayer;

import me.grplayer.lib.corpses.CorpseManager;
import me.grplayer.lib.discord.DiscordWebhook;
import me.grplayer.lib.naj0jerk.BrewingRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(event.getJoinMessage() == null) return;

        ShatteredEmpires.getInstance().getWebhook().sendAlert(event.getJoinMessage().replace("§e", ""));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if(event.getQuitMessage() == null) return;

        ShatteredEmpires.getInstance().getWebhook().sendAlert(event.getQuitMessage().replace("§e", ""));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(event.getDeathMessage() == null) return;

        ShatteredEmpires.getInstance().getWebhook().sendAlert(event.getDeathMessage().replace("§e", ""));
    }

    @EventHandler
    public void onDeathCorpse(PlayerDeathEvent event) {
        // Let's spawn a Corpse.
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();
        List<ItemStack> drops = event.getDrops();

        CorpseManager corpseManager = ShatteredEmpires.getInstance().getCorpseManager();
        corpseManager.spawnCorpse(player.getUniqueId(), deathLocation, event.getDeathMessage(), drops);

        event.getDrops().clear();
    }

    @EventHandler
    public void onBlockbreak(BlockBreakEvent event) {
        if(event.getBlock().getType() == Material.CHEST) {
            // Let's attempt to remove a corpse.
            CorpseManager corpseManager = ShatteredEmpires.getInstance().getCorpseManager();
            corpseManager.removeCorpse(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if(inventory.getType() != InventoryType.CHEST) return;

        if(inventory.getHolder() instanceof Chest) {
            Chest chest = (Chest) inventory.getHolder();
            if(chest.getBlock().getType() == Material.CHEST) {
                // Let's attempt to remove a corpse.
                CorpseManager corpseManager = ShatteredEmpires.getInstance().getCorpseManager();
                corpseManager.removeCorpse(chest.getLocation().getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onAdvancementGained(PlayerAdvancementDoneEvent event) {
        if(event.getAdvancement().getKey().getKey().startsWith("recipes/")) return;

        String advancementName = ShatteredEmpires.getInstance().getTranslationManager().getTranslation("advancements." + event.getAdvancement().getKey().getKey().replace("/", ".") + ".title");

        ShatteredEmpires.getInstance().getWebhook().sendAlert(event.getPlayer().getName() + " has made the advancement [" + advancementName + "]");
    }

}
