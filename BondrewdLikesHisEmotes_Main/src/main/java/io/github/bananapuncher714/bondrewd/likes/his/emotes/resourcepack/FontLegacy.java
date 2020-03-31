package io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack;

import com.google.gson.JsonObject;

public class FontLegacy extends FontProvider {
	protected NamespacedKey sizes;
	protected NamespacedKey template;
	
	public FontLegacy( NamespacedKey sizes, NamespacedKey template ) {
		super( "legacy_unicode" );
		this.sizes = sizes;
		this.template = template;
	}

	public NamespacedKey getSizes() {
		return sizes;
	}

	public NamespacedKey getTemplate() {
		return template;
	}
	
	public JsonObject toJsonObject() {
		JsonObject object = super.toJsonObject();
		object.addProperty( "sizes", sizes.toString() );
		object.addProperty( "template", template.toString() );
		
		return object;
	}
}
