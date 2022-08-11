package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 23/08/2018.
 */
public class CopyOnWriteSetTest {

  CopyOnWriteSet<Integer> set = new CopyOnWriteSet<>();


  @Test
  public void add() {
    assertEquals(5, set.size());
    assertFalse(set.add(4));
    assertTrue(set.add(5));
    assertEquals(6, set.size());
  }


  @Test
  public void addAll() {
    HashSet<Integer> set1 = new HashSet<>();
    for (int i = 3; i < 10; i++) {
      set1.add(i);
    }
    assertTrue(set.addAll(set1));
    assertFalse(set.addAll(set1));
    assertEquals(10, set.size());
  }


  @Test
  public void clear() {
    assertFalse(set.isEmpty());
    set.clear();
    assertTrue(set.isEmpty());
    assertEquals(0, set.size());
  }


  @Test
  public void contains() {
    assertTrue(set.contains(3));
    assertFalse(set.contains(53));
  }


  @Test
  public void containsAll() {
    assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));
    assertFalse(set.containsAll(Arrays.asList(4, 5, 6)));
    assertFalse(set.containsAll(Arrays.asList(7, 8, 9)));
  }


  @Test
  public void iterator() {
    Iterator<Integer> iter = set.iterator();
    set.remove(3);
    set.remove(2);

    // The iterator will still show the deleted values
    HashSet<Integer> set1 = new HashSet<>();
    while (iter.hasNext()) {
      set1.add(iter.next());
    }

    assertTrue(set1.contains(3));
    assertTrue(set1.contains(2));
    assertTrue(set1.containsAll(set));
    assertEquals(5, set1.size());
  }


  @Test
  public void iterator2() {
    Iterator<Integer> iter = set.iterator();
    set.remove(2);

    // The iterator will still show the deleted values
    while (iter.hasNext()) {
      int i = iter.next();
      if (i == 1 || i == 0) {
        iter.remove();
      }
    }

    assertEquals(2, set.size());
    assertTrue(set.contains(3));
    assertTrue(set.contains(4));
  }


  @Test
  public void remove() {
    assertTrue(set.remove(3));
    assertFalse(set.remove(33));
    assertEquals(4, set.size());
    assertFalse(set.contains(3));
  }


  @Test
  public void removeAll() {
    assertFalse(set.removeAll(Arrays.asList(7, 8, 9)));
    assertEquals(5, set.size());
    assertTrue(set.removeAll(Arrays.asList(3, 4, 5, 6, 7)));
    assertEquals(3, set.size());
    assertFalse(set.contains(3));
    assertFalse(set.contains(4));
  }


  @Test
  public void replace() {
    HashSet<Integer> rpl = new HashSet<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
    set.replace(rpl);
    assertEquals(10, set.size());
    for (int i = 0; i < 5; i++) {
      assertFalse(set.contains(i));
    }
    for (int i = 10; i < 20; i++) {
      assertTrue(set.contains(i));
    }
  }


  @Test
  public void retainAll() {
    assertTrue(set.retainAll(Arrays.asList(3, 4, 5, 6, 7)));
    assertTrue(set.contains(3));
    assertTrue(set.contains(4));
    assertFalse(set.contains(5));
    assertFalse(set.contains(2));
  }


  @Before
  public void setUp() {
    for (int i = 0; i < 5; i++) {
      set.add(i);
    }
  }


  @Test
  public void testToString() {
    assertNotNull(set.toString());
  }


  @Test
  public void toArray() {
    Object[] vals = set.toArray();
    assertTrue(set.containsAll(Arrays.asList(vals)));
    assertEquals(vals.length, set.size());
  }


  @Test
  public void toArray1() {
    Integer[] vals = set.toArray(new Integer[5]);
    assertTrue(set.containsAll(Arrays.asList(vals)));
    assertEquals(vals.length, set.size());
  }
}