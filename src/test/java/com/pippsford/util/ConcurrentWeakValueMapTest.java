package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.ref.WeakReference;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/**
 * @author Simon Greatrix on 23/08/2018.
 */
public class ConcurrentWeakValueMapTest {

  ConcurrentWeakValueMap<Integer, Object> map = new ConcurrentWeakValueMap<>();

  Random rand = new Random(0x7357ab1e);


  @Test
  public void clear() {
    assertTrue(map.isEmpty());
    String[] vals = populate();
    assertEquals(10, map.size());
    assertFalse(map.isEmpty());
    map.clear();
    assertEquals(0, map.size());
    assertTrue(map.isEmpty());
  }


  @Test
  public void containsKey() {
    String[] vals = populate();
    assertTrue(map.containsKey(5));
    assertFalse(map.containsKey(25));
  }


  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void containsValue() {
    String[] vals = populate();
    assertTrue(map.containsValue(vals[5]));
    assertFalse(map.containsKey("Happy"));
  }


  @Test
  public void entrySet() {
    String[] vals = populate();
    Set<Entry<Integer, Object>> set = map.entrySet();
    assertEquals(10, set.size());
    assertTrue(set.contains(new SimpleEntry<>(5, vals[5])));
    assertFalse(set.contains(new SimpleEntry<>(6, vals[5])));
    assertFalse(set.contains(new SimpleEntry<>(5, vals[6])));

    Set<Entry<Integer, Object>> set2 = new HashSet<>();
    set2.addAll(set);
    assertEquals(set2, set);

    assertEquals(10, set.stream().count());
  }


  @Test
  public void get() {
    String[] vals = populate();
    assertEquals(vals[1], map.get(1));
    assertNull(map.get(25));

    WeakReference<String> ref = new WeakReference<>(vals[1]);
    Arrays.fill(vals, null);
    vals = null;
    do {
      System.gc();
    }
    while (ref.get() != null);
    assertNull(map.get(1));
  }


  @Test
  public void keySet() {
    Set<Integer> set = map.keySet();
    assertEquals(0, set.size());
    String[] vals = populate();
    assertEquals(10, map.size());
    set.remove(4);
    set.remove(14);
    assertEquals(9, set.size());
    assertEquals(9, map.size());
    assertFalse(map.containsKey(4));
  }


  private String[] populate() {
    String[] ret = new String[10];
    for (int i = 0; i < 10; i++) {
      ret[i] = Double.toString(rand.nextDouble());
      map.put(i, ret[i]);
    }
    return ret;
  }


  @Test
  public void put() {
    try {
      map.put(7, null);
      fail();
    } catch (IllegalArgumentException e) {
      // correct
    }
  }


  @Test
  public void putAll() {
    String[] ret = populate();
    HashMap<Integer, Double> all = new HashMap<>();
    for (int i = 5; i < 15; i++) {
      all.put(i, rand.nextDouble());
    }
    map.putAll(all);
    assertEquals(15, map.size());
  }


  @Test
  public void putIfAbsent() {
    String[] ret = populate();
    map.putIfAbsent(0, "Hello");
    map.putIfAbsent(10, "Goodbye");
    assertNotEquals("Hello", map.get(0));
    assertEquals("Goodbye", map.get(10));

    try {
      map.putIfAbsent(17, null);
      fail();
    } catch (IllegalArgumentException e) {
      // correct
    }
  }


  @Test
  public void remove() {
    String[] ret = populate();
    assertFalse(map.remove(1, null));
    assertTrue(map.remove(1, ret[1]));
    assertFalse(map.remove(1, ret[1]));
    assertFalse(map.remove(2, ret[1]));
  }


  @Test
  public void remove1() {
    assertNull(map.remove(11));
    String[] ret = populate();
    assertNull(map.remove(11));
    assertEquals(ret[1], map.remove(1));
  }


  @Test
  public void replace() {
    String[] ret = populate();
    assertFalse(map.replace(1, "foo", "bar"));
    assertFalse(map.replace(1, null, "bar"));
    assertTrue(map.replace(1, ret[1], "bar"));
    try {
      map.replace(1, "bar", null);
      fail();
    } catch (IllegalArgumentException e) {
      // correct
    }
    assertEquals("bar", map.get(1));
  }


  @Test
  public void replace1() {
    String[] ret = populate();
    assertNull(map.replace(11, "Hello"));
    assertNull(map.get(11));
    assertEquals(ret[1], map.replace(1, "Hello"));
    assertEquals("Hello", map.get(1));
    try {
      map.replace(2, null);
      fail();
    } catch (IllegalArgumentException e) {
      // correct
    }
  }


  @Test
  public void values() {
    Collection<Object> coll = map.values();
    assertTrue(coll.isEmpty());
    String[] ret = populate();
    assertFalse(coll.isEmpty());
    assertEquals(10, coll.size());

    assertEquals(10, coll.stream().sorted().count());

    Object[] vals = coll.toArray();
    Arrays.sort(ret);
    Arrays.sort(vals);
    assertTrue(Arrays.equals(ret, vals));
  }

}