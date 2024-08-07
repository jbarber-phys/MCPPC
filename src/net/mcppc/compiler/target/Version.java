package net.mcppc.compiler.target;

import java.util.Objects;

import net.mcpp.util.Ordered;

/**
 * represents a version of minecraft, for command purposes;
 * same as a pack_format / pack_version;
 * @author RadiumE13
 *
 */
//TODO verify that sci notation won't break 1.20.2 in nbt
public class Version  implements Ordered<Version>{
	public static final Version JAVA_1_18_2 = new Version(9); // approx initial commit
	public static final Version JAVA_1_19 = new Version(10);
	public static final Version JAVA_1_20 = new Version(15); 
	public static final Version JAVA_1_20_2_SNAP_RETURN_RUN = new Version(16); 
	public static final Version JAVA_1_20_2 = new Version(18); 
	public static final Version JAVA_1_20_3_SNAP = new Version(25); 
	public static final Version JAVA_1_20_3 = new Version(26); 
	/*
	 * 43: Renamed legacy tag folders like tags/items to tags/item, with the exception of tags/functions.
	 */
	public static final Version JAVA_1_21_SNAP_PARTICLE_SNBT = new Version(39); 
	/*
	 * 43: Renamed legacy tag folders like tags/items to tags/item, with the exception of tags/functions.
	 */
	public static final Version JAVA_1_21_SNAP_SOME_SINGULAR = new Version(43); 
	/*
	 * 45: Added data driven jukebox songs. Renamed legacy folders like loot_tables to loot_table
	 *     or tags/functions to tags/function. 
	 */
	public static final Version JAVA_1_21_SNAP_ALL_SINGULAR = new Version(45); 
	public static final Version JAVA_1_21 = new Version(48); 
	
	
	//internal only
	static final Version JAVA_INFINITY = new Version(Integer.MAX_VALUE);
	static final Version JAVA_NEFINITY = new Version(Integer.MIN_VALUE);
	public final int packFormat;
	public static final String edition = "java";//
	public Version(int packFormat) {
		this.packFormat = packFormat;
	}
	public VTarget justMe() {
		return new VTarget(this,this);
	}

	@Override
	public int compareTo(Version o) {
		return Integer.compare(this.packFormat, o.packFormat);
	}
	@SuppressWarnings("static-access")
	@Override public int hashCode() {
		return Objects.hash(this.packFormat,this.edition);
	}
	@Override public boolean equals(Object obj) {
		if(!(obj instanceof Version))return false;
		return (this.packFormat == ((Version) obj).packFormat);
	}
	@Override protected Object clone() throws CloneNotSupportedException {
		return new Version(this.packFormat);
	}
	@Override
	public String toString() {
		return "%d".formatted(this.packFormat);
	}
	
}
