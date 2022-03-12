package me.grplayer;

import me.grplayer.lib.discord.DiscordWebhook;
import me.grplayer.lib.naj0jerk.BrewAction;
import me.grplayer.lib.naj0jerk.BrewingRecipe;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;

public class ShatteredEmpires extends JavaPlugin {

    private DiscordWebhook webhook;

    @Override
    public void onEnable() {
        // Before we do anything, let's save the config
        saveDefaultConfig();

        ConfigurationSection messages = getConfig().getConfigurationSection("messages");
        if(messages.getBoolean("forward")) {
            webhook = new DiscordWebhook(messages.getString("webhook"));
        }

        new BrewingRecipe(Material.EMERALD, (inventory, item, ingridient) -> {
            boolean isPotion = item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION;
            if(!isPotion) return false;

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            boolean extended = meta.getBasePotionData().isExtended();
            if(meta.getBasePotionData().getType() != PotionType.THICK) return false;

            meta.setBasePotionData(new PotionData(PotionType.THICK, extended, false));
            meta.addCustomEffect(new PotionEffect(PotionEffectType.BAD_OMEN, Integer.MAX_VALUE, 0), true);
            meta.setColor(Color.RED);
            meta.setDisplayName("§c§lPotion of Foreboding");
            item.setItemMeta(meta);

            return true;
        });

        getServer().getPluginManager().registerEvents(new Events(), this);
        getLogger().info("Dear server admin,\n" + "Shattered Empires extensions has been made possible by some \"borrowed\" code, which is not mine.\n - " + getDescription().getAuthors().get(0) + "\n" + "The original author is NacOJerk, URL: https://www.spigotmc.org/threads/how-to-make-custom-potions-and-brewing-recipes.211002/");
        getLogger().info("Shattered Empires extensions has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shattered Empires extensions has been disabled!");
    }

    private static ShatteredEmpires instance;

    public static ShatteredEmpires getInstance() {
        return instance;
    }

    public ShatteredEmpires() {
        instance = this;
    }

    public DiscordWebhook getWebhook() {
        return webhook;
    }
}
