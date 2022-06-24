package io.github.bananapuncher714.bondrewd.likes.his.emotes.util;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.BondrewdLikesHisEmotes;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.Emote;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PlaceholderUtil extends PlaceholderExpansion implements Listener {

    private final BondrewdLikesHisEmotes plugin;

    public PlaceholderUtil(BondrewdLikesHisEmotes plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bondrewd";
    }

    @Override
    public @NotNull String getAuthor() {
        return "bananapuncher";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1.3";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    //Ideally this would return the unicode formatted to correct font but not really doable afaik here
    // Would need to intercept elsewhere with PAPI ithink
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String string) {
        for (Emote emote : plugin.getEmotes()) {
            if (Objects.equals(emote.getId(), string)) {
                return ":"+emote.getId()+":";
            }
        }
        return null;
    }
}
