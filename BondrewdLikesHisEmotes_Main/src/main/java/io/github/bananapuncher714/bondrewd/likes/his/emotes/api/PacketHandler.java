package io.github.bananapuncher714.bondrewd.likes.his.emotes.api;

import io.netty.channel.Channel;

public interface PacketHandler {
	void inject( Channel channel );
	void setTransformer( StringTransformer transformer );
	StringTransformer getTransformer();
}
