package me.grplayer.lib;

import java.util.UUID;

/**
 * Basic skin library
 * @author Gersom
 */
public class SkinLib {

    public static String getAvatar(UUID uuid) {
        return "https://crafatar.com/avatars/" + uuid.toString().replace("-", "");
    }

}
