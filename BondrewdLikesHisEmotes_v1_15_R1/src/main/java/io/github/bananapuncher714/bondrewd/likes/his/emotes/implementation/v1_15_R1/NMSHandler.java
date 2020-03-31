package io.github.bananapuncher714.bondrewd.likes.his.emotes.implementation.v1_15_R1;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.StringTransformer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.server.v1_15_R1.EnumProtocol;
import net.minecraft.server.v1_15_R1.EnumProtocolDirection;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NetworkManager;
import net.minecraft.server.v1_15_R1.Packet;
import net.minecraft.server.v1_15_R1.PacketDataSerializer;
import net.minecraft.server.v1_15_R1.PacketEncoder;
import net.minecraft.server.v1_15_R1.PlayerConnection;
import net.minecraft.server.v1_15_R1.SkipEncodeException;

public class NMSHandler implements PacketHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker b = MarkerManager.getMarker( "PACKET_SENT", NetworkManager.b );
	
	private Map< UUID, PacketEncoder > handlers = new HashMap< UUID, PacketEncoder >();
	private StringTransformer transformer;
	
	@Override
	public void uninject( Player player ) {
		PacketEncoder encoder = handlers.remove( player.getUniqueId() );
		if ( encoder != null ) {
			PlayerConnection connection = ( ( CraftPlayer ) player ).getHandle().playerConnection;
			connection.networkManager.channel.pipeline().replace( CustomPacketEncoder.class, "encoder", encoder );
		}
	}

	@Override
	public void inject( Player player ) {
		PlayerConnection connection = ( ( CraftPlayer ) player ).getHandle().playerConnection;
		handlers.put( player.getUniqueId(), connection.networkManager.channel.pipeline().replace( PacketEncoder.class, "encoder", new CustomPacketEncoder() ) );
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
		public PacketDataSerializer a( String s, int i ) {
			if ( transformer != null ) {
				s = transformer.transform( s );
			}
			return super.a( s, i);
		}

		@Override
		public PacketDataSerializer a( NBTTagCompound nbttagcompound ) {
			// TODO Maybe parse NBT compounds someday? This could be used in items and such.
			return super.a( nbttagcompound );
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


			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug( b, "OUT: [{}:{}] {}", var0.channel().attr( NetworkManager.c ).get(), var4, var1.getClass().getName() );
			}

			if ( var4 == null ) {
				throw new IOException( "Can't serialize unregistered packet" );
			}

			PacketDataSerializer var5 = new CustomDataSerializer( var2 );
			var5.d( var4.intValue() );

			try {
				var1.b(var5);
			} catch ( Exception var6 ) {
				LOGGER.error( var6 );
				if ( var1.a() ) {
					throw new SkipEncodeException( var6 );
				}
				throw var6;
			} 
		}
	}
}
