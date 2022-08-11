package com.pippsford.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class TypeSafeListTest {

  TypeSafeList<Integer> list = new TypeSafeList<>(Integer.class);


  @Test
  public void add() {
    list.add(1);
    assertEquals(1, list.size());
    assertEquals(Integer.valueOf(1), list.get(0));
  }


  @Test
  public void addAll() {
    list.addAll(Arrays.asList(1, 2, 3));
    assertEquals(3, list.size());
  }


  @Test(expected = ClassCastException.class)
  public void addObject() {
    list.addObject(2, "foo");
  }


  @Test
  public void checkedCopyOf() {
    assertSame(list, list.checkedCopyOf(list));

    TypeSafeList<Integer> list2 = new TypeSafeList<>(TypeSafeList.F_INT);
    list2.addObject(0, "1");
    Collection<Integer> l = list.checkedCopyOf(list2);
    assertEquals(list2, l);
  }


  @Test
  public void clear() {
    list.add(1);
    list.clear();
    assertTrue(list.isEmpty());
  }


  @Test
  public void contains() {
    list.add(1);
    assertTrue(list.contains(1));
    assertFalse(list.contains(20));
  }


  @Test
  public void containsAll() {
    list.addAll(Arrays.asList(1, 2, 3, 4));
    assertTrue(list.containsAll(Set.of(1, 3, 4)));
  }


  @Test
  public void forEach() {
    list.addAll(Arrays.asList(1, 2, 3));
    StringBuilder buf = new StringBuilder();
    list.forEach(i -> buf.append(i));
    assertEquals("123", buf.toString());
  }


  @Test
  public void getConvertor() {
    assertNotNull(list.getConvertor());
  }


  @Test
  public void indexOf() {
    list.addAll(Arrays.asList(1, 2, 3));
    assertEquals(1, list.indexOf(2));
    assertEquals(-1, list.indexOf(5));
  }


  @Test
  public void isEmpty() {
    assertTrue(list.isEmpty());
    list.add(1);
    assertFalse(list.isEmpty());
  }


  @Test
  public void iterator() {
    list.addAll(Arrays.asList(1, 2, 3));
    Iterator<Integer> iterator = list.iterator();
    assertTrue(iterator.hasNext());
    assertEquals(Integer.valueOf(1), iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals(Integer.valueOf(2), iterator.next());
    iterator.remove();
    assertTrue(iterator.hasNext());
    assertEquals(Integer.valueOf(3), iterator.next());
    assertFalse(iterator.hasNext());

    assertTrue(list.contains(1));
    assertFalse(list.contains(2));
    assertTrue(list.contains(3));
  }


  @Test
  public void lastIndexOf() {
    list.addAll(Arrays.asList(1, 2, 3, 2, 1));
    assertEquals(4, list.lastIndexOf(1));
    assertEquals(-1, list.lastIndexOf(5));
  }


  @Test
  public void listIterator() {
    list.addAll(Arrays.asList(1, 2, 3));
    ListIterator<Integer> iterator = list.listIterator();
    assertTrue(iterator.hasNext());
    assertFalse(iterator.hasPrevious());
    assertEquals(-1, iterator.previousIndex());
    assertEquals(0, iterator.nextIndex());
    iterator.add(0);
    assertEquals(1, iterator.nextIndex());
    assertTrue(iterator.hasPrevious());
    iterator.next();
    iterator.remove();
    iterator.next();
    iterator.set(5);

    // added 0, removed 1, changed 2 to 5:
    assertEquals("[0, 5, 3]", list.toString());
  }


  @Test
  public void parallelStream() {
    assertNotNull(list.parallelStream());
  }


  @Test
  public void remove() {
    list.addAll(Arrays.asList(1, 2, 3));
    list.remove(Integer.valueOf(2));
    assertEquals("[1, 3]", list.toString());
  }


  @Test
  public void removeAll() {
    list.addAll(Arrays.asList(1, 2, 3));
    list.removeAll(Set.of(2, 3, 4, 5, 6));
    assertEquals(1, list.size());
  }


  @Test
  public void removeIf() {
    list.addAll(Arrays.asList(1, 2, 3));
    list.removeIf(i -> (i.intValue() & 1) == 1);
    assertEquals(1, list.size());
    assertEquals(Integer.valueOf(2), list.get(0));
  }


  @Test
  public void replaceAll() {
    list.addAll(Arrays.asList(1, 2, 3));
    list.replaceAll(i -> 2 * i);
    assertEquals("[2, 4, 6]", list.toString());
  }


  @Test
  public void retainAll() {
    list.addAll(Arrays.asList(1, 2, 3));
    list.retainAll(Set.of(2, 3, 4, 5, 6));
    assertEquals(2, list.size());
  }


  @Test
  public void set() {
    list.addAll(Arrays.asList(1, 2, 3));
    list.set(1, 10);
    assertEquals("[1, 10, 3]", list.toString());
  }


  @Test
  public void sort() {
    list.addAll(Arrays.asList(4, 2, 1, 3));
    list.sort(Comparator.naturalOrder());
    assertEquals("[1, 2, 3, 4]", list.toString());
  }


  @Test
  public void spliterator() {
    assertNotNull(list.spliterator());
  }


  @Test
  public void stream() {
    assertNotNull(list.stream());
  }


  @Test(expected = IndexOutOfBoundsException.class)
  public void subList() {
    assertNotNull(list.subList(1, 20));
  }


  @Test
  public void testAdd() {
    list.add(1);
    list.add(0, 2);
    assertEquals(2, list.size());
    assertEquals(Integer.valueOf(2), list.get(0));
  }


  @Test
  public void testAddAll() {
    list.addAll(0, Arrays.asList(1, 2, 3));
    assertEquals(3, list.size());
  }


  @Test
  public void testHashCode() {
    int h = list.hashCode();
    list.add(1);
    assertNotEquals(h, list.hashCode());
  }


  @Test
  public void testRemove() {
    list.addAll(Arrays.asList(1, 2, 3));
    list.remove(0);
    assertEquals("[2, 3]", list.toString());
  }


  @Test
  public void testToArray() {
    assertNotNull(list.toArray());
  }


  @Test
  public void toArray() {
    assertNotNull(list.toArray(new Integer[0]));
  }


  @Test
  public void typeCheck() {
  }
}