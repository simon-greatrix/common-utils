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
public class MutableObjectTest {

  MutableObject<String> mo = new MutableObject<>("wibble");


  @Test
  public void get() {
    assertEquals("wibble", mo.get());
  }


  @Test
  public void set() {
    mo.set("wobble");
    assertEquals("wobble", mo.get());
  }


  @Test
  public void testEquals() {
    assertTrue(mo.equals(mo));
    assertFalse(mo.equals(new MutableObject<>("wobble")));
    assertTrue(mo.equals(new MutableObject<>("wibble")));
    assertFalse(mo.equals(null));
  }


  @Test
  public void testHashCode() {
    assertNotEquals(new MutableObject<>("wobble").hashCode(), mo.hashCode());
    assertEquals(new MutableObject<>("wibble").hashCode(), mo.hashCode());
  }


  @Test
  public void testToString() {
    assertNotNull(mo.toString());
  }


  @Test
  public void waitFor() throws InterruptedException {
    MutableObject<String> mo2 = new MutableObject<>();
    mo2.waitFor(null);
  }


  @Test
  public void waitFor2() throws InterruptedException {
    MutableObject<String> mo2 = new MutableObject<>();
    Thread thread = new Thread(() -> {
      try {
        mo2.waitFor("dog");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread.start();
    mo2.set("cat");
    mo2.set("dog");
    thread.join(60_000);
    if (thread.isAlive()) {
      thread.interrupt();
      fail();
    }
  }


  @Test
  public void waitWhile() throws InterruptedException {
    MutableObject<String> mo2 = new MutableObject<>();
    Thread thread = new Thread(() -> {
      try {
        mo2.waitWhile(i -> i == null);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    thread.start();
    mo2.set(null);
    mo2.set("dog");
    thread.join(60_000);
    if (thread.isAlive()) {
      thread.interrupt();
      fail();
    }
  }

}
