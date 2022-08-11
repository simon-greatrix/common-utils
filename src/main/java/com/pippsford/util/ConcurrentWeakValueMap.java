package com.pippsford.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A map where the values are WeakReferences. This means that values can be garbage collected when no longer in use.
 * Note that this map is thread-safe.
 *
 * @param <K> the key type for this map
 * @param <V> the value type for this map
 *
 * @author Simon Greatrix
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ConcurrentWeakValueMap<K, V> implements ConcurrentMap<K, V> {

  /**
   * Iterator over map entries. The Iterator automatically skips enqueued referents.
   */
  private abstract static class AbstractIterator<K2, V2, I2> implements Iterator<I2> {

    /**
     * Iterator on backing map.
     */
    protected final Iterator<Entry<K2, MapReference<K2, V2>>> iterator;

    /**
     * Current entry.
     */
    protected ReferenceEntry<K2, V2> current;

    /**
     * Next entry to return.
     */
    protected ReferenceEntry<K2, V2> next;


    public AbstractIterator(
        Iterator<Entry<K2, MapReference<K2, V2>>> iterator
    ) {

      this.iterator = iterator;
      current = null;
      getNext();
    }


    /**
     * Get the nextEntry value, skipping enqueued referents.
     */
    protected final void getNext() {

      while (iterator.hasNext()) {
        Entry<K2, MapReference<K2, V2>> entry = iterator.next();
        MapReference<K2, V2> ref = entry.getValue();
        V2 value = ref.get();
        if (value != null) {
          // have good value
          next = new ReferenceEntry<>(entry, value);
          return;
        }
      }

      // no more entries
      next = null;
    }


    /**
     * Is there a nextEntry, non-enqueued, entry?.
     */
    public boolean hasNext() {

      return next != null;
    }


    /**
     * Get the nextEntry entry. The return type depends on the type of this Iterator.
     */
    protected Entry<K2, V2> nextEntry() {

      current = next;
      if (current == null) {
        throw new NoSuchElementException();
      }

      getNext();
      return current;
    }


    /**
     * Remove the current value from this map.
     */
    public void remove() {

      iterator.remove();
      current = null;
    }

  }



  /**
   * Iterator over map keys. The Iterator automatically skips enqueued referents.
   */
  private static class EntryIterator<K2, V2> extends AbstractIterator<K2, V2, Entry<K2, V2>> {

    public EntryIterator(
        Iterator<Entry<K2, MapReference<K2, V2>>> iterator
    ) {

      super(iterator);
    }


    /**
     * Get the nextEntry entry from this map.
     */
    // Do not need to explicitly throw NoSuchElementException as nextEntry() does that.
    @SuppressWarnings("squid:S2272")
    public Entry<K2, V2> next() {

      return nextEntry();
    }

  }



  private static class EntrySpliterator<K2, V2> implements Spliterator<Entry<K2, V2>> {

    /**
     * Iterator on backing map.
     */
    protected final Spliterator<Entry<K2, MapReference<K2, V2>>> spliterator;


    protected EntrySpliterator(Spliterator<Entry<K2, MapReference<K2, V2>>> spliterator) {
      this.spliterator = spliterator;
    }


    @Override
    public int characteristics() {
      // We are neither SIZED nor SUB-SIZED, as garbage collection may occur.
      // Even if the underlying spliterator is SORTED, we cannot expose it as it operates on the wrong generic type.
      return NONNULL | (spliterator.characteristics() & ~(SIZED | SUBSIZED | SORTED));
    }


    @Override
    public long estimateSize() {
      return spliterator.estimateSize();
    }


    @Override
    public long getExactSizeIfKnown() {
      // Garbage collection may occur, so the exact size is never known.
      return -1L;
    }


    @Override
    public boolean tryAdvance(final Consumer<? super Entry<K2, V2>> action) {
      final boolean[] searching = {true};
      Consumer<? super Entry<K2, MapReference<K2, V2>>> consumer = e -> {
        MapReference<K2, V2> ref = e.getValue();
        V2 value = ref.get();
        if (value != null) {
          searching[0] = false;
          action.accept(new ReferenceEntry<>(e, value));
        }
      };
      do {
        if (!spliterator.tryAdvance(consumer)) {
          return false;
        }
      } while (searching[0]);
      return true;
    }


    @Override
    public Spliterator<Entry<K2, V2>> trySplit() {
      Spliterator<Entry<K2, MapReference<K2, V2>>> split = spliterator.trySplit();
      if (split != null) {
        return new EntrySpliterator<>(split);
      }
      return null;
    }

  }



  /**
   * A WeakReference that knows which map key maps to its value. This allows the ReferenceQueue to remove the mapping
   * from the map. Furthermore this reference
   * will pretend to be equal to its value hiding the dereference operation when examining values in the map.
   */
  public static class MapReference<K2, V2> extends WeakReference<V2> {

    /**
     * The map key for this value.
     */
    private final K2 key;

    /**
     * My myQueue.
     */
    private final ReferenceQueue<V2> myQueue;


    /**
     * Create a new MapReference.
     *
     * @param queue the reference myQueue this will be registered with
     * @param key   the associated key
     * @param val   the referenced value
     */
    public MapReference(ReferenceQueue<V2> queue, K2 key, V2 val) {

      super(val, queue);
      this.key = key;
      myQueue = queue;
    }


    /**
     * A MapReference is equal to another object if it is either a reference to the same object, or the object itself.
     * This makes matching values easy.
     *
     * @param o the object to compare with for equality
     *
     * @return true if this equals the given object
     */
    @Override
    // It is OK to assign to parameters.
    @SuppressWarnings("squid:S1226")
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (o instanceof Reference<?>) {
        Reference<?> ref = (Reference<?>) o;
        o = ref.get();
        if (o == null) {
          return false;
        }
      }
      return o.equals(get());
    }


    /**
     * Get the key this was mapped from.
     *
     * @return the associated key
     */
    public K2 getKey() {

      return key;
    }


    /**
     * We use the same hashCode as the value.
     *
     * @return the value's hash code
     */
    @Override
    public int hashCode() {

      Object val = get();
      return (val == null) ? 0 : val.hashCode();
    }


    /**
     * Create a new MapReference mapped to the same key as this and registered in the specified ReferenceQueue.
     *
     * @param newVal the new value for this mapping
     *
     * @return a reference for the new value
     */
    public MapReference<K2, V2> newReference(V2 newVal) {
      if (newVal == null) {
        throw new NullReferenceException();
      }
      return new MapReference<>(myQueue, key, newVal);
    }


    /**
     * This reference's value as a string.
     *
     * @return this reference's value as a string
     */
    @Override
    public String toString() {
      return String.valueOf(get());
    }

  }



  private static class NullReferenceException extends IllegalArgumentException {

    NullReferenceException() {
      super("Cannot have reference to null.");
    }

  }



  /**
   * An entry in the map. To prevent garbage collection, the entry maintains a strong reference to its value.
   */
  private static class ReferenceEntry<K, V> implements Entry<K, V> {

    /**
     * The original entry in the backing map.
     */
    final Entry<K, MapReference<K, V>> entry;

    /**
     * The strong reference to the value.
     */
    V value;


    /**
     * Create a new ReferenceEntry.
     */
    public ReferenceEntry(Entry<K, MapReference<K, V>> entry, V value) {

      this.entry = entry;
      this.value = value;
    }


    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof Entry<?, ?>)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      return Objects.equals(e.getKey(), entry.getKey()) && Objects.equals(e.getValue(), value);
    }


    /**
     * Get the map key for this entry.
     */
    public K getKey() {

      return entry.getKey();
    }


    /**
     * Get the map value for this entry.
     */
    public V getValue() {

      return value;
    }


    @Override
    public int hashCode() {

      return entry.hashCode();
    }


    /**
     * Set the map value for this entry.
     */
    public V setValue(V newValue) {

      MapReference<K, V> ref = entry.getValue();
      MapReference<K, V> newRef = ref.newReference(newValue);
      entry.setValue(newRef);
      V oldVal = value;
      value = newValue;
      return oldVal;
    }


    public String toString() {
      return entry.getKey() + "=" + value;
    }

  }



  /**
   * Iterator over map entries. The Iterator automatically skips enqueued referents.
   */
  private static class ValueIterator<K2, V2> extends AbstractIterator<K2, V2, V2> {

    public ValueIterator(
        Iterator<Entry<K2, MapReference<K2, V2>>> iterator
    ) {

      super(iterator);
    }


    /**
     * Get the nextEntry value from this map.
     */
    // Do not need to explicitly throw NoSuchElementException as nextEntry() does that.
    @SuppressWarnings("squid:S2272")
    public V2 next() {
      Entry<K2, V2> e = nextEntry();
      return e.getValue();
    }

  }



  private static class ValueSpliterator<K2, V2> implements Spliterator<V2> {

    /**
     * Iterator on backing map.
     */
    protected final Spliterator<MapReference<K2, V2>> spliterator;


    protected ValueSpliterator(Spliterator<MapReference<K2, V2>> spliterator) {
      this.spliterator = spliterator;
    }


    @Override
    public int characteristics() {
      // We are neither SIZED nor SUB-SIZED, as garbage collection may occur.
      // Even if the underlying spliterator is SORTED, we cannot expose it as it operates on the wrong generic type.
      return NONNULL | (spliterator.characteristics() & ~(SIZED | SUBSIZED | SORTED));
    }


    @Override
    public long estimateSize() {
      return spliterator.estimateSize();
    }


    @Override
    public long getExactSizeIfKnown() {
      // Garbage collection may occur, so the exact size is never known.
      return -1L;
    }


    @Override
    public boolean tryAdvance(final Consumer<? super V2> action) {
      final boolean[] searching = {true};
      Consumer<? super MapReference<K2, V2>> consumer = ref -> {
        V2 value = ref.get();
        if (value != null) {
          searching[0] = false;
          action.accept(value);
        }
      };
      do {
        if (!spliterator.tryAdvance(consumer)) {
          return false;
        }
      } while (searching[0]);
      return true;
    }


    @Override
    public Spliterator<V2> trySplit() {
      Spliterator<MapReference<K2, V2>> split = spliterator.trySplit();
      if (split != null) {
        return new ValueSpliterator<>(split);
      }
      return null;
    }

  }


  /**
   * Get an array from a collection. Special handling is needed to skip enqueued referents.
   *
   * @param coll  the collection to process
   * @param array the candidate destination array
   * @param <T>   the type of array to return
   *
   * @return an array containing everything in the collection
   */
  // It is OK to assign to parameters.
  @SuppressWarnings({"squid:S1226"})
  // We have to be sure the correct iterator is used to handle enqueued referents.
  @SuppressFBWarnings("UAA_USE_ADD_ALL")
  static <T> T[] toArray(Collection<? extends T> coll, T[] array) {
    // Get all the objects in the collection.
    // The size may change, so we get the objects into blocks
    // and put the blocks into a list
    List<T> blocks = new ArrayList<>(coll.size());
    Iterator<? extends T> itr = coll.iterator();
    while (itr.hasNext()) {
      blocks.add(itr.next());
    }
    int size = blocks.size();

    // ensure the destination is big enough
    if (array.length < size) {
      @SuppressWarnings("unchecked")
      T[] newArray = (T[]) java.lang.reflect.Array.newInstance(
          array.getClass().getComponentType(), size);
      array = newArray;
    }

    // copy the objects out of the blocks into the destination
    int i = 0;
    itr = blocks.iterator();
    while (itr.hasNext()) {
      array[i] = itr.next();
      i++;
    }

    // put in trailing null if necessary
    if (i < array.length) {
      array[i] = null;
    }

    return array;
  }


  /**
   * Set of Entries in this map.
   */
  protected class Entries extends RefSet<Entry<K, V>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o) {

      if (o == null) {
        return false;
      }
      if (!(o instanceof Map.Entry<?, ?>)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;

      Object val = get(e.getKey());

      return (val != null) && (val.equals(e.getValue()));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Iterator<Entry<K, V>> iterator() {

      return new EntryIterator<>(map.entrySet().iterator());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o) {

      if (o == null) {
        return false;
      }
      if (!(o instanceof Map.Entry<?, ?>)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      return ConcurrentWeakValueMap.this.remove(e.getKey()) != null;
    }


    @Override
    public Spliterator<Entry<K, V>> spliterator() {
      return new EntrySpliterator<>(map.entrySet().spliterator());
    }

  }



  /**
   * Abstract set of keys or entries in this map.
   */
  private abstract class RefSet<Q> extends AbstractSet<Q> {

    protected RefSet() {
      // do nothing
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {

      ConcurrentWeakValueMap.this.clear();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {

      return ConcurrentWeakValueMap.this.size();
    }


    /**
     * Get the objects in this set as an array. Note that enqueued referents will be skipped.
     */
    @Nonnull
    @Override
    public Object[] toArray() {

      return toArray(new Object[0]);
    }


    /**
     * Get the objects in this set as an array. Note that enqueued referents will be skipped, so the returned array may
     * have less values than the size() would
     * indicate. As per the Map API if the supplied array is too large, there will be a trailing null after all this
     * Set's objects.
     */
    @Override
    @Nonnull
    public <T> T[] toArray(@Nonnull T[] array) {

      @SuppressWarnings("unchecked")
      T[] newArray = (T[]) ConcurrentWeakValueMap.toArray(this, array);
      return newArray;
    }

  }



  /**
   * Collection of values in this map.
   */
  protected class Values extends AbstractCollection<V> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {

      ConcurrentWeakValueMap.this.clear();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o) {

      return containsValue(o);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Iterator<V> iterator() {

      return new ValueIterator<>(map.entrySet().iterator());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {

      return ConcurrentWeakValueMap.this.size();
    }


    @Override
    public Spliterator<V> spliterator() {
      return new ValueSpliterator<>(map.values().spliterator());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public <T> T[] toArray(@Nonnull T[] array) {

      @SuppressWarnings("unchecked")
      T[] newArray = (T[]) ConcurrentWeakValueMap.toArray(this, array);
      return newArray;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Object[] toArray() {

      return toArray(new Object[0]);
    }

  }



  /**
   * The backing map.
   */
  protected final ConcurrentMap<K, MapReference<K, V>> map;

  /**
   * The reference myQueue where expired values will be placed.
   */
  private final ReferenceQueue<V> queue = new ReferenceQueue<>();

  /**
   * Entries set, initialised lazily.
   */
  private Set<Entry<K, V>> entries = null;


  /**
   * Values collection, initialised lazily.
   */
  private Collection<V> values = null;


  /**
   * Create new WeakValueMap backed by a HashMap and allowing clean-up on read.
   */
  public ConcurrentWeakValueMap() {

    this(new ConcurrentHashMap<>());
  }


  /**
   * Create new WeakValueMap backed by the specified map and allowing clean-up on read.
   *
   * @param map the map to initialise from
   */
  public ConcurrentWeakValueMap(ConcurrentMap<K, MapReference<K, V>> map) {
    this.map = map;
  }


  /**
   * Trigger clean-up of the map. This should be called if the map is not being written to and clean-up on read is not
   * allowed.
   * This method is called automatically on every write operation and on every read operation if clean-up on read is
   * allowed.
   */
  public void cleanUp() {

    while (true) {
      @SuppressWarnings("unchecked")
      MapReference<K, V> ref = (MapReference<K, V>) queue.poll();
      if (ref == null) {
        break;
      }
      K key = ref.getKey();
      MapReference<K, V> val = map.get(key);
      if (val == ref) {
        map.remove(key);
        notifyCleanUp(key);
      }
    }
  }


  /**
   * Clear this map.
   *
   * @see Map#clear()
   */
  public void clear() {

    cleanUp();
    map.clear();
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

    cleanUp();
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

    cleanUp();

    // we can't contain null, so return false now and avoid NPE later
    if (value == null) {
      return false;
    }

    // two MapRefs are equal if they point to the same value, so
    // create a new ref with a null key to match on

    MapReference<K, Object> ref = new MapReference<>(null, null,
        value
    );
    return map.containsValue(ref);
  }


  /**
   * Does this map contain the specified value?.
   *
   * @return true if this map contains a mapping to the value
   *
   * @see Map#containsValue(Object)
   */
  @Nonnull
  public Set<Entry<K, V>> entrySet() {

    cleanUp();

    Set<Entry<K, V>> es = entries;
    if (es != null) {
      return es;
    }
    entries = new Entries();
    return entries;
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

    cleanUp();
    MapReference<K, V> ref = map.get(key);
    return (ref != null) ? ref.get() : null;
  }


  /**
   * Is this map empty?.
   *
   * @return true if this map is empty
   *
   * @see Map#isEmpty()
   */
  public boolean isEmpty() {

    cleanUp();
    return map.isEmpty();
  }


  /**
   * Get the set of all keys for this map.
   *
   * @return set of all keys in this map.
   *
   * @see Map#keySet()
   */
  @Nonnull
  public Set<K> keySet() {
    cleanUp();
    return map.keySet();
  }


  /**
   * Invoked when the garbage collection of a key has been detected and its key mapping removed. The default
   * implementation does nothing.
   *
   * @param key the key whose value was GCed.
   */
  protected void notifyCleanUp(K key) {
    // do nothing
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
    cleanUp();
    if (value == null) {
      throw new NullReferenceException();
    }
    MapReference<K, V> ref = new MapReference<>(queue, key, value);
    MapReference<K, V> ret = map.put(key, ref);
    return (ret != null) ? ret.get() : null;
  }


  /**
   * Put all the mappings in the specified map into this map.
   *
   * @param t the map to copy into this
   *
   * @see Map#putAll(Map)
   */
  public void putAll(@Nonnull Map<? extends K, ? extends V> t) {

    cleanUp();
    for (Entry<? extends K, ? extends V> e : t.entrySet()) {
      K k = e.getKey();
      V v = e.getValue();
      put(k, v);
    }
  }


  @Override
  public V putIfAbsent(K key, V value) {
    cleanUp();
    if (value == null) {
      throw new NullReferenceException();
    }
    MapReference<K, V> ref = new MapReference<>(queue, key, value);
    MapReference<K, V> ret = map.putIfAbsent(key, ref);
    return (ret != null) ? ret.get() : null;
  }


  @Override
  public boolean remove(Object key, Object value) {
    cleanUp();
    // Cannot have null values
    if (value == null) {
      return false;
    }

    MapReference<K, V> ref = map.get(key);
    if (ref == null) {
      return false;
    }
    if (value.equals(ref.get())) {
      return map.remove(key, ref);
    }

    return false;
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
    cleanUp();
    MapReference<K, V> ret = map.remove(key);
    return (ret != null) ? ret.get() : null;
  }


  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    if (oldValue == null) {
      return false;
    }
    if (newValue == null) {
      throw new NullReferenceException();
    }
    MapReference<K, V> oldRef = new MapReference<>(queue, key, oldValue);
    MapReference<K, V> newRef = new MapReference<>(queue, key, newValue);
    return map.replace(key, oldRef, newRef);
  }


  @Override
  public V replace(K key, V value) {
    if (value == null) {
      throw new NullReferenceException();
    }
    cleanUp();
    MapReference<K, V> oldRef = map.get(key);
    if (oldRef == null) {
      return null;
    }

    V oldValue = oldRef.get();
    if (oldValue != null) {
      MapReference<K, V> newRef = new MapReference<>(queue, key, value);
      oldRef = map.replace(key, newRef);

      // another thread may have removed or changed the reference
      if (oldRef == null) {
        return null;
      }

      V oldValue2 = oldRef.get();
      if (oldValue2 != null) {
        oldValue = oldValue2;
      }
    }
    return oldValue;
  }


  /**
   * Get number of entries in this map.
   *
   * @return number of entries in this map.
   *
   * @see Map#size()
   */
  public int size() {
    cleanUp();
    return map.size();
  }


  /**
   * Get the collection of all values in this map.
   *
   * @return collection all values in this map
   *
   * @see Map#values()
   */
  @Nonnull
  public Collection<V> values() {
    cleanUp();

    Collection<V> vs = values;
    if (vs != null) {
      return vs;
    }
    values = new Values();
    return values;
  }


}
