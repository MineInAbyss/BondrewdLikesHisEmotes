package io.github.bananapuncher714.bondrewd.likes.his.emotes.implementation.v1_19_R2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.BondrewdLikesHisEmotes;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.ComponentTransformer;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.*;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

public class NMSHandler implements PacketHandler {
	private final Map< Channel, ChannelHandler > encoder = Collections.synchronizedMap(new WeakHashMap<>() );
	private final Map< Channel, ChannelHandler > decoder = Collections.synchronizedMap(new WeakHashMap<>() );
	private ComponentTransformer transformer;
	private final JsonParser parser = new JsonParser();
	
	public NMSHandler() {
		// Replacement for TinyProtocotocol, despite copying its channel initializers and whatnot
		List< NetworkManager > networkManagers;
		List< ChannelFuture > channelFutures;

		try {
			Field networkManagerField = ServerConnection.class.getDeclaredField( "g" );
			networkManagerField.setAccessible( true );
			Field channelFutureField = ServerConnection.class.getDeclaredField( "f" );
			channelFutureField.setAccessible( true );

			networkManagers = ( List< NetworkManager > ) networkManagerField.get( MinecraftServer.getServer().ad() );
			channelFutures = ( List< ChannelFuture > ) channelFutureField.get( MinecraftServer.getServer().ad() );
		} catch ( NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1 ) {
			networkManagers = new ArrayList<>();
			channelFutures = new ArrayList<>();
			e1.printStackTrace();
		}

		final List< NetworkManager > managers = networkManagers;
		final List< ChannelFuture > futures = channelFutures;

		// Handle connected channels
		ChannelInitializer<Channel> endInitProtocol = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel( Channel channel ) {
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
		ChannelInitializer<Channel> beginInitProtocol = new ChannelInitializer< Channel >() {
			@Override
			protected void initChannel( Channel channel ) throws Exception {
				ChannelHandler handler = null;
				for ( Entry< String, ChannelHandler > entry : channel.pipeline() ) {
					if ( entry.getValue().getClass().getName().equals( "com.viaversion.viaversion.bukkit.handlers.BukkitChannelInitializer" ) ) {
						handler = entry.getValue();
					}
				}
				
				if ( handler == null ) {
					channel.pipeline().addLast( endInitProtocol );
				} else {
					// Urrrgh Viaversion...
					Class< ? > clazz = handler.getClass();
					Method initChannel = ChannelInitializer.class.getDeclaredMethod( "initChannel", Channel.class );
					initChannel.setAccessible( true );
					Field original = clazz.getDeclaredField( "original" );
					original.setAccessible( true );
					ChannelInitializer< Channel > initializer = ( ChannelInitializer< Channel > ) original.get( handler );
					ChannelInitializer< Channel > miniInit = new ChannelInitializer< Channel >() {
						@Override
						protected void initChannel( Channel ch ) throws Exception {
							initChannel.invoke( initializer, ch );
							
							inject( ch );
						}
					};
					original.set( handler, miniInit );
				}
			}

		};

		ChannelInboundHandlerAdapter serverChannelHandler = new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead( ChannelHandlerContext ctx, Object msg ) {
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
		PlayerConnection conn = ( (CraftPlayer) player ).getHandle().b;
		NetworkManager manager = conn.b;
		Channel channel = manager.m;
		
		if ( channel != null ) {
			inject( channel );
			
			for ( Entry< String, ChannelHandler > entry : channel.pipeline() ) {
				ChannelHandler handler = entry.getValue();
				if ( handler instanceof CustomPacketEncoder ) {
					( ( CustomPacketEncoder ) handler ).setPlayer( player );
				} else if ( handler instanceof CustomPacketDecoder ) {
					( ( CustomPacketDecoder ) handler ).setPlayer( player );
				}
			}
		}
	}

	@Override
	public void uninject( Player player ) {
		PlayerConnection conn = ( ( CraftPlayer ) player ).getHandle().b;
		NetworkManager manager = conn.b;
		Channel channel = manager.m;
		
		if ( channel != null ) {
			uninject( channel );
		}
	}
	
	private void uninject( Channel channel ) {
		if ( encoder.containsKey( channel ) ) {
			// Replace our custom packet encoder with the default one that the player had
			ChannelHandler previousHandler = encoder.remove( channel );
			if ( previousHandler instanceof PacketEncoder ) {
				// PacketEncoder is not shareable, so we can't re-add it back. Instead, we'll have to create a new instance
				channel.pipeline().replace( "encoder", "encoder", new PacketEncoder( EnumProtocolDirection.b ) );
			} else {
				channel.pipeline().replace( "encoder", "encoder", previousHandler );
			}
		}
		
		if ( decoder.containsKey( channel ) ) {
			ChannelHandler previousHandler = decoder.remove( channel );
			if ( previousHandler instanceof PacketDecoder ) {
				channel.pipeline().replace( "decoder", "decoder", new PacketDecoder( EnumProtocolDirection.a ) );
			} else {
				channel.pipeline().replace( "decoder", "decoder", previousHandler );
			}
		}
	}
	
	private void inject( Channel channel ) {
		if ( !encoder.containsKey( channel ) ) {
			// Replace the vanilla PacketEncoder with our own
			ChannelHandler handler = channel.pipeline().get( "encoder" );
			if ( !( handler instanceof CustomPacketEncoder ) ) {
				encoder.put( channel, channel.pipeline().replace( "encoder", "encoder", new CustomPacketEncoder() ) );
			}
		}
		
		if ( !decoder.containsKey( channel ) ) {
			// Replace the vanilla PacketDecoder with our own
			ChannelHandler handler = channel.pipeline().get( "decoder" );
			if ( !( handler instanceof CustomPacketDecoder ) ) {
				decoder.put( channel, channel.pipeline().replace( "decoder", "decoder", new CustomPacketDecoder() ) );
			}
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
		private final Supplier< Player > supplier;
		
		public CustomDataSerializer( Supplier< Player > supplier, ByteBuf bytebuf ) {
			super( bytebuf );
			
			this.supplier = supplier;
		}
		
		/*@Override
		public PacketDataSerializer a( IChatBaseComponent component ) {
			JsonElement element = ChatSerializer.b( component );
			BaseComponent[] components = ComponentSerializer.parse( element.toString() );
			for ( int i = 0; i < components.length; i++ ) {
				components[ i ] = transformer.transform( components[ i ] );
			}
			String json = ComponentSerializer.toString( components );
			return super.a( ChatSerializer.a( json ) );
		}*/
		
		@Override
		public PacketDataSerializer a( String string, int len ) {
			if ( transformer != null ) {
				try {
					JsonElement element = parser.parse( string );
					if ( element.isJsonObject() ) {
						JsonObject obj = element.getAsJsonObject();

						if ( obj.has( "args" ) || obj.has( "text" ) || obj.has( "extra" ) || obj.has( "translate" ) ) {
							BaseComponent[] components = ComponentSerializer.parse( element.toString() );
							for ( int i = 0; i < components.length; i++ ) {
								components[ i ] = transformer.transform( components[ i ] );
							}
							String json = ComponentSerializer.toString( components );
							return super.a( json, len );
						}
					}
				} catch ( Exception e ) {
				}
			}
			
			return super.a( string, len );
		}
		
		@Override
		public PacketDataSerializer a( NBTTagCompound compound ) {
			if ( transformer != null && compound != null ) {
				transform( compound, val -> {
					try {
						JsonElement element = parser.parse( val );
						if ( element.isJsonObject() ) {
							JsonObject obj = element.getAsJsonObject();
							
							if ( obj.has( "args" ) || obj.has( "text" ) || obj.has( "extra" ) || obj.has( "translate" ) ) {
								BaseComponent[] components = ComponentSerializer.parse( element.toString() );
								for ( int i = 0; i < components.length; i++ ) {
									components[ i ] = transformer.transform( components[ i ] );
								}
								
								return ComponentSerializer.toString( components );
							}
						}
					} catch ( Exception ignored) {
					}
					return val;
				} );
			}
			
			return super.a( compound );
		}
		
		private void transform( NBTTagCompound compound, Function< String, String > transformer ) {
			for ( String key : compound.e() ) {
				NBTBase base = compound.c( key );
				if ( base instanceof NBTTagCompound ) {
					transform( ( NBTTagCompound ) base, transformer );
				} else if ( base instanceof NBTTagList ) {
					transform( ( NBTTagList ) base, transformer );
				} else if ( base instanceof NBTTagString ) {
					compound.a( key, NBTTagString.a( transformer.apply( ( ( NBTTagString ) base ).f_() ) ) );
				}
			}
		}
		
		private void transform( NBTTagList list, Function< String, String > transformer ) {
			List< NBTBase > objects = new ArrayList<>(list);
			for ( NBTBase base : objects ) {
				if ( base instanceof NBTTagCompound ) {
					transform( ( NBTTagCompound ) base, transformer );
				} else if ( base instanceof NBTTagList ) {
					transform( ( NBTTagList ) base, transformer );
				} else if ( base instanceof NBTTagString ) {
					int index = list.indexOf(base);
					list.remove(base);
					list.add(index, NBTTagString.a(transformer.apply(base.f_())));
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
		
//		@Override
//		public NBTTagCompound a( NBTReadLimiter limiter ) {
//			NBTTagCompound compound = super.a( limiter );
//			
//			if ( compound != null ) {
//				Player player = supplier.get();
//				if ( player != null ) {
//					transform( compound, val -> transformer.verifyFor( player, val ) );
//				}
//			}
//			
//			return compound;
//		}
	}

	private class CustomPacketEncoder extends MessageToByteEncoder< Packet< ? > > {
		private final EnumProtocolDirection protocolDirection = EnumProtocolDirection.b;
		private Player player;
		
		@Override
		protected void encode( ChannelHandlerContext var0, Packet< ? > var1, ByteBuf var2 ) throws Exception {
			EnumProtocol var3 = ( EnumProtocol ) var0.channel().attr( NetworkManager.e ).get();
			if (var3 == null) {
				throw new RuntimeException("ConnectionProtocol unknown: " + var1);
			}
			Integer var4 = var3.a( this.protocolDirection, var1 );

			if ( var4 == null ) {
				throw new IOException( "Can't serialize unregistered packet" );
			}

			PacketDataSerializer var5 = new CustomDataSerializer( () -> player, var2 );
			var5.d(var4);

			try {
				int var6 = var5.writerIndex();
				var1.a( var5 );
				int var7 = var5.writerIndex() - var6;
				if ( var7 > 8388608 ) {
					throw new IllegalArgumentException("Packet too big (is " + var7 + ", should be less than 8388608): " + var1);
				}
			} catch ( Exception var6 ) {
				// Throw an error or something?
				//				LOGGER.error( var6 );
				if ( var1.a() ) {
					throw new SkipEncodeException( var6 );
				}
				throw var6;
			}
		}
		
		protected void setPlayer( Player player ) {
			this.player = player;
		}
	}
	
	private class CustomPacketDecoder extends ByteToMessageDecoder {
		private final EnumProtocolDirection protocolDirection = EnumProtocolDirection.a;
		private Player player;
		
		@Override
		protected void decode( ChannelHandlerContext var0, ByteBuf var1, List< Object > var2 ) throws Exception {
			if ( var1.readableBytes() == 0 ) {
				return;
			}
			
			PacketDataSerializer var3 = new CustomDataSerializer( () -> player, var1 );
			int var4 = var3.k();
			Packet< ? > var5 = var0.channel().attr( NetworkManager.e ).get().a( this.protocolDirection, var4, var3 );

			if (var5 == null) {
				throw new IOException("Bad packet id " + var4);
			}
			
			if ( var3.readableBytes() > 0 ) {
				throw new IOException( "Packet " + ( ( EnumProtocol )var0.channel().attr(NetworkManager.e).get()).a() + "/" + var4 + " (" + var5.getClass().getSimpleName() + ") was larger than I expected, found " + var3.readableBytes() + " bytes extra whilst reading packet " + var4 );
			}
			var2.add(var5);
		}
		
		protected void setPlayer( Player player ) {
			this.player = player;
		}
	}
}
