package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class LRUCacheTest {

  LRUCache<Integer, String> cache = new LRUCache<>(1, 10, n -> null);


  @Test
  public void get() {
    for (int i = 0; i < 5; i++) {
      cache.put(i, Integer.toString(i));
    }
    assertNull(cache.get(6));
    assertEquals("1", cache.get(1));

    for (int i = 0; i < 15; i++) {
      cache.put(i, Integer.toString(i));
    }
    assertEquals(null, cache.get(1));
  }


  @Test
  public void remove() {
    for (int i = 0; i < 5; i++) {
      cache.put(i, Integer.toString(i));
    }
    LRUCache<Integer, String> c2 = new LRUCache<>(cache, n -> "x");
    assertEquals("1", c2.get(1));
    assertEquals("x", c2.get(10));
    c2.remove(1);
    assertEquals("x", c2.get(1));
  }
}