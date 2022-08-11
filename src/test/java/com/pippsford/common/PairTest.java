package com.pippsford.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class PairTest {

  Pair<Integer, Integer> pair = new Pair<>(1, 2);


  @Test
  public void left() {
    assertEquals(Integer.valueOf(1), pair.left());
  }


  @Test
  public void right() {
    assertEquals(Integer.valueOf(2), pair.right());
  }


  @Test
  public void set() {
    pair.set(3, 4);
    assertEquals(Integer.valueOf(3), pair.left());
    assertEquals(Integer.valueOf(4), pair.right());
  }


  @Test
  public void setLeft() {
    pair.setLeft(3);
    assertEquals(Integer.valueOf(3), pair.left());
    assertEquals(Integer.valueOf(2), pair.right());
  }


  @Test
  public void setRight() {
    pair.setRight(4);
    assertEquals(Integer.valueOf(1), pair.left());
    assertEquals(Integer.valueOf(4), pair.right());
  }


  @Test
  public void testEquals() {
    assertFalse(pair.equals(new Pair<>()));
    assertFalse(pair.equals(new Pair<>(2, 1)));
    assertTrue(pair.equals(new Pair<>(1, 2)));
    assertTrue(pair.equals(pair));
  }


  @Test
  public void testHashCode() {
    assertNotEquals(pair.hashCode(), new Pair().hashCode());
  }


  @Test
  public void testToString() {
    assertNotNull(pair.toString());
  }
}