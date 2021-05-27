package io.github.bananapuncher714.bondrewd.likes.his.emotes;

public class Emote {
	public final String id;
	String font;
	String formatting;
	String namespace;
	int height;
	int ascent;
	char c;
	
	public Emote( String id ) {
		this.id = id;
	}
	
	public void setHeightAndAscent( int height, int ascent ) {
		this.height = height;
		this.ascent = ascent;
	}
	
	public void setFont( String font ) {
		this.font = font;
	}
	
	public void setFormatting( String formatting ) {
		this.formatting = formatting;
	}
	
	public void setNamespace( String namespace ) {
		this.namespace = namespace;
	}

	public String getId() {
		return id;
	}

	public String getFont() {
		return font;
	}

	public String getFormatting() {
		return formatting;
	}

	public String getNamespace() {
		return namespace;
	}

	public int getHeight() {
		return height;
	}

	public int getAscent() {
		return ascent;
	}

	public char getChar() {
		return c;
	}

	public void setChar( char c ) {
		this.c = c;
	}
}
