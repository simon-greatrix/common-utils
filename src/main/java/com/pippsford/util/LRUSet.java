package com.pippsford.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of items which is maintained at below an approximate size limit.
 *
 * @param <E> type of item stored in the set
 *
 * @author Simon Greatrix
 */
public class LRUSet<E> implements Set<E> {

  private static final Logger logger = LoggerFactory.getLogger(LRUSet.class);



  /** Thread to remove items from the set when it is over size. */
  class Cleaner extends LoggedThread {

    /** Number of items to remove. */
    private final int count;


    /**
     * Create cleaner thread.
     *
     * @param count number of items to remove
     */
    Cleaner(int count) {
      super(logger, "LRUSet cleaner");
      this.count = count;
    }


    @Override
    protected void runImpl() {
      Object[] keysToRemove = new Object[count];
      long[] timestamps = new long[count];
      for (int i = 0; i < count; i++) {
        timestamps[i] = Long.MAX_VALUE;
      }
      try {
        for (Entry<E, Long> entry : map.entrySet()) {
          long time = entry.getValue();
          int pos = Arrays.binarySearch(timestamps, time);
          if (pos < 0) {
            pos = -(pos + 1);
          }
          if (pos == count) {
            continue;
          }

          // move everything down to make room
          int moveLen = count - pos - 1;
          System.arraycopy(timestamps, pos, timestamps, pos + 1,
              moveLen
          );
          System.arraycopy(keysToRemove, pos, keysToRemove, pos + 1,
              moveLen
          );
          timestamps[pos] = time;
          keysToRemove[pos] = entry.getKey();
        }

        // now do remove
        for (int i = 0; i < count; i++) {
          Object key = keysToRemove[i];
          if (key == null) {
            continue;
          }
          map.remove(key, timestamps[i]);
        }
      } finally {
        cleaner = null;
      }

      // may still be over-size
      checkSize();
    }

  }



  /**
   * Map of value to the times they were last used.
   */
  final ConcurrentMap<E, Long> map;

  /** Synchronization lock. */
  private final Object lock = new Object();

  /** Maximum size for set. */
  private final int maxSize;

  /** Thread to remove items when over size. */
  Thread cleaner = null;


  /**
   * Create new LRU Set.
   *
   * @param maxSize approximate maximum size
   */
  public LRUSet(int maxSize) {
    this(maxSize, 0.75f, 16);
  }


  /**
   * Create new LRU Set.
   *
   * @param maxSize     approximate maximum size
   * @param load        hash table load factor
   * @param concurrency maximum concurrency
   */
  public LRUSet(int maxSize, float load, int concurrency) {
    map = new ConcurrentHashMap<>(maxSize, load, concurrency);
    this.maxSize = maxSize;
  }


  /**
   * Create new LRU Set.
   *
   * @param maxSize     approximate maximum size
   * @param concurrency maximum concurrency
   */
  public LRUSet(int maxSize, int concurrency) {
    this(maxSize, 0.75f, concurrency);
  }


  @Override
  public boolean add(E e) {
    Long v = System.currentTimeMillis();
    Long ov = map.put(e, v);
    if (ov == null) {
      checkSize();
      return true;
    }
    return false;
  }


  @Override
  public boolean addAll(@Nonnull Collection<? extends E> c) {
    Long v = System.currentTimeMillis();
    boolean isChanged = false;
    for (E e : c) {
      Long ov = map.put(e, v);
      isChanged = isChanged || ov == null;
    }
    checkSize();
    return isChanged;
  }


  /**
   * Check if the set is currently over size, and trigger a clean-up if it is.
   */
  void checkSize() {
    int mapSize = map.size();
    if (mapSize <= maxSize * 1.1) {
      return;
    }
    synchronized (lock) {
      if (cleaner != null) {
        return;
      }
      cleaner = new Cleaner(mapSize - (int) (0.9 * maxSize));
      cleaner.start();
    }
  }


  @Override
  public void clear() {
    map.clear();
  }


  @Override
  public boolean contains(Object o) {
    Long v = map.get(o);
    if (v == null) {
      return false;
    }
    // Object o must be an instance of E as it was in the map
    @SuppressWarnings("unchecked")
    E e = (E) o;
    v = System.currentTimeMillis();
    map.put(e, v);
    return true;
  }


  @Override
  public boolean containsAll(@Nonnull Collection<?> c) {
    Long v = System.currentTimeMillis();
    boolean all = true;
    for (Object o : c) {
      Long ov = map.get(o);
      if (ov == null) {
        all = false;
      } else {
        // Object o must be an instance of E as it was in the map
        @SuppressWarnings("unchecked")
        E e = (E) o;
        map.put(e, v);
      }
    }
    return all;
  }


  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }


  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  @Override
  @Nonnull
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }


  @Override
  public boolean remove(Object o) {
    Long v = map.remove(o);
    return (v != null);
  }


  @Override
  public boolean removeAll(@Nonnull Collection<?> c) {
    return map.keySet().removeAll(c);
  }


  @Override
  public boolean retainAll(@Nonnull Collection<?> c) {
    return map.keySet().retainAll(c);
  }


  @Override
  public int size() {
    return map.size();
  }


  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  @Override
  @Nonnull
  public Object[] toArray() {
    return map.keySet().toArray();
  }


  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  @Override
  @Nonnull
  public <T> T[] toArray(@Nonnull T[] a) {
    return map.keySet().toArray(a);
  }

}
