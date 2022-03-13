package me.grplayer.lib.discord;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A really basic sanitizer for Discord messages.
 * Removes all Discord-specific formatting codes.
 * @author Gersom
 */
public class DiscordSanitizer {

    /**
     * Sanitizes a Discord message.
     * @param s The message to sanitize.
     * @return The sanitized message.
     */
    @Contract(pure = true)
    public static @NotNull String sanitize(String s) {
       return s.replaceAll("<@", "").replaceAll("<@&", "").replaceAll(">", "").replaceAll("@everyone", "@\u200beveryone").replaceAll("@here", "@\u200bhere");
    }

}
