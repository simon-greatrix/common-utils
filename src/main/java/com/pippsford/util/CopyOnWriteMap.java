package com.pippsford.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A Map which can be read concurrently and allows for updates. The map that is being read from is never updated directly, so read operations are thread-safe
 * and multi-threading, whilst write operations are single threaded.
 *
 * <p>Every write operation causes a complete new copy of the Map to be created, so write operations are expensive, but read operations are no more expensive
 * than for the backing map.
 *
 * <p>Note that this map implementation does not support mutation through its entry set, key set or values collection, nor the iterators upon them. If you need
 * this functionality, copy this map into a new one, mutate that and then use the replace(java.util.Map) method to update this map.
 *
 * <p>This Map is therefore recommended for use when the majority of operations will be reads.
 *
 * @param <K> the key type for this map
 * @param <V> the value type for this map
 *
 * @author Dr Simon Greatrix
 */
public class CopyOnWriteMap<K, V> implements ConcurrentMap<K, V> {

  /** Function to create new maps. The function takes accepts a suggested capacity (-1 implies use a default) and should return an appropriate Map instance. */
  protected final Function<Integer, Map<K, V>> mapCreator;

  /** The write lock. */
  private final Object writeLock = new Object();

  /** The backing map. */
  protected Map<K, V> map;


  /** Create a CopyOnWriteMap. */
  public CopyOnWriteMap() {
    this(suggestedCapacity -> {
      if (suggestedCapacity <= 0) {
        return new HashMap<>();
      }

      // The JavaDoc for HashMap specifies a default load factor of 0.75f.
      // The map is rehashed when size > (capacity * load-factor) so to
      // contain our specified number of entries we must have:
      // capacity > ( size / load-factor )
      return new HashMap<>((int) (1 + (suggestedCapacity / 0.75)), 0.75f);
    });
  }


  /**
   * Create a CopyOnWriteMap.
   *
   * @param mapCreator A supplier of new backing maps, which takes a predicted capacity and returns an empty mutable map instance.
   */
  public CopyOnWriteMap(Function<Integer, Map<K, V>> mapCreator) {
    this.mapCreator = mapCreator;
    map = mapCreator.apply(-1);
  }


  /**
   * Create a copy.
   *
   * @param original the original
   */
  public CopyOnWriteMap(CopyOnWriteMap<K, V> original) {
    map = original.map;
    mapCreator = original.mapCreator;
  }


  /**
   * Create an instance with the map and creator explicitly specified.
   *
   * @param initialMap the initial map
   * @param mapCreator the new map creator
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public CopyOnWriteMap(Map<K, V> initialMap, Function<Integer, Map<K, V>> mapCreator) {
    map = initialMap;
    this.mapCreator = mapCreator;
  }


  /**
   * Clear this map.
   *
   * @see Map#clear()
   */
  public void clear() {
    synchronized (getLock()) {
      // we do not need to create a copy just to clear it
      map = createNewMap(-1);
    }
  }


  /**
   * Does this map contain the specified key?.
   *
   * @param key the key to check
   *
   * @return true if this map contains a mapping for the key
   *
   * @see Map#containsValue(Object)
   */
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }


  /**
   * Does this map contain the specified value?.
   *
   * @param value the value to check for
   *
   * @return true if this map contains a mapping to the value
   *
   * @see Map#containsValue(Object)
   */
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }


  /**
   * Create a copy of this map.
   *
   * @return the copy
   */
  public Map<K, V> copy() {
    synchronized (getLock()) {
      Map<K, V> newMap = createNewMap(map.size());
      newMap.putAll(map);
      return newMap;
    }
  }


  /**
   * Create a copy of the backing map for update.
   *
   * @param size target capacity of new map
   *
   * @return copy of this map
   */
  protected Map<K, V> copyMap(int size) {
    Map<K, V> copy = createNewMap(size);
    copy.putAll(map);
    return copy;
  }


  /**
   * Create a new map. Override this method if you want to use a different backing map from a java.util.HashMap
   *
   * @param suggestedCapacity - the expected capacity required. Use {@literal <=0} to suggest a default.
   *
   * @return a new map of the correct type
   */
  protected Map<K, V> createNewMap(int suggestedCapacity) {
    return mapCreator.apply(suggestedCapacity);
  }


  /**
   * Get the entries in this map. The map cannot be updated through this set nor through iterators upon it.
   *
   * @return read-only set of all entries in this map
   *
   * @see Map#entrySet()
   */
  @Nonnull
  public Set<Entry<K, V>> entrySet() {
    return Collections.unmodifiableSet(map.entrySet());
  }


  @Override
  public boolean equals(Object o) {
    if (o instanceof Map) {
      return map.equals(o);
    }
    return false;
  }


  /**
   * Get the object mapped to the specified key.
   *
   * @param key the key to get the mapping for
   *
   * @return the value mapped to the given key, or null if not found.
   *
   * @see Map#get(Object)
   */
  public V get(Object key) {
    return map.get(key);
  }


  /**
   * Get the object which protects the map during writes. When synchronized on this object, no other thread will modify this map.
   *
   * @return the lock object
   */
  public Object getLock() {
    return writeLock;
  }


  @Override
  public int hashCode() {
    return map.hashCode();
  }


  /**
   * Is this map empty?.
   *
   * @return true if this map is empty
   *
   * @see Map#isEmpty()
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }


  /**
   * Get the set of all keys for this map.  The map cannot be updated through this set nor through iterators upon it.
   *
   * @return set of all keys in this map.
   *
   * @see Map#keySet()
   */
  @Nonnull
  public Set<K> keySet() {
    return Collections.unmodifiableSet(map.keySet());
  }


  /**
   * Put the given mapping into this map.
   *
   * @param key   the key for the mapping
   * @param value the value the key is mapped to
   *
   * @return the previous mapping for this key, or null if none
   *
   * @see Map#put(Object, Object)
   */
  public V put(K key, V value) {
    synchronized (getLock()) {
      Map<K, V> copy = copyMap(map.size());
      V obj = copy.put(key, value);
      map = copy;
      return obj;
    }
  }


  /**
   * Put all the mappings in the specified map into this map.
   *
   * @param t the map to copy into this
   *
   * @see Map#putAll(Map)
   */
  public void putAll(@Nullable Map<? extends K, ? extends V> t) {
    if (t == null || t.isEmpty()) {
      return;
    }
    synchronized (getLock()) {
      Map<K, V> copy = copyMap(map.size() + t.size());
      copy.putAll(t);
      map = copy;
    }
  }


  /**
   * If the specified key is not already associated with a value, associate it with the given value.
   *
   * @param key   key with which the specified value is to be associated.
   * @param value value to be associated with the specified key.
   *
   * @return previous value associated with specified key, or null if there was no mapping for key. A null return can also indicate that the map previously
   *     associated null with the specified key, if the implementation supports null values.
   *
   * @see ConcurrentMap#putIfAbsent(Object, Object)
   */
  public V putIfAbsent(@Nullable K key, V value) {
    synchronized (getLock()) {
      if (!map.containsKey(key)) {
        Map<K, V> copy = copyMap(map.size() + 1);
        V ret = copy.put(key, value);
        map = copy;

        return ret;
      }

      return get(key);
    }
  }


  /**
   * Remove a mapping from the map.
   *
   * @param key the key for the mapping to remove
   *
   * @return the value the key was mapped to, or null if none
   *
   * @see Map#remove(Object)
   */
  public V remove(Object key) {
    synchronized (getLock()) {
      Map<K, V> copy = copyMap(map.size());
      V obj = copy.remove(key);
      map = copy;
      return obj;
    }
  }


  /**
   * Remove entry for key only if currently mapped to given value.
   *
   * @param key   the key for the mapping to remove
   * @param value value associated with the specified key
   *
   * @return true if the value was removed, false otherwise
   *
   * @see ConcurrentMap#remove(Object, Object)
   */
  public boolean remove(@Nullable Object key, Object value) {
    synchronized (getLock()) {
      if (map.containsKey(key)) {
        Object oldVal = map.get(key);
        if (
            Objects.equals(value, oldVal)
        ) {
          Map<K, V> copy = copyMap(map.size());
          copy.remove(key);
          map = copy;
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Remove mappings from the map. Mappings are only removed if they are identical to those in the map.
   *
   * @param map  the key-value pairs to remove.
   * @param <K1> the key type of the map pairs to be removed
   * @param <V1> the value type of the map pairs to be removed
   *
   * @return the number of entries removed
   */
  public <K1, V1> int removeAll(Map<K1, V1> map) {
    int count = 0;

    // lock the map for update
    synchronized (getLock()) {
      // create copy of map
      Map<K, V> copy = copyMap(this.map.size());
      Set<Entry<K1, V1>> keys = map.entrySet();

      // iterate over entries to remove
      for (Entry<K1, V1> entry : keys) {
        Object key = entry.getKey();

        // does this contain a mapping for this key?
        if (copy.containsKey(key)) {
          // is the key mapped to the specified value? Get the
          // current value to check.
          Object curVal = copy.get(key);
          Object remVal = entry.getValue();
          if (
              Objects.equals(curVal, remVal)
          ) {
            copy.remove(key);
            count++;
          }
        }
      }

      // replace the map with the copy
      this.map = copy;
    }

    // return the count
    return count;
  }


  /**
   * Remove a set of key mappings from the map.
   *
   * @param keys the set of keys to remove
   *
   * @return the number of entries removed
   */
  public int removeAll(Set<?> keys) {
    int count = 0;

    // lock the map for update
    synchronized (getLock()) {
      // create copy of map
      Map<K, V> copy = copyMap(map.size());

      // iterate over keys to remove
      for (Object key : keys) {
        // if this contains the key, remove it
        if (copy.containsKey(key)) {
          copy.remove(key);
          count++;
        }
      }

      // replace map
      map = copy;
    }

    // return count
    return count;
  }


  /**
   * Replace entry for key only if currently mapped to given value.
   *
   * @param key      key with which the specified value is associated
   * @param oldValue value expected to be associated with the specified key
   * @param newValue value to be associated with the specified key.
   *
   * @return true if the value was replaced
   */
  public boolean replace(@Nullable K key, @Nullable V oldValue, @Nullable V newValue) {
    synchronized (getLock()) {
      if (map.containsKey(key)) {
        Object mapVal = map.get(key);
        if (
            Objects.equals(mapVal, oldValue)
        ) {
          Map<K, V> copy = copyMap(map.size());
          copy.put(key, newValue);
          map = copy;
          return true;
        }
      }
      return false;
    }
  }


  /**
   * Replace entry for key only if currently mapped to some value.
   *
   * @param key      key with which the specified value is associated
   * @param newValue value to be associated with the specified key.
   *
   * @return previous value associated with specified key, or null if there was no mapping for key
   */
  public V replace(@Nullable K key, @Nullable V newValue) {
    synchronized (getLock()) {
      if (map.containsKey(key)) {
        Map<K, V> copy = copyMap(map.size());
        V obj = copy.put(key, newValue);
        map = copy;
        return obj;
      }
      return null;
    }
  }


  /**
   * Replace the contents of this map with the same mapping as in the supplied map.
   *
   * @param map replacement mappings
   */
  public void replace(Map<K, V> map) {
    Map<K, V> newDict = createNewMap(map.size());
    newDict.putAll(map);
    this.map = newDict;
  }


  /**
   * Get number of entries in this map.
   *
   * @return number of entries in this map.
   *
   * @see Map#size()
   */
  @Override
  public int size() {
    return map.size();
  }


  /** {@inheritDoc} */
  @Override
  public String toString() {
    return map.toString();
  }


  /**
   * Get the collection of all values in this map.  The map cannot be updated through this collection nor through iterators upon it.
   *
   * @return collection all values in this map
   *
   * @see Map#values()
   */
  @Override
  @Nonnull
  public Collection<V> values() {
    return Collections.unmodifiableCollection(map.values());
  }

}
