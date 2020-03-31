package io.github.bananapuncher714.bondrewd.likes.his.emotes.util;

import org.bukkit.Bukkit;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;

public final class ReflectionUtil {
	public static final String VERSION;
	
	static {
		VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}

	private ReflectionUtil() {
	}
	
	public static PacketHandler getNewPacketHandlerInstance() {
		try {
			Class< ? > clazz = Class.forName( "io.github.bananapuncher714.bondrewd.likes.his.emotes.implementation." + VERSION + ".NMSHandler" );
			return ( PacketHandler ) clazz.newInstance();
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			e.printStackTrace();
			return null;
		}
	}
}
