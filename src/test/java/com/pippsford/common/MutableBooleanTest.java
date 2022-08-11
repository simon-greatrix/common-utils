package com.pippsford.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class MutableBooleanTest {

  @Test
  public void get() {
    MutableBoolean mb1 = new MutableBoolean(true);
    assertTrue(mb1.get());
  }


  @Test
  public void set() {
    MutableBoolean mb1 = new MutableBoolean();
    mb1.set(true);
    assertTrue(mb1.get());
    mb1.set(false);
    assertFalse(mb1.get());
  }


  @Test
  public void testEquals() {
    MutableBoolean mb1 = new MutableBoolean(true);
    MutableBoolean mb2 = new MutableBoolean(false);
    MutableBoolean mb3 = new MutableBoolean(true);
    MutableBoolean mb4 = new MutableBoolean(false);
    assertTrue(mb1.equals(mb1));
    assertTrue(mb1.equals(mb3));
    assertTrue(mb2.equals(mb2));
    assertTrue(mb2.equals(mb4));
    assertFalse(mb1.equals(mb2));
    assertFalse(mb1.equals(null));
  }


  @Test
  public void testHashCode() {
    MutableBoolean mb1 = new MutableBoolean(true);
    MutableBoolean mb2 = new MutableBoolean(false);
    assertNotEquals(mb1.hashCode(), mb2.hashCode());

    mb2.set(true);
    assertEquals(mb1.hashCode(), mb2.hashCode());
  }


  @Test
  public void testToString() {
    assertNotNull(new MutableBoolean(true).toString());
  }


  @Test
  public void waitFor() throws InterruptedException {
    MutableBoolean mb1 = new MutableBoolean(true);
    mb1.waitFor(true);
  }


  @Test
  public void waitFor2() throws InterruptedException {
    MutableBoolean mb1 = new MutableBoolean(true);
    Thread thread = new Thread(() -> {
      try {
        mb1.waitFor(false);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread.start();
    mb1.set(true);
    mb1.set(true);
    mb1.set(false);
    thread.join(60_000);
    if (thread.isAlive()) {
      thread.interrupt();
      fail();
    }
  }


  @Test
  public void waitWhile() throws InterruptedException {
    MutableBoolean mb1 = new MutableBoolean(true);
    Thread thread = new Thread(() -> {
      try {
        mb1.waitWhile(Boolean::booleanValue);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread.start();
    mb1.set(true);
    mb1.set(true);
    mb1.set(false);
    thread.join(60_000);
    if (thread.isAlive()) {
      thread.interrupt();
      fail();
    }
  }

}
