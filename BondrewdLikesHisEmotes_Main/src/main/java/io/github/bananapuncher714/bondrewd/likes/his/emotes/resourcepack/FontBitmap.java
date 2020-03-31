package io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.bananapuncher714.bondrewd.likes.his.emotes.BondrewdLikesHisEmotes;

public class FontBitmap extends FontProvider {
	protected NamespacedKey file;
	protected String[] chars;
	protected int ascent = BondrewdLikesHisEmotes.EMOTE_HEIGHT;
	protected int height = BondrewdLikesHisEmotes.EMOTE_HEIGHT;
	
	public FontBitmap( NamespacedKey path, String[] chars ) {
		super( "bitmap" );
		file = path;
		this.chars = chars;
	}
	
	public NamespacedKey getFile() {
		return file;
	}

	public String[] getChars() {
		return chars;
	}

	public int getAscent() {
		return ascent;
	}

	public FontBitmap setAscent( int ascent ) {
		this.ascent = ascent;
		return this;
	}

	public int getHeight() {
		return height;
	}

	public FontBitmap setHeight( int height ) {
		this.height = height;
		return this;
	}
	
	public JsonObject toJsonObject() {
		JsonObject object = super.toJsonObject();
		object.addProperty( "file", file.toString() );
		object.addProperty( "ascent", ascent );
		object.addProperty( "height", height );
		JsonArray arr = new JsonArray();
		for ( String s : chars ) {
			arr.add( s );
		}
		object.add( "chars", arr );
		
		return object;
	}
}
