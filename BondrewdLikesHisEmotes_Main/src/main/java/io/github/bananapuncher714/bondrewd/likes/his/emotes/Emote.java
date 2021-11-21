package io.github.bananapuncher714.bondrewd.likes.his.emotes;

public class Emote {
	public final String id;
	String font = "";
	String formatting = "";
	String value = "";
	boolean gif = false;
	
	public Emote( String id, boolean gif ) {
		this.id = id;
		this.gif = gif;
	}
	
	public void setFont( String font ) {
		this.font = font;
	}

	public String getId() {
		return id;
	}

	public String getFont() {
		return font;
	}

	public void setFormatting( String formatting ) {
		this.formatting = formatting;
	}
	
	public String getFormatting() {
		return formatting;
	}
	
	public void setValue( String val ) {
		value = val;
	}
	
	public String getValue() {
		return value;
	}
	
	public boolean isGif() {
		return gif;
	}
}
