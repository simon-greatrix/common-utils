package com.pippsford.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A Map which can be read concurrently and allows for updates. The map that is being read from is rarely updated, so read operations are thread-safe and
 * multi-threading, whilst write operations are single threaded.
 *
 * <p>This map has two backing maps, a primary one which contains established data. This is never locked for read and never updated directly, so can always
 * multi-thread. The second backing map contains recent updates and is periodically flushed to the primary map. A mapping for a given key only exists in at most
 * one of the backing maps, which means that some write operations force a flush of the secondary map.
 *
 * <p>This map is appropriate for use in situations where updates tend to come in batches, for example associated with the activation of a new publish version.
 *
 * <p>Note that this map implementation does not support mutation through its entry set, key set or values collection, nor the iterators upon them. If you need
 * this functionality, copy this map into a new one, mutate that and then use the replace(java.util.Map) method to update this map.
 *
 * @param <K> the key type for this map
 * @param <V> the value type for this map
 *
 * @author Simon Greatrix
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class DeferredWriteMap<K, V> implements ConcurrentMap<K, V> {

  /**
   * Value representing null values inside tables.
   */
  protected static final Object NULL_VAL = new Object();

  /** How often the dynamic data is flushed. */
  private static final long DEFAULT_FLUSH_INTERVAL = 600000;


  /**
   * Add all the contents of the source map to the destination, masking null values as appropriate.
   *
   * @param dest destination map
   * @param src  source map
   */
  private static <K, V, K2 extends K, V2 extends V> void doPutAll(Map<K, Object> dest, Map<K2, V2> src) {
    if (src == null) {
      return;
    }
    for (Entry<K2, V2> e : src.entrySet()) {
      dest.put(e.getKey(), maskNull(e.getValue()));
    }
  }


  /**
   * Returns internal representation for value. Use NULL_VAL if value is null.
   *
   * @param key key to mask if it is null
   * @param <T> the type which is being masked
   *
   * @return the masked key
   */
  protected static <T> Object maskNull(T key) {
    return key == null ? NULL_VAL : key;
  }


  /**
   * Returns value represented by specified internal representation.
   *
   * @param key key to unmask if should be null
   * @param <T> the type which is being unmasked
   *
   * @return the unmasked key
   */
  protected static <T> T unmaskNull(Object key) {
    if (key == NULL_VAL) {
      return null;
    }

    @SuppressWarnings("unchecked")
    T t = (T) key;
    return t;
  }


  /** Implementation of Map.Entry used in the entry set. */
  private class MyEntry implements Entry<K, V> {

    /** Key for this entry. */
    private final K key;


    MyEntry(K key) {
      this.key = key;
    }


    /** {@inheritDoc} */
    public K getKey() {
      return key;
    }


    /** {@inheritDoc} */
    public V getValue() {
      return get(key);
    }


    /** {@inheritDoc} */
    public V setValue(V value) {
      return put(key, value);
    }

  }



  /** Dynamic Map loaded with changes. */
  protected final Map<K, Object> dynamicMap;

  /** Time between dynamic map flushes. */
  protected final long flushInterval;

  /** Time of nextEntry map flush. */
  protected long nextFlush = 0;

  /**
   * Static Map periodically updated with dynamic additions.
   */
  protected Map<K, Object> staticMap;


  /**
   * Create new instance.
   *
   * @param init          initialisation data which is copied into this, may be null
   * @param dynamicMap    backing dynamic map.
   * @param flushInterval milliseconds between flushes
   */
  protected DeferredWriteMap(Map<K, V> init, Map<K, Object> dynamicMap, long flushInterval) {
    if (init == null) {
      staticMap = newMap(-1);
    } else {
      staticMap = newMap(init.size());
      doPutAll(staticMap, init);
    }
    this.dynamicMap = dynamicMap;
    this.flushInterval = flushInterval;
  }


  /**
   * Create a new instance with the default flush interval and backed by a java.util.HashMap
   */
  public DeferredWriteMap() {
    this(null, new HashMap<>(), DEFAULT_FLUSH_INTERVAL);
  }


  /**
   * Create a new instance backed by the specified dynamic map and flushing at the specified interval.
   *
   * @param flushInterval milliseconds between flushes
   */
  public DeferredWriteMap(long flushInterval) {
    this(null, new HashMap<>(), flushInterval);
  }


  /**
   * Create a new instance with the default flush interval and backed by a java.util.HashMap
   *
   * @param map map to copy data from on initialisation
   */
  public DeferredWriteMap(Map<K, V> map) {
    this(map, new HashMap<>(), DEFAULT_FLUSH_INTERVAL);
  }


  /**
   * Create a new instance backed by the specified dynamic map and flushing at the specified interval.
   *
   * @param map           map to copy data from on initialisation
   * @param flushInterval milliseconds between flushes
   */
  public DeferredWriteMap(Map<K, V> map, long flushInterval) {
    this(map, new HashMap<>(), flushInterval);
  }


  /** {@inheritDoc} */
  public void clear() {
    synchronized (dynamicMap) {
      staticMap = newMap(-1);
      dynamicMap.clear();
    }
  }


  /** {@inheritDoc} */
  public boolean containsKey(Object key) {
    if (staticMap.containsKey(key)) {
      return true;
    }

    // It is possible that the key is currently in the dynamic map. It may
    // get flushed down to the static map before we get the lock, so we
    // need to recheck.

    synchronized (dynamicMap) {
      if (dynamicMap.containsKey(key)) {
        return true;
      }
      if (staticMap.containsKey(key)) {
        return true;
      }
    }
    return false;
  }


  /** {@inheritDoc} */
  public boolean containsValue(Object value) {
    if (staticMap.containsValue(value)) {
      return true;
    }

    // It is possible that the value is currently in the dynamic map. It
    // may get flushed down to the static map before we get the lock, so
    // we need to recheck.

    synchronized (dynamicMap) {
      if (dynamicMap.containsValue(value)) {
        return true;
      }
      if (staticMap.containsValue(value)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Get the entries in this map. The map cannot be removed from or added to via the returned set, and similarly additions and removals from this map are not
   * reflected in the returned set. However the entry's getValue() method always returns the current mapping (which will be null if the mapping has been
   * removed) and the entry's setValue method will write through to this map.
   *
   * @return read-only set of all entries in this map
   *
   * @see Map#entrySet()
   */
  @Nonnull
  public Set<Entry<K, V>> entrySet() {
    flushDynamic(true);
    Set<K> inEntries = staticMap.keySet();
    Set<Entry<K, V>> entries = new HashSet<>(1 + (int) Math.ceil(inEntries.size() / 0.75), 0.75f);
    for (K inEntry : inEntries) {
      entries.add(new MyEntry(inEntry));
    }
    return Collections.unmodifiableSet(entries);
  }


  @Override
  public boolean equals(Object o) {
    flushDynamic(true);
    return staticMap.equals(o);
  }


  /**
   * Copy all the entries from the dynamic map to the static map if a reasonable amount of time has passed since the last flush.
   *
   * @param force if true update the static map regardless of timing
   */
  protected void flushDynamic(boolean force) {
    long now = System.currentTimeMillis();

    synchronized (dynamicMap) {
      if (dynamicMap.isEmpty()) {
        return;
      }
      if ((!force) && (nextFlush > now)) {
        return;
      }

      Map<K, Object> newDict = newMap(staticMap.size() + dynamicMap.size());
      newDict.putAll(staticMap);
      newDict.putAll(dynamicMap);
      staticMap = Collections.unmodifiableMap(newDict);
      dynamicMap.clear();
      nextFlush = now + flushInterval;
    }
  }


  /** {@inheritDoc} */
  public V get(Object key) {
    Object v = staticMap.get(key);
    if (v == null) {
      synchronized (dynamicMap) {
        v = dynamicMap.get(key);
        if (v == null) {
          v = staticMap.get(key);
        } else {
          flushDynamic(false);
        }
      }
    }

    return unmaskNull(v);
  }


  @Override
  public int hashCode() {
    flushDynamic(true);
    return staticMap.hashCode();
  }


  /** {@inheritDoc} */
  public boolean isEmpty() {
    if (!staticMap.isEmpty()) {
      return false;
    }
    synchronized (dynamicMap) {
      if (!dynamicMap.isEmpty()) {
        return false;
      }
      return staticMap.isEmpty();
    }
  }


  /**
   * Get the set of all keys for this map. The map cannot be updated through this set nor through iterators upon it.
   *
   * @return set of all keys in this map.
   *
   * @see Map#keySet()
   */
  @Override
  @Nonnull
  public Set<K> keySet() {
    flushDynamic(true);
    Set<K> inKeys = staticMap.keySet();
    Set<K> entries = new HashSet<>(1 + (int) Math.ceil(inKeys.size() / 0.75), 0.75f);
    entries.addAll(inKeys);
    return Collections.unmodifiableSet(entries);
  }


  /**
   * Create a new map for the static data with the specified size. This implementation returns an instance of a java.util.HashMap.
   *
   * @param size required capacity. If non-positive, then used a default capacity.
   *
   * @return the new map
   */
  protected Map<K, Object> newMap(int size) {
    if (size <= 0) {
      return new HashMap<>();
    }

    // 0.75 if the default load factor for hash maps
    return new HashMap<>((int) Math.ceil(size / 0.75f), 0.75f);
  }


  /** {@inheritDoc} */
  public V put(K key, V value) {
    Object v = maskNull(value);
    synchronized (dynamicMap) {
      Object oldStaticVal = staticMap.get(key);
      Object oldVal = dynamicMap.put(key, v);

      if (oldStaticVal != null) {
        // the value was in static, so we must flush now
        oldVal = oldStaticVal;
        flushDynamic(true);
      } else {
        // Heuristically detect if this is the first of a new batch of
        // updates, such as might occur after a publish.
        long now = System.currentTimeMillis();
        if (nextFlush + flushInterval < now) {
          // We guess this is part of a new batch, so set the nextEntry
          // flush time into the future to give some time for the
          // rest of the batch to arrive.
          nextFlush = now + flushInterval;
        }
      }

      return unmaskNull(oldVal);
    }
  }


  /** {@inheritDoc} */
  @Override
  public void putAll(@Nullable Map<? extends K, ? extends V> t) {
    synchronized (dynamicMap) {
      doPutAll(dynamicMap, t);
      flushDynamic(true);
    }
  }


  /** {@inheritDoc} */
  public V putIfAbsent(@Nullable K key, V value) {
    // check static map first for speed
    Object oldVal = staticMap.get(key);
    if (oldVal != null) {
      return unmaskNull(oldVal);
    }

    synchronized (dynamicMap) {
      // wasn't in static, but might be in dynamic
      oldVal = dynamicMap.get(key);
      if (oldVal != null) {
        return unmaskNull(oldVal);
      }

      // small chance of a flush, so recheck static
      oldVal = staticMap.get(key);
      if (oldVal != null) {
        return unmaskNull(oldVal);
      }

      // definitely absent so we can just add it to dynamic
      // and we are done.
      dynamicMap.put(key, value);

      // Heuristically detect if this is the first of a new batch of
      // updates, such as might occur after a publish.
      long now = System.currentTimeMillis();
      if (nextFlush + flushInterval < now) {
        // We guess this is part of a new batch, so set the nextEntry
        // flush time into the future to give some time for the
        // rest of the batch to arrive.
        nextFlush = now + flushInterval;
      }

      // the old value must have been null
      return null;
    }
  }


  /** {@inheritDoc} */
  public V remove(@Nullable Object key) {
    synchronized (dynamicMap) {
      if (staticMap.containsKey(key)) {
        Map<K, Object> newDict = newMap(staticMap.size() + dynamicMap.size());
        newDict.putAll(staticMap);
        newDict.putAll(dynamicMap);
        final Object val = newDict.remove(key);
        staticMap = Collections.unmodifiableMap(newDict);
        dynamicMap.clear();
        nextFlush = System.currentTimeMillis() + flushInterval;
        return unmaskNull(val);
      }

      return unmaskNull(dynamicMap.remove(key));
    }
  }


  /** {@inheritDoc} */
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    Object val = maskNull(value);
    synchronized (dynamicMap) {
      // check static map
      Object oldVal = staticMap.get(key);
      if (oldVal == null) {
        // may be in dynamic map, but if missing cannot remove
        oldVal = dynamicMap.get(key);
        if ((oldVal != null) && oldVal.equals(val)) {
          // found in dynamic remove
          dynamicMap.remove(key);
          return true;
        }

        // was missing or didn't match
        return false;
      }

      // check if we have the right value for removing
      if (!oldVal.equals(val)) {
        return false;
      }

      // found match in static, so will have to rebuild
      Map<K, Object> newDict = newMap(staticMap.size() + dynamicMap.size());
      newDict.putAll(staticMap);
      newDict.putAll(dynamicMap);
      newDict.remove(key);
      staticMap = Collections.unmodifiableMap(newDict);
      dynamicMap.clear();
      nextFlush = System.currentTimeMillis() + flushInterval;
      return true;
    }
  }


  /**
   * Remove mappings from the map. Mappings are only removed if they are identical to those in the map.
   *
   * @param map the key-value pairs to remove.
   *
   * @return the number of entries removed
   */
  public int removeAll(Map<K, V> map) {
    int cnt = 0;
    synchronized (dynamicMap) {
      // create map of everything in this map
      Map<K, Object> newDict = newMap(staticMap.size() + dynamicMap.size());
      newDict.putAll(staticMap);
      newDict.putAll(dynamicMap);

      // loop over provided map...
      for (Entry<K, V> e : map.entrySet()) {
        Object k = e.getKey();
        Object v = maskNull(e.getValue());
        Object c = newDict.get(k);

        // if entry matches the one in this map, remove it
        if (v.equals(c)) {
          newDict.remove(k);
          cnt++;
        }
      }

      // update our static map
      staticMap = Collections.unmodifiableMap(newDict);
      dynamicMap.clear();
      nextFlush = System.currentTimeMillis() + flushInterval;
    }
    return cnt;
  }


  /**
   * Remove a set of key mappings from the map.
   *
   * @param keys the set of keys to remove
   *
   * @return the number of entries removed
   */
  public int removeAll(Set<?> keys) {
    int cnt = 0;
    synchronized (dynamicMap) {
      Map<K, Object> newDict = newMap(staticMap.size() + dynamicMap.size());
      newDict.putAll(staticMap);
      newDict.putAll(dynamicMap);

      for (Object k : keys) {
        if (newDict.remove(k) != null) {
          cnt++;
        }
      }

      staticMap = Collections.unmodifiableMap(newDict);
      dynamicMap.clear();
      nextFlush = System.currentTimeMillis() + flushInterval;
    }
    return cnt;
  }


  /** {@inheritDoc} */
  public V replace(@Nullable K key, @Nullable V value) {
    Object val = maskNull(value);
    synchronized (dynamicMap) {
      // check static map. As we have the lock, it will be in static or
      // in dynamic but not in both.
      Object oldStaticVal = staticMap.get(key);
      if (oldStaticVal == null) {
        // may be in dynamic map, but if missing cannot replace
        Object oldVal = dynamicMap.get(key);
        if (oldVal == null) {
          return null;
        }

        // not in static map, so only need to update dynamic
        return unmaskNull(dynamicMap.put(key, val));
      }

      // value was mapped in static, so need to update and flush
      Map<K, Object> newDict = newMap(staticMap.size() + dynamicMap.size());
      newDict.putAll(staticMap);
      newDict.putAll(dynamicMap);
      newDict.put(key, val);
      staticMap = Collections.unmodifiableMap(newDict);
      dynamicMap.clear();
      nextFlush = System.currentTimeMillis() + flushInterval;
      return unmaskNull(oldStaticVal);
    }
  }


  /** {@inheritDoc} */
  public boolean replace(@Nullable K key, @Nullable V oldValue, @Nullable V newValue) {
    Object oldVal = maskNull(oldValue);
    Object newVal = maskNull(newValue);
    if (oldVal.equals(newVal)) {
      return false;
    }

    synchronized (dynamicMap) {
      // check static map. As we have the lock, it will be in static or
      // in dynamic but not in both.
      Object currVal = staticMap.get(key);
      if (currVal == null) {
        // may be in dynamic map, but if missing cannot replace
        currVal = dynamicMap.get(key);
        if (currVal == null) {
          return false;
        }

        // not in static map, so only need to update replace if value
        // matches
        if (currVal.equals(oldVal)) {
          // value matches, do replace
          dynamicMap.put(key, newVal);
          return true;
        }

        // was mapped but to a different val, so no replace
        return false;
      }

      // value was mapped in static, did it match?
      if (!currVal.equals(oldVal)) {
        return false;
      }

      // mapped in static and matches value, so do replace
      Map<K, Object> newDict = newMap(staticMap.size() + dynamicMap.size());
      newDict.putAll(staticMap);
      newDict.putAll(dynamicMap);
      newDict.put(key, newVal);
      staticMap = Collections.unmodifiableMap(newDict);
      dynamicMap.clear();
      nextFlush = System.currentTimeMillis() + flushInterval;
      return true;
    }
  }


  /**
   * Replace the contents of this map with the same mapping as in the supplied map.
   *
   * @param map replacement mappings
   */
  public void replace(Map<K, V> map) {
    synchronized (dynamicMap) {
      Map<K, Object> newDict = newMap(map.size());
      doPutAll(newDict, map);
      staticMap = Collections.unmodifiableMap(newDict);
      dynamicMap.clear();
      nextFlush = System.currentTimeMillis() + flushInterval;
    }
  }


  /** {@inheritDoc} */
  public int size() {
    flushDynamic(true);
    return staticMap.size();
  }


  @Override
  public String toString() {
    flushDynamic(true);
    return staticMap.toString();
  }


  /**
   * Get the collection of all values in this map.  The map cannot be updated through this collection nor through iterators upon it.
   *
   * @return collection all values in this map
   *
   * @see Map#values()
   */
  @Nonnull
  public Collection<V> values() {
    flushDynamic(true);
    Collection<Object> inValues = staticMap.values();
    ArrayList<V> myValues = new ArrayList<>(inValues.size());
    for (Object inVal : inValues) {
      myValues.add(unmaskNull(inVal));
    }
    return Collections.unmodifiableCollection(myValues);
  }

}
