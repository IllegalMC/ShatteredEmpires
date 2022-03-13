package me.grplayer.lib.discord;

import me.grplayer.ShatteredEmpires;
import me.grplayer.lib.SkinLib;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * A really basic implementation of a Discord webhook.
 * @author Gersom
 */
public class DiscordWebhook {

    private final String url;

    /**
     * Creates a new Discord webhook.
     * @param url The URL of the webhook.
     */
    public DiscordWebhook(String url) {
        this.url = url;
    }

    /**
     * Sends a message with the webhook, with the player's name and skin.
     * @param sender The player who sent the message. Used to grab the username and avatar.
     * @param message The message the player send.
     */
    public void sendMessage(OfflinePlayer sender, String message) {
        String username = sender.getName();
        String avatar = SkinLib.getAvatar(sender.getUniqueId());

        String sanitized = DiscordSanitizer.sanitize(message);

        sendMessage(username, avatar, sanitized);
    }

    public void sendAlert(String alert) {
        String json = "{\"username\":\"Alert\",\"avatar_url\":\""+ ShatteredEmpires.getInstance().getConfig().getConfigurationSection("messages").getString("alert-profile") +"\",\"content\":\"**" + alert + "**\"}";
        try {
            sendRequest(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a message with the webhook, with the player's name and skin.
     * @param username The username of the player.
     * @param avatar The avatar of the player.
     * @param message The message the player send.
     */
    private void sendMessage(String username, String avatar, String message) {
        String json = "{\"username\":\"" + username + "\",\"avatar_url\":\"" + avatar + "\",\"content\":\"" + message + "\"}";
        try {
            sendRequest(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Connect to the webhook and send the request.
     * @param json The encoded JSON to send.
     * @throws IOException If the connection fails. When something goes wrong with the connection, or if the status code is not 200.
     */
    private void sendRequest(@NotNull String json) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", Integer.toString(json.length()));
        connection.getOutputStream().write(json.getBytes());

        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
        httpsConnection.setHostnameVerifier((s, sslSession) -> true);
        httpsConnection.setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
        httpsConnection.connect();

        int responseCode = httpsConnection.getResponseCode();
        if (!(responseCode >= 200 && responseCode < 300)) {
            throw new IOException("Discord returned " + responseCode);
        }
    }

}
