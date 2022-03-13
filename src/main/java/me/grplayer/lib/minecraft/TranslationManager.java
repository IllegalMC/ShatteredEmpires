package me.grplayer.lib.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.grplayer.ShatteredEmpires;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Realy simple translation manager.
 * Will download the latest Minecraft translation file from the current server version.
 * The method getTranslation will return the translation for the given key.
 * @author Gersom
 */
public class TranslationManager {

    private final static Gson GSON = new Gson();

    private final Logger logger;

    private final ShatteredEmpires shatteredEmpires;
    private final String lang;

    private final JsonObject languageFile;

    /**
     * Constructor, will download the latest Minecraft translation file from the current server version if it doesn't exist. Will also load the translation file.
     * @param shatteredEmpires The main class of Shattered Empires.
     * @throws IOException If the language file couldn't be loaded.
     */
    public TranslationManager(ShatteredEmpires shatteredEmpires) throws IOException {
        this.shatteredEmpires = shatteredEmpires;
        this.logger = shatteredEmpires.getLogger();

        this.lang = shatteredEmpires.getConfig().getString("lang", "en_us");

        File langFile = getLanguageFile();

        if (!langFile.exists()) {
            downloadResources();
        }

        this.languageFile = GSON.fromJson(Files.readString(Path.of(langFile.getPath())), JsonObject.class);
    }

    /**
     * Get the translation for the given key.
     * @param key The key of the translation.
     * @return The translation for the given key.
     */
    public String getTranslation(String key) {
        return this.languageFile.get(key).getAsString();
    }

    private File getLanguageFile() {
        return new File(shatteredEmpires.getDataFolder(), this.lang + ".json");
    }

    private void downloadResources() {
        String version = shatteredEmpires.getServer().getClass().getPackageName().replace("org.bukkit.craftbukkit.v", "").replace("_", ".").replace("R", "");

        String url = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/" + version + "/assets/minecraft/lang/" + this.lang + ".json";
        logger.info("Downloading language file from " + url);

        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setDoInput(true);

            InputStream stream = connection.getInputStream();

            File langFile = getLanguageFile();
            if (!langFile.exists()) {
                langFile.createNewFile();
                Files.write(langFile.toPath(), stream.readAllBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
