package net.mcpp.util;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
/**
 * a double Tree implimentation of MultiMap (both K and V are ordered)
 * @author RadiumE13
 *
 * @param <K>
 * @param <V>
 */
public class MultiTreeMap<K extends Comparable<K>,V extends Comparable<V>> extends MultiMap<K, V> {

	public MultiTreeMap() {
		super(new TreeMap<K,Collection<V>>());
	}
	@Override
	protected Collection<V> newCollection() {
		return new TreeSet<V>();
	}

}
