package io.github.bananapuncher714.bondrewd.likes.his.emotes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.ComponentTransformer;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.dependencies.BondrewdExpansion;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.FontBitmap;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.FontIndex;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.NamespacedKey;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.FileUtil;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.PermissionBuilder;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.ReflectionUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

public class BondrewdLikesHisEmotes extends JavaPlugin {
	// Could technically be 8, but it's small enough as it is so why not 11
	private static int EMOTE_HEIGHT = 11;
	private static int EMOTE_ASCENT = 9;
	private static final char STARTING_CHAR = '\uEBAF';
	private static final String DEFAULT_NAMESPACE = "emotes/";
	private static final String EMOTE_FORMAT = ":%s:";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	private PacketHandler handler;
	private List< Emote > emotes = new ArrayList< Emote >();
	
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

		handler.setTransformer( new ComponentTransformer() {
			@Override
			public BaseComponent transform( BaseComponent component ) {
				return transformComponent( component );
			}
			
			@Override
			public String transform( String string ) {
				return transformString( string );
			}
		} );

		Bukkit.getPluginManager().registerEvents( new Listener() {
			@EventHandler
			private void onEvent( PlayerJoinEvent event ) {
				handler.inject( event.getPlayer() );
			}
		}, this );
		
		FileUtil.saveToFile( getResource( "README.md" ), new File( getDataFolder() + "/" + "README.md" ), true );
		FileUtil.saveToFile( getResource( "config.yml" ), new File( getDataFolder() + "/" + "config.yml" ), false );
		
		loadConfig();
		loadPermissions();

		if ( emotes.isEmpty() ) {
			getLogger().info( "Didn't detect any emotes!" );
		} else {
			getLogger().info( "Loaded emotes: " + String.join( " ", emotes.stream().map( Emote::getId ).collect( Collectors.toList() ) ) );
		}
		
		// Register PlaceholderAPI with "emotes" namespace
		if ( Bukkit.getPluginManager().getPlugin( "PlaceholderAPI" ) != null ) {
			new BondrewdExpansion( this ).register();
		} else {
        	getLogger().info( "PlaceholderAPI not detected!" );
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
					for ( Emote emote : emotes ) {
						if ( sender.hasPermission( "bondrewdemotes.emote." + emote.getId() ) ) {
							builder.append( ":" );
							builder.append( emote.getId() );
							builder.append( ": " );
							
							found = true;
						}
					}
					if ( found ) {
						sender.sendMessage( "Found emotes..." );
						sender.sendMessage( builder.toString().trim() );
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
		Map< String, FontIndex > fonts = new HashMap< String, FontIndex >();
		
		int c = STARTING_CHAR;
		List< String > emoteList = config.getStringList( "emotes" );
		for ( String emote : emoteList ) {
			Emote parsedEmote = parseEmoteFrom( emote );
			
			String font = parsedEmote.getFont();
			FontIndex index = fonts.get( font );
			if ( index == null ) {
				index = new FontIndex();
				fonts.put( font, index );
			}
			
			parsedEmote.setChar( ( char ) c );
			emotes.add( parsedEmote );
			
			FontBitmap provider = new FontBitmap( new NamespacedKey( parsedEmote.getNamespace() ), new String[] { String.valueOf( ( char ) c ) } );
			provider.setHeight( parsedEmote.getHeight() );
			provider.setAscent( parsedEmote.getAscent() );
			index.addProvider( provider );

			c++;
		};
		
		for ( Entry< String, FontIndex > entry : fonts.entrySet() ) {
			File fontFile = new File( getDataFolder() + "/fonts/" + entry.getKey() + ".json" );
			FontIndex index = entry.getValue();
			
			if ( fontFile.exists() ) {
				fontFile.delete();
			}
			fontFile.getParentFile().mkdirs();
			
			try {
				OutputStreamWriter writer = new OutputStreamWriter( new FileOutputStream( fontFile ), StandardCharsets.UTF_8 );
				GSON.toJson( index.toJsonObject(), writer );
				writer.close();
			} catch ( IOException e ) {
				getLogger().severe( String.format( "Unable to save font file '%s'", entry.getKey() ) );
				e.printStackTrace();
			}
		}
	}
	
	private void loadPermissions() {
		PermissionBuilder admin = new PermissionBuilder( "bondrewdemotes.admin" ).setDefault( PermissionDefault.OP );
		// Right now the emote permissions don't do anything apart from showing up in the list of emotes.
		PermissionBuilder all = new PermissionBuilder( "bondrewdemotes.all" ).setDefault( PermissionDefault.OP );
		for ( Emote emote : emotes ) {
			all.addChild( new PermissionBuilder( "bondrewdemotes.emote." + emote.getId()).setDefault( PermissionDefault.OP ).register().build(), true );
		}

		admin.addChild( all.register().build(), true ).register();
		admin.addChild( new PermissionBuilder( "bondrewdemotes.reload" ).setDefault( PermissionDefault.OP ).register().build(), true );
		admin.addChild( new PermissionBuilder( "bondrewdemotes.list" ).setDefault( PermissionDefault.TRUE ).register().build(), true );
	}

	private String transformString( String string ) {
		for ( Emote emote : emotes ) {
			String search = ":" + emote.getId() + ":";
			string = string.replaceAll( "(?<!\\\\)" + search, String.valueOf( emote.getChar() ) );
			string = string.replace( "\\\\" + search, search );
		}
		return string;
	}
	
	private BaseComponent transformComponent( BaseComponent component ) {
		List< BaseComponent > subComponents = new LinkedList< BaseComponent >();
		if ( component instanceof TextComponent ) {
			TextComponent text = ( TextComponent ) component;
			TextComponent emptyCopy = text.duplicate();
			if ( emptyCopy.getExtra() != null ) {
				emptyCopy.getExtra().clear();
			}
			
			List< TextComponent > components = new LinkedList< TextComponent >();
			components.add( text );
			for ( Emote emote : emotes ) {
				List< TextComponent > temp = new LinkedList< TextComponent >();
				String key = String.format( EMOTE_FORMAT, emote.getId() );
				for ( TextComponent comp : components ) {
					String val = comp.getText();
					String[] split = val.split( "(?<!\\\\)" + key, -1 );
					if ( split.length > 1 ) {
						for ( int i = 0; i < split.length; i++ ) {
							String sub = split[ i ];
							if ( !sub.isEmpty() ) {
								TextComponent subText = emptyCopy.duplicate();
								subText.setText( sub );
								temp.add( subText );
							}
							
							if ( i < split.length - 1 ) {
								TextComponent emoteComp = new TextComponent( String.valueOf( emote.getChar() ) );
								for ( char c : emote.getFormatting().toCharArray() ) {
									switch ( c ) {
									case 'k':
									case 'K':
										emoteComp.setObfuscated( true );
										break;
									case 'l':
									case 'L':
										emoteComp.setBold( true );
										break;
									case 'm':
									case 'M':
										emoteComp.setStrikethrough( true );
										break;
									case 'n':
									case 'N':
										emoteComp.setUnderlined( true );
										break;
									case 'o':
									case 'O':
										emoteComp.setItalic( true );
										break;
									}
								}
								
								emoteComp.setColor( net.md_5.bungee.api.ChatColor.WHITE );
								emoteComp.setFont( emote.getFont() );
								
								if ( component.getHoverEvent() != null ) {
									emoteComp.setHoverEvent( component.getHoverEvent() );
								} else {
									TextComponent hoverComp = new TextComponent( "\\" + key );
									emoteComp.setHoverEvent( new HoverEvent( Action.SHOW_TEXT, new BaseComponent[] { hoverComp } ) );
								}
								
								if ( component.getClickEvent() != null ) {
									emoteComp.setClickEvent( component.getClickEvent() );
								}
								
								temp.add( emoteComp );
							}
						}
					} else {
						temp.add( comp );
					}
				}
				
				components = temp;
			}
			
			if ( !components.isEmpty() ) {
				component = components.remove( 0 );
				subComponents.addAll( components );
			}
		}
		
		if ( component.getExtra() != null ) {
			for ( BaseComponent comp : component.getExtra() ) {
				subComponents.add( transformComponent( comp ) );
			}
		}
		if ( !subComponents.isEmpty() ) {
			component.setExtra( subComponents );
		}
		
		return component;
	}
	
	private Emote parseEmoteFrom( String string ) {
		String[] split = string.split( "\\s+" );
		if ( split.length == 0 ) {
			return null;
		}
		String id = split[ 0 ];
		String[] fontSplit = id.split( "\\|.*?" );
		if ( fontSplit.length > 1 ) {
			id = fontSplit[ 1 ];
		}
		String[] formatSplit = id.split( "&" );
		if ( formatSplit.length > 1 ) {
			id = formatSplit[ 0 ];
		}
		
		Emote emote = new Emote( id );
		if ( fontSplit.length > 1 ) {
			emote.setFont( fontSplit[ 0 ] );
		} else {
			emote.setFont( "default" );
		}
		
		if ( formatSplit.length > 1 ) {
			emote.setFormatting( formatSplit[ 1 ] );
		} else {
			emote.setFormatting( "" );
		}
		
		if ( split.length > 1 ) {
			emote.setNamespace( split[ 1 ] );
		} else {
			emote.setNamespace( DEFAULT_NAMESPACE + emote.id + ".png" );
		}
		
		if ( split.length > 3 ) {
			emote.setHeightAndAscent( Integer.parseInt( split[ 2 ] ), Integer.parseInt( split[ 3 ] ) );
		} else {
			emote.setHeightAndAscent( EMOTE_HEIGHT, EMOTE_ASCENT );
		}
		
		return emote;
	}
	
	public String getEmoteFor( String string ) {
		for ( Emote emote : emotes ) {
			if ( string.equalsIgnoreCase( emote.getId() ) ) {
				return String.valueOf( emote.getChar() );
			}
		}
		return null;
	}

	public List< Emote > getEmotes() {
		return Collections.unmodifiableList( emotes );
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
