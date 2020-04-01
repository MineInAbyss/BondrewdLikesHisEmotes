package io.github.bananapuncher714.bondrewd.likes.his.emotes.api;

import org.bukkit.entity.Player;

public interface PacketHandler {
	void inject( Player player );
	void uninject( Player player );
	void setTransformer( StringTransformer transformer );
	StringTransformer getTransformer();
}
