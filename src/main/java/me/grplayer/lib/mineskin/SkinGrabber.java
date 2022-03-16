package me.grplayer.lib.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.grplayer.ShatteredEmpires;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

public class SkinGrabber {

    private static final Gson GSON = new Gson();

    private final ShatteredEmpires shatteredEmpires;
    private final FileConfiguration config;

    public SkinGrabber(@NotNull ShatteredEmpires shatteredEmpires) {
        this.shatteredEmpires = shatteredEmpires;

        this.config = shatteredEmpires.getCorpseConfiguration();
        if(this.config.get("skins") == null) config.createSection("skins");
        this.shatteredEmpires.saveConfigurations();
    }

    public String @NotNull [] stealSkin(@NotNull OfflinePlayer player) {
        String[] skin = new String[3];
        UUID owner = player.getUniqueId();

        ConfigurationSection section = this.config.getConfigurationSection("skins");
        if(section == null) return skin;

        ConfigurationSection skinSection = section.getConfigurationSection(owner.toString());
        if(skinSection == null) {
            try {
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + owner.toString().replace("-", "") + "?unsigned=false");
                JsonObject json = getJsonFromUrl(url);

                JsonObject properties = json.getAsJsonArray("properties").get(0).getAsJsonObject();
                String name = properties.get("name").getAsString();
                String value = properties.get("value").getAsString();
                String signature = properties.get("signature").getAsString();

                skin[0] = name;
                skin[1] = value;
                skin[2] = signature;

                saveSkin(owner, name, value, signature);
            } catch (IOException e) {
                throw new RuntimeException("Failed to steal skin for " + player.getName(), e);
            }
        }else {
            skin[0] = skinSection.getString("name");
            skin[1] = skinSection.getString("value");
            skin[2] = skinSection.getString("signature");
        }

        return skin;
    }

    private void saveSkin(UUID owner, String name, String value, String signature) {
        ConfigurationSection section = this.config.getConfigurationSection("skins");
        if (section == null) return;

        if(section.get(owner.toString()) != null) section.set(owner.toString(), null);

        ConfigurationSection skin = section.createSection(owner.toString());
        skin.set("name", name);
        skin.set("value", value);
        skin.set("signature", signature);

        this.shatteredEmpires.saveConfigurations();
    }

    public void updateSkinDatabase() {
        ConfigurationSection skins = this.config.getConfigurationSection("skins");
        if(skins == null) return;

        this.shatteredEmpires.getLogger().info("Updating skin database...");
        for(String key : skins.getKeys(false)) {
            ConfigurationSection skin = skins.getConfigurationSection(key);
            if(skin == null) continue;

            String name = skin.getString("name");
            String value = skin.getString("value");
            String signature = skin.getString("signature");

            if(name == null || value == null || signature == null) continue;

            String[] upToDateValues = this.stealSkin(Bukkit.getOfflinePlayer(UUID.fromString(key)));
            if(upToDateValues[0] == null || upToDateValues[1] == null || upToDateValues[2] == null) continue;

            if(!name.equals(upToDateValues[0]) || !value.equals(upToDateValues[1]) || !signature.equals(upToDateValues[2])) {
                saveSkin(UUID.fromString(key), upToDateValues[0], upToDateValues[1], upToDateValues[2]);
                this.shatteredEmpires.getLogger().info("Updated skin for " + Bukkit.getOfflinePlayer(UUID.fromString(key)).getName());
            }
        }

        this.shatteredEmpires.getLogger().info("Skin database updated.");
    }

    private static JsonObject getJsonFromUrl(@NotNull URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", Bukkit.getBukkitVersion());
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.connect();

        String response = new String(connection.getInputStream().readAllBytes());

        return GSON.fromJson(response, JsonObject.class);
    }

}
