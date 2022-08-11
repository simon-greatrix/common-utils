package com.pippsford.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class MutableIntTest {

  MutableInt mi = new MutableInt(5);


  @Test
  public void add() {
    mi.add(6);
    assertEquals(11, mi.intValue());
  }


  @Test
  public void compareTo() {
    assertEquals(0, mi.compareTo(mi));
    MutableInt mi2 = new MutableInt(7);
    assertTrue(mi.compareTo(mi2) < 0);
    assertTrue(mi2.compareTo(mi) > 0);
  }


  @Test
  public void decrement() {
    mi.decrement();
    assertEquals(4, mi.intValue());
  }


  @Test
  public void doubleValue() {
    assertEquals(5.0, mi.doubleValue(), Math.ulp(5.0));
  }


  @Test
  public void floatValue() {
    assertEquals(5.0f, mi.floatValue(), Math.ulp(5.0f));
  }


  @Test
  public void increment() {
    mi.increment();
    assertEquals(6, mi.intValue());
  }


  @Test
  public void intValue() {
    assertEquals(5, mi.intValue());
  }


  @Test
  public void longValue() {
    assertEquals(5L, mi.longValue());
  }


  @Test
  public void max() {
    mi.max(10);
    mi.max(7);
    assertEquals(10, mi.intValue());
  }


  @Test
  public void min() {
    mi.min(-2);
    mi.min(3);
    assertEquals(-2, mi.intValue());
  }


  @Test
  public void set() {
    mi.set(10);
    assertEquals(10, mi.intValue());
  }


  @Test
  public void testEquals() {
    assertTrue(mi.equals(mi));
    MutableInt m2 = new MutableInt(2);
    assertFalse(mi.equals(m2));
    assertFalse(mi.equals(null));
  }


  @Test
  public void testHashCode() {
    MutableInt m2 = new MutableInt(2);
    assertTrue(mi.hashCode() != m2.hashCode());
  }


  @Test
  public void testToString() {
    assertNotNull(mi.toString());
  }


  @Test
  public void waitFor() throws InterruptedException {
    MutableInt m2 = new MutableInt(2);
    m2.waitFor(2);
  }


  @Test
  public void waitFor2() throws InterruptedException {
    MutableInt m2 = new MutableInt(2);
    Thread thread = new Thread(() -> {
      try {
        m2.waitFor(3);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread.start();
    m2.set(1);
    m2.set(2);
    m2.set(3);
    thread.join(60_000);
    if (thread.isAlive()) {
      thread.interrupt();
      fail();
    }
  }


  @Test
  public void waitWhile() throws InterruptedException {
    MutableInt m2 = new MutableInt();
    Thread thread = new Thread(() -> {
      try {
        m2.waitWhile(i -> i != 3);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread.start();
    m2.set(1);
    m2.set(2);
    m2.set(3);
    thread.join(60_000);
    if (thread.isAlive()) {
      thread.interrupt();
      fail();
    }
  }

}
