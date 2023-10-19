package net.mcpp.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
/**
 * Hash map implementation of MultiMap (using a HashSet for values)
 * @author jbarb_t8a3esk
 *
 * @param <K>
 * @param <V>
 */
public class MultiHashMap<K,V> extends MultiMap<K,V> {
	public MultiHashMap() {
		super(new HashMap<K,Collection<V>>());
	}
	@Override
	protected Collection<V> newCollection() {
		return new HashSet<V>();
	}

}
