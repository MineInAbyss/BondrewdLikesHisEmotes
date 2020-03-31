package io.github.bananapuncher714.bondrewd.likes.his.emotes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

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
import io.github.bananapuncher714.bondrewd.likes.his.emotes.tinyprotocol.TinyProtocol;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.FileUtil;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.PermissionBuilder;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.ReflectionUtil;

public class BondrewdLikesHisEmotes extends JavaPlugin {
	// Could technically be 8, but it's small enough as it is so why not 9
	public static final int EMOTE_HEIGHT = 9;
	private static final char STARTING_CHAR = '\uEBAF';
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private PacketHandler handler;
	private StringTransformer transformer = this::transform;

	private File MODIFIED_FONT;
	private File ASSET_DIR;
	private File ORIGINAL_FONT;

	private Map< String, Character > emotes = new HashMap< String, Character >();

	@Override
	public void onEnable() {
		handler = ReflectionUtil.getNewPacketHandlerInstance();
		if ( handler == null ) {
			getLogger().severe( ReflectionUtil.VERSION + " is not currently supported! Disabling..." );
			setEnabled( false );
			return;
		} else {
			getLogger().info( "Detected version " + ReflectionUtil.VERSION );
		}

		handler.setTransformer( transformer );

		new TinyProtocol( this ) {};

		MODIFIED_FONT = new File( getDataFolder() + "/" + "default.json" );
		ASSET_DIR = new File( getDataFolder() + "/assets/" );
		ORIGINAL_FONT = new File( ASSET_DIR + "/" + "default.json" );

		ASSET_DIR.mkdirs();

		FileUtil.saveToFile( getResource( "README.md" ), new File( getDataFolder() + "/" + "README.md" ), true );

		loadEmotes();
		loadPermissions();

		if ( emotes.isEmpty() ) {
			getLogger().info( "Didn't detect any emotes! Perhaps placed in the wrong folder?" );
		} else {
			getLogger().info( "Loaded emotes: " + String.join( " ", emotes.keySet() ) );
		}
	}

	@Override
	public List< String > onTabComplete( CommandSender sender, Command command, String alias, String[] args ) {
		// Just a simple tab complete for now...
		List< String > completions = new ArrayList< String >();
		List< String > suggestions = new ArrayList< String >();
		if ( args.length == 1 ) {
			if ( sender.hasPermission( "bondrewdemotes.reload" ) ) {
				suggestions.add( "reload" );
			}
			if ( sender.hasPermission( "bondrewdemotes.list" ) ) {
				suggestions.add( "list" );
			}
		}
		StringUtil.copyPartialMatches( args[ args.length - 1 ], suggestions, completions);
		Collections.sort( completions );
		return completions;
	}

	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args ) {
		if ( args.length == 1 ) {
			if ( args[ 0 ].equalsIgnoreCase( "reload" ) ) {
				if ( sender.hasPermission( "bondrewdemotes.reload" ) ) {
					loadEmotes();
					loadPermissions();
					sender.sendMessage( ChatColor.GREEN + "Done!" );
				} else {
					sender.sendMessage( ChatColor.RED + "You do not have permission to run this command!" );
				}
			} else if ( args[ 0 ].equalsIgnoreCase( "list" ) ) {
				if ( sender.hasPermission( "bondrewdemotes.list" ) ) {

					StringBuilder builder = new StringBuilder( ChatColor.WHITE + "Available emotes: " );
					builder.append( ChatColor.YELLOW );
					boolean found = false;
					for ( String key : emotes.keySet() ) {
						if ( sender.hasPermission( "bondrewdemotes.emote." + key ) ) {
							builder.append( key );
							builder.append( " " );
							found = true;
						}
					}
					if ( found ) {
						sender.sendMessage( builder.toString() );
					} else {
						sender.sendMessage( ChatColor.RED + "You cannot use any emotes!" );
					}
				} else {
					sender.sendMessage( ChatColor.RED + "You do not have permission to run this command!" );
				}
			} else {
				sender.sendMessage( ChatColor.RED + "Invalid arguments!" );
			}
		} else {
			sender.sendMessage( ChatColor.RED + "Invalid arguments!" );
		}
		return false;
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
				if ( fileName.endsWith( ".png" ) ) {
					String emoteName = fileName.replaceFirst( "\\.png$", "" );
					emotes.put( emoteName, ( char ) c );

					FontBitmap provider = new FontBitmap( new NamespacedKey( "emotes/" + fileName ), new String[] { String.valueOf( ( char ) c ) } );
					index.addProvider( provider );

					c++;
				}
			}
		}

		if ( MODIFIED_FONT.exists() ) {
			MODIFIED_FONT.delete();
		}

		try {
			OutputStreamWriter writer = new OutputStreamWriter( new FileOutputStream( MODIFIED_FONT ), StandardCharsets.UTF_8 );
			GSON.toJson( index.toJsonObject(), writer );
			writer.close();
		} catch ( IOException e ) {
			getLogger().severe( "Unable to save modified font file!" );
			e.printStackTrace();
		}
	}

	private void loadPermissions() {
		PermissionBuilder admin = new PermissionBuilder( "bondrewdemotes.admin" ).setDefault( PermissionDefault.OP );
		PermissionBuilder all = new PermissionBuilder( "bondrewdemotes.all" ).setDefault( PermissionDefault.OP );
		for ( String key : emotes.keySet() ) {
			all.addChild( new PermissionBuilder( "bondrewdemotes.emote." + key ).setDefault( PermissionDefault.OP ).register().build(), true );
		}

		admin.addChild( all.register().build(), true ).register();
		admin.addChild( new PermissionBuilder( "bondrewdemotes.reload" ).setDefault( PermissionDefault.OP ).register().build(), true );
		admin.addChild( new PermissionBuilder( "bondrewdemotes.list" ).setDefault( PermissionDefault.TRUE ).register().build(), true );
	}

	private String transform( String string ) {
		for ( Entry< String, Character > entry : emotes.entrySet() ) {
			String search = ":" + entry.getKey() + ":";
			string = string.replaceAll( "(?<!\\\\)" + search, String.valueOf( entry.getValue() ) );
			string = string.replace( "\\\\" + search, search );
		}
		return string;
	}

	public static PacketHandler getHandler() {
		return JavaPlugin.getPlugin( BondrewdLikesHisEmotes.class ).handler;
	}
}
