package me.grplayer.lib.corpses;

import com.comphenix.protocol.ProtocolManager;
import me.grplayer.ShatteredEmpires;
import me.grplayer.lib.mineskin.SkinGrabber;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class CorpseManager {

    private final ShatteredEmpires shatteredEmpires;
    private final SkinGrabber skinGrabber;
    private final ProtocolManager protocolManager;
    private final FileConfiguration config;
    private final Logger logger;

    private final List<Corpse> corpses = Collections.synchronizedList(new java.util.ArrayList<>());

    public CorpseManager(@NotNull ShatteredEmpires shatteredEmpires) {
        this.shatteredEmpires = shatteredEmpires;
        this.protocolManager = shatteredEmpires.getProtocolManager();
        this.config = shatteredEmpires.getCorpseConfiguration();
        this.logger = shatteredEmpires.getLogger();

        // Load corpses
        for(String key : this.config.getConfigurationSection("corpses").getKeys(false)) {
            shatteredEmpires.getLogger().info("Loading corpse: " + key);
            UUID uuid = UUID.fromString(key);
            Corpse corpse = Corpse.fromConfig(this.config, uuid, protocolManager);
            if(corpse != null) this.corpses.add(corpse);
        }

        // Load skins
        this.skinGrabber = new SkinGrabber(this.shatteredEmpires);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this.shatteredEmpires, this.skinGrabber::updateSkinDatabase, 0, 20 * 30);
    }

    public void spawnCorpse(UUID player, @NotNull Location deathLocation, String deathMessage, List<ItemStack> drops) {
        if(deathLocation.getWorld() == null) return;

        logger.info("Spawning corpse for " + player.toString());

        // Wohoo, we spawned a corpse! Now it is time to spawn a chest with the drops
        Location chestLocation = findOptimalChestLocation(deathLocation);

        if(chestLocation.getBlock().getType() == Material.AIR) {
            chestLocation.getBlock().setType(Material.CHEST);
        }

        if(chestLocation.getBlock().getState() instanceof Chest chest) {
            Inventory chestInventory = chest.getBlockInventory();
            chestInventory.setContents(drops.toArray(new ItemStack[0]));
            logger.info("Spawned chest with " + chestInventory.getContents().length + " items");
        }else {
            // Drop the items on the ground if the chest is not there
            chestLocation.getBlock().setType(Material.AIR);
            for(ItemStack item : drops) {
                chestLocation.getWorld().dropItem(chestLocation, item);
            }

            logger.warning("Could not spawn chest at " + chestLocation.toString());
            logger.severe("All items were dropped on the ground");
        }

        // Now let's spawn a name tag with the death message
        Location nameTagLocation = deathLocation.clone();
        nameTagLocation.setY(nameTagLocation.getY() + 1);
        ArmorStand nameTag = nameTagLocation.getWorld().spawn(deathLocation, ArmorStand.class);
        String nameTagText = deathLocation.distance(chestLocation) > 10 ? deathMessage + " | " + "Loot at: " + chestLocation.getBlockX() + " " + chestLocation.getBlockY() + " " + chestLocation.getBlockZ() : deathMessage;
        nameTag.setCustomName(nameTagText);
        nameTag.setCustomNameVisible(true);
        nameTag.setVisible(false);
        nameTag.setGravity(false);
        nameTag.setInvulnerable(true);
        nameTag.setSmall(true);
        nameTag.setCollidable(false);

        Corpse corpse = Corpse.createCorpse(Bukkit.getOfflinePlayer(player), nameTag, deathLocation, chestLocation, protocolManager);
        corpse.spawn();
        corpse.saveToConfig(config);

        this.shatteredEmpires.saveConfigurations();

        this.corpses.add(corpse);
    }

    private @NotNull Location findOptimalChestLocation(@NotNull Location deathLocation) {
        // Find the chest location
        Location chestLocation = deathLocation.clone();
        chestLocation.setX(chestLocation.getX() + 1);

        // If there is no air, try it again.
        while(chestLocation.getBlock().getType() != Material.AIR) {
            Location testLocation = chestLocation.clone();
            testLocation.setX(testLocation.getX() + 1);
            testLocation.setY(testLocation.getY() + 1);
            if(testLocation.getBlock().getType() == Material.AIR) {
                chestLocation = testLocation;
            }else {
                chestLocation.setX(chestLocation.getX() + 1);
            }
        }

        return chestLocation.getBlock().getLocation();
    }

    public void removeCorpse(Location chestLocation) {
        for(Corpse corpse : this.corpses) {
            if(corpse.getChestLocation().toVector().equals(chestLocation.toVector())) {
                corpse.remove(this.config);
                this.corpses.remove(corpse);
                this.shatteredEmpires.saveConfigurations();
                break;
            }
        }
    }

    public void spawnCorpses() {
        for(Corpse corpse : this.corpses) {
            corpse.spawn();
        }
    }

    public void spawnCorpsesOnJoin(Player player) {
        for(Corpse corpse : this.corpses) {
            corpse.spawnForPlayer(player);
        }
    }

    protected SkinGrabber getSkinGrabber() {
        return this.skinGrabber;
    }

}
