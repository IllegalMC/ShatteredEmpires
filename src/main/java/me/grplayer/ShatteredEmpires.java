package me.grplayer;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.grplayer.lib.corpses.CorpseManager;
import me.grplayer.lib.discord.DiscordWebhook;
import me.grplayer.lib.minecraft.TranslationManager;
import me.grplayer.lib.naj0jerk.BrewingRecipe;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;

public class ShatteredEmpires extends JavaPlugin {

    private ProtocolManager protocolManager;

    private FileConfiguration corpseConfig;

    private DiscordWebhook webhook;
    private TranslationManager translationManager;
    private CorpseManager corpseManager;

    @Override
    public void onEnable() {
        // Before we do anything, let's save the configs
        setupConfigurations();

        // Then, we initialize the translation manager
        try {
            translationManager = new TranslationManager(this);
        } catch (IOException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);

            return;
        }

        // Then, we initialize the webhook
        ConfigurationSection messages = getConfig().getConfigurationSection("messages");
        if(messages.getBoolean("forward")) {
            webhook = new DiscordWebhook(messages.getString("webhook"));
        }

        // Then, we initialize the protocol manager & the corpse manager
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.corpseManager = new CorpseManager(this);
        this.corpseManager.spawnCorpses();

        // And finally, we register the brewing recipe & events
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

    private void setupConfigurations() {
        saveDefaultConfig();

        File corpseConfigFile = new File(getDataFolder(), "corpses.yml");
        if (!corpseConfigFile.exists()) {
            corpseConfigFile.getParentFile().mkdirs();
            saveResource("corpses.yml", false);
        }

        this.corpseConfig = new YamlConfiguration();
        try {
            this.corpseConfig.load(corpseConfigFile);
            if(this.corpseConfig.getConfigurationSection("corpses") == null) this.corpseConfig.createSection("corpses");
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void saveConfigurations() {
        saveDefaultConfig();

        try {
            corpseConfig.save(new File(getDataFolder(), "corpses.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    public CorpseManager getCorpseManager() {
        return corpseManager;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public FileConfiguration getCorpseConfiguration() {
        return this.corpseConfig;
    }
}
