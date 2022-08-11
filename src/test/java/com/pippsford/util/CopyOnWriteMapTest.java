package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 06/07/2017.
 */
public class CopyOnWriteMapTest {

  private static final TreeMap<Integer, String> ROMAN = new TreeMap<>();


  /**
   * Create a roman number.
   *
   * @param number the number
   *
   * @return the roman equivalent
   */
  public static String roman(int number) {
    int l = ROMAN.floorKey(number);
    if (number == l) {
      return ROMAN.get(number);
    }
    return ROMAN.get(l) + roman(number - l);
  }


  static {
    int[] ri = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4,
        1};
    String[] rs = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL",
        "X", "IX", "V", "IV", "I"};
    for (int i = 0; i < ri.length; i++) {
      ROMAN.put(ri[i], rs[i]);
    }
  }

  final CopyOnWriteMap<Integer, String> map = new CopyOnWriteMap<>();


  @Test
  public void clear() throws Exception {
    assertFalse(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());
  }


  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void containsKey() throws Exception {
    assertTrue(map.containsKey(1));
    assertFalse(map.containsKey("Foo"));
  }


  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void containsValue() throws Exception {
    assertTrue(map.containsValue(roman(2)));
    assertFalse(map.containsValue(Math.PI));
  }


  @Test
  public void entrySet() throws Exception {
    Set<Entry<Integer, String>> set = map.entrySet();
    assertEquals(map.size(), set.size());

    // putting and clearing the map, does not affect the set
    map.put(5, "five");
    assertEquals(4, set.size());
    map.clear();
    assertEquals(4, set.size());
  }


  @Test
  public void get() throws Exception {
    assertEquals(roman(1), map.get(1));
    assertNull(map.get(87));
  }


  @Test
  public void keySet() throws Exception {
    Set<Integer> set = map.keySet();
    assertEquals(map.size(), set.size());
    assertTrue(set.contains(1));
    assertTrue(set.contains(3));

    // putting and clearing the map, does not affect the set
    map.put(5, roman(5));
    assertEquals(4, set.size());
    map.clear();
    assertEquals(4, set.size());
  }


  @Test
  public void put() throws Exception {
    assertFalse(map.containsKey(8));
    map.put(8, roman(8));
    assertTrue(map.containsKey(8));
  }


  @Test
  public void putAll() throws Exception {
    Map<Integer, String> insert = new HashMap<>();
    for (int i = 'a'; i <= 'z'; i++) {
      insert.put(i, roman(i));
    }

    final Set<Integer> initial = map.keySet();
    map.putAll(null);
    map.putAll(Collections.emptyMap());
    map.putAll(insert);
    Set<Integer> after = map.keySet();
    assertEquals(initial.size() + 26, after.size());
  }


  @Test
  public void putIfAbsent() throws Exception {
    String s = map.putIfAbsent(17, roman(17));
    assertNull(s);

    map.put(16, roman(16));
    s = map.putIfAbsent(16, "don't use");
    assertEquals(roman(16), s);
  }


  @Test
  public void remove() throws Exception {
    map.remove(1);
    assertFalse(map.containsKey(1));
  }


  @Test
  public void remove1() throws Exception {
    map.remove(2, "not right");
    assertTrue(map.containsKey(2));
    map.remove(2, roman(2));
    assertFalse(map.containsKey(2));
  }


  @Test
  public void removeAll() throws Exception {
    Map<Integer, String> remove = new HashMap<>();
    remove.put(3, roman(3));
    remove.put(4, roman(4));
    remove.put(1, "ONE");
    remove.put(6, roman(6));
    int x = map.removeAll(remove);
    assertEquals(x, 2);
    assertEquals(2, map.size());
  }


  @Test
  public void removeAll1() throws Exception {
    Set<Integer> remove = new HashSet<>();
    for (int i = 3; i < 7; i++) {
      remove.add(i);
    }
    int x = map.removeAll(remove);
    assertEquals(2, x);
    assertEquals(2, map.size());
  }


  @SuppressWarnings("ConstantConditions")
  @Test
  public void replace() throws Exception {
    map.replace(3, "THREE");
    map.replace(6, null, roman(6));
    assertTrue(map.containsKey(3));
    assertFalse(map.containsKey(6));
    assertEquals("THREE", map.get(3));
  }


  @Test
  public void replace1() throws Exception {
    map.replace(3, roman(3), "THREE");
    map.replace(6, roman(6));
    assertTrue(map.containsKey(3));
    assertFalse(map.containsKey(6));
    assertEquals("THREE", map.get(3));
  }


  @Test
  public void replace2() throws Exception {
    Map<Integer, String> original = new HashMap<>();
    original.putAll(map);

    Map<Integer, String> newValues = new HashMap<>();
    for (int i = 26; i <= 50; i++) {
      newValues.put(i, roman(i));
    }

    assertFalse(!map.equals(original));
    assertTrue(!map.equals(newValues));
    map.replace(newValues);
    assertFalse(map.equals(original));
    assertTrue(map.equals(newValues));
  }


  /** Setup for tests. */
  @Before
  public void setUp() {
    for (int i = 1; i <= 4; i++) {
      map.put(i, roman(i));
    }
  }


  @Test
  public void testHashCode() {
    Map<Integer, String> newValues = new HashMap<>();
    for (int i = 26; i <= 50; i++) {
      newValues.put(i, roman(i));
    }

    int hc = newValues.hashCode();
    assertNotEquals(hc, map.hashCode());
    map.replace(newValues);
    assertEquals(hc, map.hashCode());
  }


  @Test
  public void testToString() {
    Map<Integer, String> newValues = new HashMap<>();
    for (int i = 26; i <= 50; i++) {
      newValues.put(i, roman(i));
    }

    String s = newValues.toString();
    assertNotEquals(s, map.toString());
    map.replace(newValues);
    assertEquals(s, map.toString());
  }


  @Test
  public void values() throws Exception {
    Collection<String> vals = map.values();
    assertEquals(map.size(), vals.size());
    map.clear();
    assertEquals(4, vals.size());
  }
}