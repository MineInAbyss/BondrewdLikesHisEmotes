package io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack;

public class NamespacedKey {
	public final String namespace;
	public final String key;
	
	public NamespacedKey( String key ) {
		this( "minecraft", key );
	}
	
	public NamespacedKey( String namespace, String key ) {
		this.namespace = namespace.toLowerCase();
		this.key = key.toLowerCase();
	}
	
	public String toString() {
		return namespace + ":" + key;
	}
	
	public static NamespacedKey fromString( String string ) {
		String[] values = string.split( ":" );
		if ( values.length == 1 ) {
			return new NamespacedKey( string );
		} else {
			return new NamespacedKey( values[ 0 ], values[ 1 ] );
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NamespacedKey other = (NamespacedKey) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		return true;
	}
}
