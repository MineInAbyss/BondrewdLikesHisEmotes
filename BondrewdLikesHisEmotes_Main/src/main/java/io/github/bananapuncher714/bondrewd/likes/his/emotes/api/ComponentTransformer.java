package io.github.bananapuncher714.bondrewd.likes.his.emotes.api;

import net.md_5.bungee.api.chat.BaseComponent;

public interface ComponentTransformer {
	BaseComponent transform( BaseComponent component );
	String transform( String string );
}
