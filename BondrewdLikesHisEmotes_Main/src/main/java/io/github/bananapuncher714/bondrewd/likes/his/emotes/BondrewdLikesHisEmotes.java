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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.FileUtil;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.PermissionBuilder;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.ReflectionUtil;

public class BondrewdLikesHisEmotes extends JavaPlugin {
	// Could technically be 8, but it's small enough as it is so why not 11
	private static int EMOTE_HEIGHT = 11;
	private static int EMOTE_ASCENT = 9;
	private static final char STARTING_CHAR = '\uEBAF';
	private static final String DEFAULT_NAMESPACE = "emotes/";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	private PacketHandler handler;
	private StringTransformer transformer = this::transform;

	private File MODIFIED_FONT;
	private File ASSET_DIR;
	private File ORIGINAL_FONT;

	// Prevent concurrent errors or something
	private Map< String, Character > emotes = new ConcurrentHashMap< String, Character >();

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

		Bukkit.getPluginManager().registerEvents( new Listener() {
			@EventHandler
			private void onEvent( PlayerJoinEvent event ) {
				handler.inject( event.getPlayer() );
			}
		}, this );
		
		MODIFIED_FONT = new File( getDataFolder() + "/" + "default.json" );
		ASSET_DIR = new File( getDataFolder() + "/convert/" );
		ORIGINAL_FONT = new File( ASSET_DIR + "/" + "default.json" );

		ASSET_DIR.mkdirs();

		FileUtil.saveToFile( getResource( "README.md" ), new File( getDataFolder() + "/" + "README.md" ), true );
		FileUtil.saveToFile( getResource( "config.yml" ), new File( getDataFolder() + "/" + "config.yml" ), false );
		
		loadConfig();
		loadPermissions();

		if ( emotes.isEmpty() ) {
			getLogger().info( "Didn't detect any emotes! Perhaps placed in the wrong folder?" );
		} else {
			getLogger().info( "Loaded emotes: " + String.join( " ", emotes.keySet() ) );
		}
	}

	@Override
	public void onDisable() {
		for ( Player player : Bukkit.getOnlinePlayers() ) {
			handler.uninject( player );
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
					loadConfig();
					loadPermissions();
					sender.sendMessage( ChatColor.GREEN + "Done!" );
				} else {
					sender.sendMessage( ChatColor.RED + "You do not have permission to run this command!" );
				}
			} else if ( args[ 0 ].equalsIgnoreCase( "list" ) ) {
				if ( sender.hasPermission( "bondrewdemotes.list" ) ) {

					StringBuilder builder = new StringBuilder( ChatColor.WHITE + "Available emotes: " );
					boolean found = false;
					for ( String key : emotes.keySet() ) {
						if ( sender.hasPermission( "bondrewdemotes.emote." + key ) ) {
							builder.append( ChatColor.WHITE );
							builder.append( emotes.get( key ) );
							builder.append( " " );
							builder.append( ChatColor.YELLOW );
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
	
	private void loadConfig() {
		FileConfiguration config = YamlConfiguration.loadConfiguration( new File( getDataFolder() + "/" + "config.yml" ) );
		
		EMOTE_HEIGHT = config.getInt( "default-height" );
		EMOTE_ASCENT = config.getInt( "default-ascent" );
		
		emotes.clear();
		
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
		
		int c = STARTING_CHAR;
		List< String > emoteList = config.getStringList( "emotes" );
		for ( String emote : emoteList ) {
			String[] emotePart = emote.split( "\\s+" );
			String name = emotePart[ 0 ];
			String namespace = emotePart.length > 1 ? emotePart[ 1 ] : DEFAULT_NAMESPACE + name + ".png";
			int height = emotePart.length > 3 ? Integer.parseInt( emotePart[ 2 ] ) : EMOTE_HEIGHT;
			int ascent = emotePart.length > 3 ? Integer.parseInt( emotePart[ 3 ] ) : EMOTE_ASCENT;
			
			emotes.put( name, ( char ) c );

			FontBitmap provider = new FontBitmap( new NamespacedKey( namespace ), new String[] { String.valueOf( ( char ) c ) } );
			provider.setHeight( height );
			provider.setAscent( ascent );
			index.addProvider( provider );

			c++;
		};
		
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
		// Right now the emote permissions don't do anything apart from showing up in the list of emotes.
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
	
	public static int getEmoteHeight() {
		return EMOTE_HEIGHT;
	}
	
	public static int getEmoteAscent() {
		return EMOTE_ASCENT;
	}
}
