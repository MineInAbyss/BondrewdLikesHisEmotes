package io.github.bananapuncher714.bondrewd.likes.his.emotes.implementation.v1_16_R3;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.BondrewdLikesHisEmotes;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.StringTransformer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.server.v1_16_R3.EnumProtocol;
import net.minecraft.server.v1_16_R3.EnumProtocolDirection;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.NBTTagString;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketDataSerializer;
import net.minecraft.server.v1_16_R3.PacketEncoder;
import net.minecraft.server.v1_16_R3.ServerConnection;
import net.minecraft.server.v1_16_R3.SkipEncodeException;

public class NMSHandler implements PacketHandler {
	private Map< Channel, ChannelHandler > encoder = new ConcurrentHashMap< Channel, ChannelHandler >();
	private StringTransformer transformer;
	
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
		inject( ( ( CraftPlayer ) player ).getHandle().playerConnection.networkManager.channel );
	}

	@Override
	public void uninject( Player player ) {
		uninject( ( ( CraftPlayer ) player ).getHandle().playerConnection.networkManager.channel );
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
	}
	
	private void inject( Channel channel ) {
		if ( !encoder.containsKey( channel ) ) {
			// Replace the vanilla PacketEncoder with our own
			encoder.put( channel, channel.pipeline().replace( "encoder", "encoder", new CustomPacketEncoder() ) );
		}
	}

	@Override
	public StringTransformer getTransformer() {
		return transformer;
	}

	@Override
	public void setTransformer( StringTransformer transformer ) {
		this.transformer = transformer;
	}

	private class CustomDataSerializer extends PacketDataSerializer {
		public CustomDataSerializer( ByteBuf bytebuf ) {
			super( bytebuf );
		}

		@Override
		public PacketDataSerializer a( IChatBaseComponent ichatbasecomponent ) {
			// TODO Find some way to translate them?
			return super.a( ichatbasecomponent );
		}
		
		@Override
		public PacketDataSerializer a( String s, int i ) {
			if ( transformer != null ) {
				s = transformer.transform( s );
			}
			return super.a( s, i);
		}

		@Override
		public PacketDataSerializer a( NBTTagCompound compound ) {
			if ( transformer != null && compound != null ) {
				transform( compound );
			}
			
			return super.a( compound );
		}
		
		private void transform( NBTTagCompound compound ) {
			for ( String key : compound.getKeys() ) {
				NBTBase base = compound.get( key );
				if ( base instanceof NBTTagCompound ) {
					transform( ( NBTTagCompound ) base );
				} else if ( base instanceof NBTTagList ) {
					transform( ( NBTTagList ) base );
				} else if ( base instanceof NBTTagString ) {
					compound.set( key, NBTTagString.a( transformer.transform( ( ( NBTTagString ) base ).asString() ) ) );
				}
			}
		}
		
		private void transform( NBTTagList list ) {
			List< NBTBase > objects = new ArrayList< NBTBase >( list );
			for ( NBTBase base : objects ) {
				if ( base instanceof NBTTagCompound ) {
					transform( ( NBTTagCompound ) base );
				} else if ( base instanceof NBTTagList ) {
					transform( ( NBTTagList ) base );
				} else if ( base instanceof NBTTagString ) {
					list.remove( base );
					list.add( NBTTagString.a( transformer.transform( ( ( NBTTagString ) base ).asString() ) ) );
				}
			}
		}
	}

	private class CustomPacketEncoder extends MessageToByteEncoder< Packet< ? > > {
		private EnumProtocolDirection protocolDirection = EnumProtocolDirection.CLIENTBOUND;

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

			PacketDataSerializer var5 = new CustomDataSerializer( var2 );
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
}
