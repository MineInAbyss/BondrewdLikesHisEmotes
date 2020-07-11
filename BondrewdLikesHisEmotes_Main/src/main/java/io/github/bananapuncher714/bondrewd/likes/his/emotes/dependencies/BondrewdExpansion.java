package io.github.bananapuncher714.bondrewd.likes.his.emotes.dependencies;

import org.bukkit.entity.Player;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.BondrewdLikesHisEmotes;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class BondrewdExpansion extends PlaceholderExpansion {
	private BondrewdLikesHisEmotes plugin;
	
	public BondrewdExpansion( BondrewdLikesHisEmotes plugin ) {
		this.plugin = plugin;
	}
	
	@Override
	public String getAuthor() {
		return "Mine In Abyss";
	}

	@Override
	public String getIdentifier() {
		return "emote";
	}

	@Override
	public String getVersion() {
		return "1.0.6";
	}
	
	@Override
	public String onPlaceholderRequest( Player p, String params ) {
		return plugin.getEmoteFor( params );
	}
}
