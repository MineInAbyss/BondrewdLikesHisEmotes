package io.github.bananapuncher714.bondrewd.likes.his.emotes;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.StringTransformer;

public class BondrewdLikesHisEmotes extends JavaPlugin {
	private PacketHandler handler;
	private StringTransformer transformer;
	
	private File MODIFIED_FONT;
	private File ASSET_DIR;
	private File ORIGINAL_FONT;
	
	@Override
	public void onEnable() {
		handler = ReflectionUtil.getNewPacketHandlerInstance();
	}
}
