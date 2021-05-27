package io.github.bananapuncher714.bondrewd.likes.his.emotes.implementation.v1_16_R1;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.BondrewdLikesHisEmotes;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.ComponentTransformer;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_16_R1.EnumProtocol;
import net.minecraft.server.v1_16_R1.EnumProtocolDirection;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import net.minecraft.server.v1_16_R1.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_16_R1.MinecraftServer;
import net.minecraft.server.v1_16_R1.NBTBase;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.NBTTagList;
import net.minecraft.server.v1_16_R1.NBTTagString;
import net.minecraft.server.v1_16_R1.NetworkManager;
import net.minecraft.server.v1_16_R1.Packet;
import net.minecraft.server.v1_16_R1.PacketDataSerializer;
import net.minecraft.server.v1_16_R1.PacketDecoder;
import net.minecraft.server.v1_16_R1.PacketEncoder;
import net.minecraft.server.v1_16_R1.PlayerConnection;
import net.minecraft.server.v1_16_R1.ServerConnection;
import net.minecraft.server.v1_16_R1.SkipEncodeException;

public class NMSHandler implements PacketHandler {
	private Map< Channel, ChannelHandler > encoder = new ConcurrentHashMap< Channel, ChannelHandler >();
	private Map< Channel, ChannelHandler > decoder = new ConcurrentHashMap< Channel, ChannelHandler >();
	private Map< Channel, Player > playerMap = Collections.synchronizedMap( new WeakHashMap< Channel, Player >() );
	private ComponentTransformer transformer;
	private JsonParser parser = new JsonParser();
	
	public NMSHandler() {
		// Replacement for TinyProtocotocol, despite copying its channel initializers and whatnot
		List< NetworkManager > networkManagers;
		List< ChannelFuture > channelFutures;

		try {
			Field networkManagerField = ServerConnection.class.getDeclaredField( "connectedChannels" );
			networkManagerField.setAccessible( true );
			Field channelFutureField = ServerConnection.class.getDeclaredField( "listeningChannels" );
			channelFutureField.setAccessible( true );

			networkManagers = ( List< NetworkManager > ) networkManagerField.get( MinecraftServer.getServer().getServerConnection() );
			channelFutures = ( List< ChannelFuture > ) channelFutureField.get( MinecraftServer.getServer().getServerConnection() );
		} catch ( NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1 ) {
			networkManagers = new ArrayList< NetworkManager >();
			channelFutures = new ArrayList< ChannelFuture >();
			e1.printStackTrace();
		}

		final List< NetworkManager > managers = networkManagers;
		final List< ChannelFuture > futures = channelFutures;

		// Handle connected channels
		ChannelInitializer< Channel > endInitProtocol = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel( Channel channel ) throws Exception {
				try {
					// This can take a while, so we need to stop the main thread from interfering
					synchronized ( managers ) {
						// Stop injecting channels
						channel.eventLoop().submit(() -> inject( channel ) );
					}
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		};

		// This is executed before Minecraft's channel handler
		ChannelInitializer< Channel > beginInitProtocol = new ChannelInitializer< Channel >() {
			@Override
			protected void initChannel( Channel channel ) throws Exception {
				channel.pipeline().addLast(endInitProtocol);
			}

		};

		ChannelInboundHandlerAdapter serverChannelHandler = new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				Channel channel = ( Channel ) msg;

				// Prepare to initialize ths channel
				channel.pipeline().addFirst(beginInitProtocol);
				ctx.fireChannelRead(msg);
			}
		};

		try {
			bind( futures, serverChannelHandler );
		} catch ( IllegalArgumentException ex ) {
			new BukkitRunnable() {
				@Override
				public void run() {
					bind( futures, serverChannelHandler );
				}
			}.runTask( JavaPlugin.getPlugin( BondrewdLikesHisEmotes.class ) );
		}
	}
	
	private void bind( List< ChannelFuture > channelFutures, ChannelInboundHandlerAdapter serverChannelHandler ) {
		for ( ChannelFuture future : channelFutures ) {
			future.channel().pipeline().addFirst( serverChannelHandler );
		}
		
		for ( Player player : Bukkit.getOnlinePlayers() ) {
			inject( player );
		}
	}

	@Override
	public void inject( Player player ) {
		PlayerConnection conn = ( ( CraftPlayer ) player ).getHandle().playerConnection;
		NetworkManager manager = conn.networkManager;
		Channel channel = manager.channel;
		
		
		if ( channel != null ) {
			playerMap.put( channel, player );
			
			inject( channel );
		}
	}

	@Override
	public void uninject( Player player ) {
		PlayerConnection conn = ( ( CraftPlayer ) player ).getHandle().playerConnection;
		NetworkManager manager = conn.networkManager;
		Channel channel = manager.channel;
		
		if ( channel != null ) {
			uninject( channel );
			
			playerMap.remove( channel );
		}
	}
	
	private void uninject( Channel channel ) {
		if ( encoder.containsKey( channel ) ) {
			// Replace our custom packet encoder with the default one that the player had
			ChannelHandler previousHandler = encoder.remove( channel );
			if ( previousHandler instanceof PacketEncoder ) {
				// PacketEncoder is not shareable, so we can't re-add it back. Instead, we'll have to create a new instance
				channel.pipeline().replace( CustomPacketEncoder.class, "encoder", new PacketEncoder( EnumProtocolDirection.CLIENTBOUND ) );
			} else {
				channel.pipeline().replace( CustomPacketEncoder.class, "encoder", previousHandler );
			}
		}
		
		if ( decoder.containsKey( channel ) ) {
			ChannelHandler previousHandler = decoder.remove( channel );
			if ( previousHandler instanceof PacketDecoder ) {
				channel.pipeline().replace( CustomPacketDecoder.class, "decoder", new PacketDecoder( EnumProtocolDirection.SERVERBOUND ) );
			} else {
				channel.pipeline().replace( CustomPacketDecoder.class, "decoder", previousHandler );
			}
		}
	}
	
	private void inject( Channel channel ) {
		if ( !encoder.containsKey( channel ) ) {
			// Replace the vanilla PacketEncoder with our own
			encoder.put( channel, channel.pipeline().replace( "encoder", "encoder", new CustomPacketEncoder( channel ) ) );
		}
		
		if ( !decoder.containsKey( channel ) ) {
			// Replace the vanilla PacketDecoder with our own
			decoder.put( channel, channel.pipeline().replace( "decoder", "decoder", new CustomPacketDecoder( channel ) ) );
		}
	}

	@Override
	public void setTransformer( ComponentTransformer transformer ) {
		this.transformer = transformer;
	}
	
	@Override
	public ComponentTransformer getTransformer() {
		return transformer;
	}

	private class CustomDataSerializer extends PacketDataSerializer {
		private Supplier< Player > supplier;
		
		public CustomDataSerializer( Supplier< Player > supplier, ByteBuf bytebuf ) {
			super( bytebuf );
			
			this.supplier = supplier;
		}

		@Override
		public PacketDataSerializer a( IChatBaseComponent component ) {
			JsonElement element = ChatSerializer.b( component );
			BaseComponent[] components = ComponentSerializer.parse( element.toString() );
			for ( int i = 0; i < components.length; i++ ) {
				components[ i ] = transformer.transform( components[ i ] );
			}
			String json = ComponentSerializer.toString( components );
			return super.a( ChatSerializer.a( json ) );
		}
		
		@Override
		public PacketDataSerializer a( NBTTagCompound compound ) {
			if ( transformer != null && compound != null ) {
				transform( compound, val -> {
					try {
						JsonElement element = parser.parse( val );
						if ( element.isJsonObject() ) {
							JsonObject obj = element.getAsJsonObject();
							
							if ( obj.has( "args" ) || obj.has( "text" ) || obj.has( "extra" ) ) {
								BaseComponent[] components = ComponentSerializer.parse( element.toString() );
								for ( int i = 0; i < components.length; i++ ) {
									components[ i ] = transformer.transform( components[ i ] );
								}
								
								return ComponentSerializer.toString( components );
							}
						}
					} catch ( Exception e ) {
					}
					return val;
				} );
			}
			
			return super.a( compound );
		}
		
		private void transform( NBTTagCompound compound, Function< String, String > transformer ) {
			for ( String key : compound.getKeys() ) {
				NBTBase base = compound.get( key );
				if ( base instanceof NBTTagCompound ) {
					transform( ( NBTTagCompound ) base, transformer );
				} else if ( base instanceof NBTTagList ) {
					transform( ( NBTTagList ) base, transformer );
				} else if ( base instanceof NBTTagString ) {
					compound.set( key, NBTTagString.a( transformer.apply( ( ( NBTTagString ) base ).asString() ) ) );
				}
			}
		}
		
		private void transform( NBTTagList list, Function< String, String > transformer ) {
			List< NBTBase > objects = new ArrayList< NBTBase >( list );
			for ( NBTBase base : objects ) {
				if ( base instanceof NBTTagCompound ) {
					transform( ( NBTTagCompound ) base, transformer );
				} else if ( base instanceof NBTTagList ) {
					transform( ( NBTTagList ) base, transformer );
				} else if ( base instanceof NBTTagString ) {
					String val = ( ( NBTTagString ) base ).asString();
					list.remove( base );
					list.add( NBTTagString.a( transformer.apply( val ) ) );
				}
			}
		}
		
		@Override
		public String e( int i ) {
			String val = super.e( i );
			
			if ( val != null ) {
				Player player = supplier.get();
				if ( player != null ) {
					val = transformer.verifyFor( player, val );
				}
			}
			
			return val;
		}
		
		@Override
		public NBTTagCompound l() {
			NBTTagCompound compound = super.l();
			
			if ( compound != null ) {
				Player player = supplier.get();
				if ( player != null ) {
//					transform( compound, val -> transformer.verifyFor( player, val ) );
				}
			}
			
			return compound;
		}
	}

	private class CustomPacketEncoder extends MessageToByteEncoder< Packet< ? > > {
		private EnumProtocolDirection protocolDirection = EnumProtocolDirection.CLIENTBOUND;
		private Channel channel;
		
		protected CustomPacketEncoder( Channel channel ) {
			this.channel = channel;
		}
		
		@Override
		protected void encode( ChannelHandlerContext var0, Packet< ? > var1, ByteBuf var2 ) throws Exception {
			EnumProtocol var3 = ( EnumProtocol ) var0.channel().attr( NetworkManager.c ).get();
			if (var3 == null) {
				throw new RuntimeException("ConnectionProtocol unknown: " + var1);
			}
			Integer var4 = var3.a( this.protocolDirection, var1 );

			if ( var4 == null ) {
				throw new IOException( "Can't serialize unregistered packet" );
			}

			PacketDataSerializer var5 = new CustomDataSerializer( () -> playerMap.get( channel ), var2 );
			var5.d( var4.intValue() );

			try {
				var1.b(var5);
			} catch ( Exception var6 ) {
				// Throw an error or something?
				//				LOGGER.error( var6 );
				if ( var1.a() ) {
					throw new SkipEncodeException( var6 );
				}
				throw var6;
			} 
		}
	}
	
	private class CustomPacketDecoder extends ByteToMessageDecoder {
		private EnumProtocolDirection protocolDirection = EnumProtocolDirection.SERVERBOUND;
		private Channel channel;
		
		protected CustomPacketDecoder( Channel channel ) {
			this.channel = channel;
		}
		
		@Override
		protected void decode( ChannelHandlerContext var0, ByteBuf var1, List< Object > var2 ) throws Exception {
			if ( var1.readableBytes() == 0 ) {
				return;
			}

			PacketDataSerializer var3 = new CustomDataSerializer( () -> playerMap.get( channel ), var1 );
			int var4 = var3.i();
			Packet< ? > var5 = ( ( EnumProtocol ) var0.channel().attr( NetworkManager.c ).get() ).a( this.protocolDirection, var4 );

			if (var5 == null) {
				throw new IOException("Bad packet id " + var4);
			}

			var5.a( var3 );
			if ( var3.readableBytes() > 0 ) {
				throw new IOException( "Packet " + ( ( EnumProtocol )var0.channel().attr(NetworkManager.c).get()).a() + "/" + var4 + " (" + var5.getClass().getSimpleName() + ") was larger than I expected, found " + var3.readableBytes() + " bytes extra whilst reading packet " + var4 );
			}
			var2.add(var5);
		}
	}
}
