package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 23/08/2018.
 */
public class LRUSetTest {

  LRUSet<Integer> set = new LRUSet<>(10);


  @Test
  public void add() {
    assertTrue(set.add(20));
    assertTrue(set.contains(20));
    assertFalse(set.add(20));
  }


  @Test
  public void addAll() throws Exception {
    ArrayList<Integer> toAdd = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      toAdd.add(i);
    }

    assertTrue(set.addAll(toAdd));
    Thread c = set.cleaner;
    if (c != null) {
      c.join();
    }
    assertTrue(set.size() <= 10);
  }


  @Test
  public void clear() {
    assertFalse(set.isEmpty());
    set.clear();
    assertTrue(set.isEmpty());
  }


  @Test
  public void contains() {
    assertTrue(set.contains(5));
    assertFalse(set.contains(25));
  }


  @Test
  public void containsAll() {
    HashSet<Integer> set1 = new HashSet<>();
    for (int i = 0; i < 5; i++) {
      set1.add(i);
    }

    assertTrue(set.containsAll(set1));

    set1.add(30);
    assertFalse(set.containsAll(set1));
  }


  @Test
  public void iterator() {
    Iterator<Integer> iter = set.iterator();
    int j = 0;
    while (iter.hasNext()) {
      int i = iter.next();
      if ((i & 1) == 1) {
        iter.remove();
      }
      j++;
    }
    assertEquals(10, j);
    assertEquals(5, set.size());
  }


  @Test
  public void remove() {
    assertTrue(set.remove(5));
    assertFalse(set.remove(55));
    assertFalse(set.contains(5));
    assertEquals(9, set.size());
  }


  @Test
  public void removeAll() {
    HashSet<Integer> set1 = new HashSet<>();
    for (int i = 0; i < 5; i++) {
      set1.add(i);
      set1.add(i + 50);
    }

    set.removeAll(set1);
    assertFalse(set.contains(0));
    assertTrue(set.contains(5));
    assertEquals(5, set.size());
  }


  @Test
  public void retainAll() {
    HashSet<Integer> set1 = new HashSet<>();
    for (int i = 0; i < 7; i++) {
      set1.add(i);
      set1.add(i + 50);
    }

    set.retainAll(set1);
    assertTrue(set.contains(0));
    assertFalse(set.contains(7));
    assertEquals(7, set.size());
  }


  @Before
  public void setUp() {
    for (int i = 0; i < 10; i++) {
      set.add(i);
    }
  }


  @Test
  public void toArray() {
    Integer[] i1 = new Integer[set.size()];
    Integer[] i2 = set.toArray(i1);
    assertSame(i1, i2);

    Object[] oa = set.toArray();
    Arrays.sort(oa);
    Arrays.sort(i1);
    assertTrue(Arrays.equals(oa, i1));
  }
}