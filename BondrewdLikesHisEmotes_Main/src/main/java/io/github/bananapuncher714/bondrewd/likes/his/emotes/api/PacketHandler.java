package io.github.bananapuncher714.bondrewd.likes.his.emotes.api;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.BondrewdLikesHisEmotes;
import org.bukkit.entity.Player;

public interface PacketHandler {
	void inject( Player player );
	void addChatCompletions(Player player, BondrewdLikesHisEmotes plugin);

	void uninject(Player player );
	void setTransformer( ComponentTransformer transformer );
	ComponentTransformer getTransformer();
}
