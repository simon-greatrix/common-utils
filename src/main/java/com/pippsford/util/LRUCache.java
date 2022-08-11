package com.pippsford.util;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;

/**
 * A concurrent cache which uses a Least-Recently-Used eviction algorithm.
 *
 * @author Simon Greatrix on 2019-03-07.
 */
public class LRUCache<K, V> {

  /** Default concurrency. Should be a prime number. */
  private static final int DEFAULT_CONCURRENCY = 17;



  static class LRU<K, V> {

    final K key;

    final V value;

    LRU<K, V> next;

    LRU<K, V> previous;


    LRU() {
      key = null;
      value = null;
      previous = this;
      next = this;
    }


    @SuppressWarnings("squid:S2445")
    LRU(LRU<K, V> head, K key, V value) {
      this.key = key;
      this.value = value;

      next = head.next;
      next.previous = this;

      previous = head;
      head.next = this;
    }


    @SuppressWarnings("squid:S2445")
    void remove() {
      previous.next = next;
      next.previous = previous;

      next = null;
      previous = null;
    }


    @SuppressWarnings("squid:S2445")
    void touch(LRU<K, V> head) {
      if (head == previous) {
        return;
      }
      previous.next = next;
      next.previous = previous;

      next = head.next;
      next.previous = this;

      previous = head;
      head.next = this;
    }

  }



  class Segment {

    final LRU<K, V> head;

    int size;


    Segment() {
      size = 0;
      head = new LRU<>();
    }


    Segment(Segment old) {
      size = old.size;
      head = new LRU<>();
      LRU<K, V> oldHead = old.head;
      LRU<K, V> oldEntry = oldHead.previous;
      while (oldEntry != oldHead) {
        LRU<K, V> newEntry = new LRU<>(head, oldEntry.key, oldEntry.value);
        map.put(newEntry.key, newEntry);
        oldEntry = oldEntry.previous;
      }
    }


    synchronized V get(K key, Function<K, ? extends V> valueSource) {
      LRU<K, V> lru = map.get(key);
      if (lru != null) {
        lru.touch(head);
        return lru.value;
      }

      V value = valueSource.apply(key);
      put(key, value);
      return value;
    }


    synchronized void put(K key, V value) {
      LRU<K, V> newLRU = new LRU<>(head, key, value);
      LRU<K, V> oldLRU = map.put(key, newLRU);
      if (oldLRU != null) {
        // size has not changed
        oldLRU.remove();
        return;
      }

      size++;
      if (size <= maxSegment) {
        // size is OK
        return;
      }

      // Need to remove LRU
      LRU<K, V> leastUsed = head.previous;
      leastUsed.remove();
      map.remove(leastUsed.key);
    }


    synchronized void remove(K key) {
      LRU<K, V> current = map.remove(key);
      if (current == null) {
        // was not present
        return;
      }

      // Size is reduced
      current.remove();
      size--;
    }

  }



  private final Function<K, V> baseValueSource;

  private final int concurrency;

  private final ConcurrentHashMap<K, LRU<K, V>> map;

  private final int maxSegment;

  private final int maxSize;

  private final ArrayList<Segment> segments;


  public LRUCache(int maxSize, Function<K, V> valueSource) {
    this(DEFAULT_CONCURRENCY, maxSize, valueSource);
  }


  /**
   * New instance.
   *
   * @param concurrency maximum supported concurrency
   * @param maxSize     maximum number of entries in cache
   * @param valueSource source of values for cache
   */
  public LRUCache(int concurrency, int maxSize, Function<K, V> valueSource) {
    map = new ConcurrentHashMap<>();
    this.maxSize = maxSize;
    maxSegment = Math.max(2, (int) Math.ceil((double) maxSize / concurrency));
    this.concurrency = concurrency;
    baseValueSource = valueSource;
    segments = new ArrayList<>(concurrency);
    for (int i = 0; i < concurrency; i++) {
      segments.add(i, new Segment());
    }
  }


  /**
   * Create a new instance with the same initial data as an existing instance.
   *
   * @param source      the existing instance
   * @param valueSource the source for new values
   */
  public LRUCache(LRUCache<K, V> source, Function<K, V> valueSource) {
    map = new ConcurrentHashMap<>(source.map.size());
    concurrency = source.concurrency;
    maxSize = source.maxSize;
    maxSegment = source.maxSegment;
    baseValueSource = valueSource;

    ArrayList<ForkJoinTask<Segment>> tasks = new ArrayList<>(concurrency);
    for (int i = 0; i < concurrency; i++) {
      final Segment oldSegment = source.segments.get(i);
      tasks.add(ForkJoinPool.commonPool().submit(() -> new Segment(oldSegment)));
    }

    segments = new ArrayList<>(concurrency);
    for (int i = 0; i < concurrency; i++) {
      segments.add(i, tasks.get(i).join());
    }
  }


  public V get(K key) {
    return getSegment(key).get(key, baseValueSource);
  }


  /**
   * Get a value from this cache, using the provided function to compute a value if none is present.
   *
   * @param key         the key to look up
   * @param valueSource the function to calculate a value
   * @param <V2>        the value's type
   *
   * @return the value
   */
  public <V2 extends V> V2 get(K key, Function<K, V2> valueSource) {
    @SuppressWarnings("unchecked")
    V2 v2 = (V2) getSegment(key).get(key, valueSource);
    return v2;
  }


  Segment getSegment(K key) {
    return segments.get((key.hashCode() & 0x7fff_ffff) % concurrency);
  }


  public void put(K key, V value) {
    getSegment(key).put(key, value);
  }


  public void remove(K key) {
    getSegment(key).remove(key);
  }

}
