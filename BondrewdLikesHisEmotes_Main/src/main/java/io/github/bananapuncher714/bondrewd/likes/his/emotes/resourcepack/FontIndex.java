package io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FontIndex {
	protected List< FontProvider > providers = new ArrayList< FontProvider >();

	public FontIndex() {}
	
	public FontIndex( JsonObject object ) {
		JsonArray arr = object.get( "providers" ).getAsJsonArray();
		for ( JsonElement element : arr ) {
			JsonObject providerObject = element.getAsJsonObject();
			String type = providerObject.get( "type" ).getAsString();
			FontProvider provider = FontProviderFactory.getProvider( type, providerObject );
			if ( provider != null ) {
				addProvider( provider );
			}
		}
	}
	
	public void addProvider( FontProvider provider ) {
		providers.add( provider );
	}

	public void remove( FontProvider provider ) {
		providers.remove( provider );
	}
	
	public List< FontProvider > getProviders() {
		return new ArrayList< FontProvider >( providers );
	}
	
	public JsonObject toJsonObject() {
		JsonObject object = new JsonObject();
		JsonArray pr = new JsonArray();
		object.add( "providers", pr );
		
		for ( FontProvider provider : providers ) {
			pr.add( provider.toJsonObject() );
		}
		
		return object;
	}
}
