package me.grplayer.lib.corpses;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Corpse {

    // We need to give every corpse a unique id, because we need to be able to remove them later, and since a player can have multiple corpses (They can die multiple times) we can't use their UUID.
    private final UUID id;

    private final OfflinePlayer owner;
    private final UUID nameTag;
    private final Location deathLocation, chestLocation;

    public void saveToConfig(@NotNull Configuration config) {
        ConfigurationSection section = config.getConfigurationSection("corpses").createSection(id.toString());
        section.set("owner", owner.getUniqueId().toString());
        section.set("nameTag", nameTag.toString());
        section.set("deathLocation", deathLocation.toString());
        section.set("chestLocation", chestLocation.toString());
    }

    public void remove(Configuration config) {
        World world = deathLocation.getWorld();
        Entity nameTag = getEntity(world, this.nameTag);

        nameTag.remove();

        if(world.getBlockAt(chestLocation).getType().equals(org.bukkit.Material.CHEST)) {
            Chest chest = (Chest) world.getBlockAt(chestLocation).getState();
            ItemStack[] loot = chest.getBlockInventory().getContents();
            chest.getBlockInventory().clear();
            for(ItemStack item : loot) {
                if(item == null) continue;
                chest.getLocation().getWorld().dropItem(chest.getLocation(), item);
            }

            world.getBlockAt(chestLocation).setType(Material.AIR);
        }

        config.getConfigurationSection("corpses").set(id.toString(), null);
    }

    public Location getChestLocation() {
        return this.chestLocation;
    }

    public void spawn() {

    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull Corpse createCorpse(OfflinePlayer owner, @NotNull Entity nameTag, Location deathLocation, Location chestLocation) {
        return new Corpse(UUID.randomUUID(), owner, nameTag, deathLocation, chestLocation);
    }

    private Corpse(UUID id, OfflinePlayer owner, @NotNull Entity nameTag, Location deathLocation, Location chestLocation) {
        this.id = id;
        this.owner = owner;
        this.nameTag = nameTag.getUniqueId();
        this.deathLocation = deathLocation;
        this.chestLocation = chestLocation;
    }

    public static @Nullable Corpse fromConfig(@NotNull Configuration config, UUID id) {
        ConfigurationSection section = config.getConfigurationSection("corpses");
        if(section == null) return null;

        OfflinePlayer owner = section.getString("owner") == null ? null : org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(section.getString("owner")));
        UUID nameTag = UUID.fromString(section.getConfigurationSection(id.toString()).getString("nameTag"));
        Location deathLocation = section.getLocation("deathLocation", null);
        Location chestLocation = section.getLocation("chestLocation", null);

        if(deathLocation == null || chestLocation == null) return null;

        Entity nameTagEntity = getEntity(deathLocation.getWorld(), nameTag);

        if(nameTagEntity == null) return null;

        return new Corpse(id, owner, nameTagEntity, deathLocation, chestLocation);
    }

    private static @Nullable Entity getEntity(@NotNull World world, UUID uuid) {
        for(Entity entity : world.getEntities()) {
            if(entity.getUniqueId().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }
}
