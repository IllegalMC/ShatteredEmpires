package me.grplayer.lib.corpses;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import me.grplayer.ShatteredEmpires;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Corpse {

    // We need to give every corpse a unique id, because we need to be able to remove them later, and since a player can have multiple corpses (They can die multiple times) we can't use their UUID.
    private final UUID id;

    private final OfflinePlayer owner;
    private final UUID nameTag;
    private final Location deathLocation, chestLocation;
    private final ProtocolManager protocolManager;

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

        for(Player player : world.getPlayers()) {
            removeForPlayer(player);
        }

        config.getConfigurationSection("corpses").set(id.toString(), null);
    }

    public Location getChestLocation() {
        return this.chestLocation;
    }

    public void spawn() {
        Player[] targets = deathLocation.getWorld().getPlayers().toArray(new Player[0]);

        // Send out the packets
        for(Player target : targets) {
            spawnForPlayer(target);
        }
    }

    protected void spawnForPlayer(Player player) {
        // UUID + Entity ID of the corpse
        UUID uuid = UUID.randomUUID();
        int entityId = calculateEntityId(player);

        PacketContainer infoPacket = getInfoPacket(uuid, owner.getUniqueId());
        PacketContainer spawnPacket = getSpawnPacket(uuid, entityId, deathLocation);
        PacketContainer hidePacket = getHidePacket(uuid);

        try {
            this.protocolManager.sendServerPacket(player, infoPacket);
            this.protocolManager.sendServerPacket(player, spawnPacket);
            this.protocolManager.sendServerPacket(player, hidePacket);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Could not send packet of corpse for " + player.getName(), e);
        }
    }

    private void removeForPlayer(Player player) {
        int entityId = calculateEntityId(player);
        PacketContainer destroyPacket = getDestroyPacket(entityId);

        try {
            this.protocolManager.sendServerPacket(player, destroyPacket);
        }catch (InvocationTargetException e) {
            throw new RuntimeException("Could not send packet of corpse removal for " + player.getName(), e);
        }
    }

    private int calculateEntityId(@NotNull Player player) {
        return player.getEntityId() + player.getUniqueId().hashCode() + id.hashCode();
    }

    private @NotNull PacketContainer getInfoPacket(UUID uuid, UUID owner) {
        OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(owner);

        WrappedGameProfile gameProfile = new WrappedGameProfile(uuid, offlinePlayer.getName());
        String[] stolenSkin = ShatteredEmpires.getInstance().getCorpseManager().getSkinGrabber().stealSkin(offlinePlayer);
        gameProfile.getProperties().put("texture", WrappedSignedProperty.fromValues(stolenSkin[0], stolenSkin[1], stolenSkin[2]));

        PacketContainer infoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        infoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

        infoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(
                gameProfile,
                0,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText(offlinePlayer.getName())
        )));

        return infoPacket;
    }

    private @NotNull PacketContainer getSpawnPacket(UUID uuid, int entityID, @NotNull Location deathLocation) {
        Location toSpawn = deathLocation.clone();

        PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

        spawnPacket.getUUIDs().write(0, uuid);
        spawnPacket.getIntegers().write(0, entityID);

        // Location
        spawnPacket.getDoubles().write(0, toSpawn.getX());
        spawnPacket.getDoubles().write(1, toSpawn.getY());
        spawnPacket.getDoubles().write(2, toSpawn.getZ());

        // Yaw and pitch
        spawnPacket.getBytes().write(0, (byte) (float) toSpawn.getYaw());
        spawnPacket.getBytes().write(1, (byte) (float) toSpawn.getPitch());

        return spawnPacket;
    }

    private @NotNull PacketContainer getHidePacket(UUID uuid) {
        PacketContainer hidePacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        hidePacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        hidePacket.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(
                new WrappedGameProfile(uuid, ""),
                0,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText("")
        )));

        return hidePacket;
    }

    private @NotNull PacketContainer getDestroyPacket(int entityID) {
        PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntLists().write(0, List.of(entityID));

        return destroyPacket;
    }

    public static @NotNull Corpse createCorpse(OfflinePlayer owner, @NotNull Entity nameTag, Location deathLocation, Location chestLocation, ProtocolManager protocolManager) {
        return new Corpse(UUID.randomUUID(), owner, nameTag, deathLocation, chestLocation, protocolManager);
    }

    private Corpse(UUID id, OfflinePlayer owner, @NotNull Entity nameTag, Location deathLocation, Location chestLocation, ProtocolManager protocolManager) {
        this.id = id;
        this.owner = owner;
        this.nameTag = nameTag.getUniqueId();
        this.deathLocation = deathLocation;
        this.chestLocation = chestLocation;
        this.protocolManager = protocolManager;
    }

    public static @Nullable Corpse fromConfig(@NotNull Configuration config, UUID id, ProtocolManager protocolManager) {
        ConfigurationSection corpses = config.getConfigurationSection("corpses");
        if(corpses == null) return null;

        ConfigurationSection section = corpses.getConfigurationSection(id.toString());
        if(section == null) return null;

        OfflinePlayer owner = section.getString("owner") == null ? null : org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(section.getString("owner")));
        UUID nameTag = UUID.fromString(section.getString("nameTag"));

        Location deathLocation = parseLocationFromString(section.getString("deathLocation", null));
        Location chestLocation = parseLocationFromString(section.getString( "chestLocation", null));

        if(deathLocation == null || chestLocation == null) return null;

        Entity nameTagEntity = getEntity(deathLocation.getWorld(), nameTag);

        if(nameTagEntity == null) return null;

        return new Corpse(id, owner, nameTagEntity, deathLocation, chestLocation, protocolManager);
    }

    private static Location parseLocationFromString(String deathLocation) {
        if (deathLocation == null) return null;

        String data = deathLocation.split("Location")[1].replace("(", "").replace(")", "");
        String worldName = data.split("world=")[1].split(",")[0].split("=")[1].replace("}", "");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            ShatteredEmpires.getInstance().getLogger().warning("Could not find world " + worldName + " for corpse " + deathLocation);
            return null;
        }

        String[] coords = new String[]{data.split("x=")[1].split(",")[0], data.split("y=")[1].split(",")[0], data.split("z=")[1].split(",")[0]};
        String[] pitchYaw = new String[]{data.split("pitch=")[1].split(",")[0], data.split("yaw=")[1].split(",")[0].replace("}", "")};

        return new Location(world, Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]), Float.parseFloat(pitchYaw[0]), Float.parseFloat(pitchYaw[1]));
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
