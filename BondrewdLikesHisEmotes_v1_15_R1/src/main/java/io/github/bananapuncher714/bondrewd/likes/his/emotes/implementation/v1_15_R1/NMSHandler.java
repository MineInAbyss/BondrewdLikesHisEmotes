package io.github.bananapuncher714.bondrewd.likes.his.emotes.implementation.v1_15_R1;

import java.io.IOException;
import java.util.NoSuchElementException;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.StringTransformer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.server.v1_15_R1.EnumProtocol;
import net.minecraft.server.v1_15_R1.EnumProtocolDirection;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NetworkManager;
import net.minecraft.server.v1_15_R1.Packet;
import net.minecraft.server.v1_15_R1.PacketDataSerializer;
import net.minecraft.server.v1_15_R1.PacketEncoder;
import net.minecraft.server.v1_15_R1.SkipEncodeException;

public class NMSHandler implements PacketHandler {
	private StringTransformer transformer;
	
	@Override
	public void inject( Channel channel ) {
		try {
			channel.pipeline().replace( PacketEncoder.class, "encoder", new CustomPacketEncoder() );
		} catch ( IllegalArgumentException | NoSuchElementException  e ) {
			// We might have replaced it already
			// Don't go around reloading the server without properly stopping/starting it though
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
