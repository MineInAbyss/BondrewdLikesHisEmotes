package io.github.bananapuncher714.bondrewd.likes.his.emotes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.StringTransformer;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.FontBitmap;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.FontIndex;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.NamespacedKey;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.FileUtil;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.ReflectionUtil;

public class BondrewdLikesHisEmotes extends JavaPlugin {
	private static final char STARTING_CHAR = '\uEBAF';
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	private PacketHandler handler;
	private StringTransformer transformer;
	
	private File MODIFIED_FONT;
	private File ASSET_DIR;
	private File ORIGINAL_FONT;
	
	private Map< String, Character > emotes = new HashMap< String, Character >();
	
	@Override
	public void onEnable() {
		handler = ReflectionUtil.getNewPacketHandlerInstance();
		
		// Inject all future players
		Bukkit.getPluginManager().registerEvents( new Listener() {
			@EventHandler
			private void onEvent( PlayerJoinEvent event ) {
				handler.inject( event.getPlayer() );
			}
		}, this );
		
		// Inject all currently online players
		for ( Player player : Bukkit.getOnlinePlayers() ) {
			handler.inject( player );
		}
		
		MODIFIED_FONT = new File( getDataFolder() + "/" + "default.json" );
		ASSET_DIR = new File( getDataFolder() + "/assets/" );
		ORIGINAL_FONT = new File( ASSET_DIR + "/" + "default.json" );
		
		ASSET_DIR.mkdirs();
		
		FileUtil.saveToFile( getResource( "README.md" ), new File( getDataFolder() + "/" + "README.md" ), true );
		
		loadEmotes();
		loadPermissions();
	}
	
	@Override
	public void onDisable() {
		// Clean up
		for ( Player player : Bukkit.getOnlinePlayers() ) {
			handler.uninject( player );
		}
	}
	
	private void loadEmotes() {
		FontIndex index;
		if ( ORIGINAL_FONT.exists() && ORIGINAL_FONT.isFile() ) {
			try {
				JsonObject obj = GSON.fromJson( new InputStreamReader( new FileInputStream( ORIGINAL_FONT ), "UTF-8" ), JsonObject.class );
				index = new FontIndex( obj );
			} catch ( JsonSyntaxException | JsonIOException | UnsupportedEncodingException | FileNotFoundException e ) {
				getLogger().severe( "Unable to read " + ORIGINAL_FONT );
				e.printStackTrace();
				index = new FontIndex();
			}
		} else {
			index = new FontIndex();
		}
		
		emotes.clear();
		int c = STARTING_CHAR;
		if ( ASSET_DIR.exists() && ASSET_DIR.isDirectory() ) {
			for ( File file : ASSET_DIR.listFiles() ) {
				String fileName = file.getName();
				if ( fileName.matches( "*\\.png" ) ) {
					String emoteName = fileName.replaceFirst( "\\.png$", "" );
					emotes.put( emoteName, ( char ) c );
					
					FontBitmap provider = new FontBitmap( new NamespacedKey( "emotes/" + fileName ), new String[] { c + "" } );
					index.addProvider( provider );

					c++;
				}
			}
		}
		
		if ( MODIFIED_FONT.exists() ) {
			MODIFIED_FONT.delete();
		}
		
		try {
			GSON.toJson( index.toJsonObject(), new FileWriter( MODIFIED_FONT ) );
		} catch ( JsonIOException | IOException e ) {
			getLogger().severe( "Unable to write modified font to file!" );
			e.printStackTrace();
		}
	}
	
	private void loadPermissions() {
		
	}
}
