package io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class FontProviderFactory {
	public static FontProvider getProvider( String type, JsonObject object ) {
		if ( type.equalsIgnoreCase( "bitmap" ) ) {
			return getProviderBitmap( object );
		} else if ( type.equalsIgnoreCase( "ttf" ) ) {
			return getProviderTTF( object );
		} else if ( type.equalsIgnoreCase( "legacy_unicode" ) ) {
			return getProviderLegacy( object );
		} else {
			return null;
		}
	}
	
	private static FontBitmap getProviderBitmap( JsonObject object ) {
		NamespacedKey file = NamespacedKey.fromString( object.get( "file" ).getAsString() );
		
		int ascent = object.has( "ascent" ) ? object.get( "ascent" ).getAsInt() : 0;		
		int height = object.has( "height" ) ? object.get( "height" ).getAsInt() : 256;

		JsonArray charArr = object.get( "chars" ).getAsJsonArray();
		String[] arr = new String[ charArr.size() ];
		int i = 0;
		for ( JsonElement element : charArr ) {
			arr[ i++ ] = element.getAsString();
		}
		
		FontBitmap providerBitmap = new FontBitmap( file, arr );
		providerBitmap.setAscent( ascent );
		providerBitmap.setHeight( height );
		
		return providerBitmap;
	}
	
	private static FontTTF getProviderTTF( JsonObject object ) {
		NamespacedKey file = NamespacedKey.fromString( object.get( "file" ).getAsString() );
		
		int size = object.has( "size" ) ? object.get( "size" ).getAsInt() : 11;
		double oversample = object.has( "oversample" ) ? object.get( "oversample" ).getAsDouble() : 1;
		
		double shift1 = 0;
		double shift2 = 0;
		if ( object.has( "shift" ) ) {
			JsonArray shift = object.get( "shift" ).getAsJsonArray();
			shift1 = shift.get( 0 ).getAsDouble();
			shift2 = shift.get( 1 ).getAsDouble();
		}
		
		FontTTF provider = new FontTTF( file );
		provider.setOversample( oversample );
		provider.setSize( size );
		provider.setShift( shift1, shift2 );

		return provider;
	}
	
	private static FontLegacy getProviderLegacy( JsonObject object ) {
		NamespacedKey sizes = NamespacedKey.fromString( object.get( "sizes" ).getAsString() );
		NamespacedKey template = NamespacedKey.fromString( object.get( "template" ).getAsString() );
		
		return new FontLegacy( sizes, template );
	}
}
