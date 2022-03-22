package me.grplayer;

import me.grplayer.lib.discord.DiscordWebhook;
import me.grplayer.lib.naj0jerk.BrewingRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Events implements Listener {

    private static final List<UUID> currentlyBullying = new ArrayList<>();

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
        if (shatteredEmpires.getWebhook() == null) return;

        DiscordWebhook webhook = shatteredEmpires.getWebhook();
        // Let's not delay the chat message.
        Bukkit.getScheduler().runTaskAsynchronously(shatteredEmpires, () -> webhook.sendMessage(event.getPlayer(), message));
    }

    private static boolean shouldBully(@NotNull ItemStack itemStack) {
        LocalDateTime now = LocalDateTime.now();
        return (itemStack.getType() == Material.COOKIE || itemStack.getType() == Material.BREAD || itemStack.getType().name().startsWith("COOKED")) && now.getDayOfMonth() == 1 && now.getMonth() == Month.APRIL;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getJoinMessage() == null) return;

        ShatteredEmpires.getInstance().getWebhook().sendAlert(event.getJoinMessage().replace("§e", ""));

        if (shouldBully(new ItemStack(Material.BREAD))) {
            for (int i = 0; i < 50; i++) {
                event.getPlayer().sendMessage("§c§lBULLY MODE ACTIVATED");
                event.getPlayer().sendMessage("§c§lBULLY MODE ACTIVATED");
                event.getPlayer().sendMessage("§c§lBULLY MODE ACTIVATED");
            }
            event.getPlayer().sendMessage(" ");
            event.getPlayer().sendMessage("Here are your coords, in case you get lost: " + event.getPlayer().getLocation().getBlockX() + " " + event.getPlayer().getLocation().getBlockY() + " " + event.getPlayer().getLocation().getBlockZ());

            Bukkit.getScheduler().scheduleSyncDelayedTask(ShatteredEmpires.getInstance(), () -> {
                currentlyBullying.add(event.getPlayer().getUniqueId());
                // Mess with the player.
                for (int i = 0; i < 50; i++) {
                    int finalI = i;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(ShatteredEmpires.getInstance(), () -> {
                        event.getPlayer().sendTitle("§c§lBULLY MODE ACTIVATED", "§c§lBULLY MODE ACTIVATED " + finalI + "/49", 0, finalI * 20 + 20, 0);
                        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 100, true, false));

                        event.getPlayer().setVelocity(new Vector(new Random().nextDouble(-2, 2), new Random().nextDouble(-2, 2), new Random().nextDouble(-2, 2)));

                        if (finalI == 49) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(ShatteredEmpires.getInstance(), () -> {
                                currentlyBullying.remove(event.getPlayer().getUniqueId());
                                event.getPlayer().sendTitle("", "", 10, 20, 10);
                                event.getPlayer().sendMessage("§c§lBULLY MODE DEACTIVATED");
                            }, 20L);
                        }
                    }, i * 20);
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if(event.getQuitMessage() == null) return;

        ShatteredEmpires.getInstance().getWebhook().sendAlert(event.getQuitMessage().replace("§e", ""));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getDeathMessage() == null) return;

        ShatteredEmpires.getInstance().getWebhook().sendAlert(event.getDeathMessage().replace("§e", ""));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(currentlyBullying.contains(event.getEntity().getUniqueId()));
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (shouldBully(event.getItemDrop().getItemStack())) {
            Player p = event.getPlayer();
            p.sendMessage("You are getting bullied by devman.");
            p.sendTitle(String.format("Catch your %s", event.getItemDrop().getName()), "(It will duplicate)", 0, 20, 0);

            Location origin = event.getItemDrop().getLocation();
            event.getItemDrop().setGlowing(true);
            Bukkit.getScheduler().scheduleSyncRepeatingTask(ShatteredEmpires.getInstance(), () -> {
                // Let the item drop if it's too high.
                int maxAllowance = (event.getItemDrop().getLocation().getWorld().getHighestBlockYAt(event.getItemDrop().getLocation())) + 1;
                if (event.getItemDrop().getLocation().toVector().getY() > maxAllowance) return;

                event.getItemDrop().setVisualFire(true);
                event.getItemDrop().teleport(origin.add(new Random().nextInt(5), new Random().nextInt(5), new Random().nextInt(5)));
                p.setSprinting(true);
            }, 0, 20L);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (shouldBully(event.getItem().getItemStack())) {
                ItemStack bread = event.getItem().getItemStack().clone();
                bread.setAmount(event.getItem().getItemStack().getAmount());

                p.getInventory().addItem(bread);
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
