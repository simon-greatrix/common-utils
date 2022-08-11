package com.pippsford.common;

import java.util.function.Predicate;

/**
 * A boolean that can be mutated.
 *
 * @author Simon Greatrix on 01/02/2018.
 */
public class MutableBoolean {

  private static final long serialVersionUID = 1L;

  /** This instance's value. */
  private boolean value;


  /** New instance with the initial value of false. */
  public MutableBoolean() {
    this(false);
  }


  /**
   * New instance with the given initial value.
   *
   * @param value the initial value
   */
  public MutableBoolean(boolean value) {
    this.value = value;
  }


  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof MutableBoolean) {
      return ((MutableBoolean) o).get() == get();
    }
    return false;
  }


  public synchronized boolean get() {
    return value;
  }


  @Override
  public int hashCode() {
    return Boolean.hashCode(get());
  }


  /**
   * Set the value to a specific value.
   *
   * @param newValue the new value
   */
  public synchronized void set(boolean newValue) {
    value = newValue;
    notifyAll();
  }


  @Override
  public String toString() {
    return "MutableBoolean(" + value + ")";
  }


  /**
   * Wait for this to acquire the expected value.
   *
   * @param expected the expected value
   *
   * @throws InterruptedException if interrupted
   */
  public synchronized void waitFor(boolean expected) throws InterruptedException {
    while (expected != value) {
      wait();
    }
  }


  /**
   * Wait while the predicate returns true.
   *
   * @param predicate the predicate
   *
   * @throws InterruptedException if interrupted
   */
  public synchronized void waitWhile(Predicate<Boolean> predicate) throws InterruptedException {
    while (predicate.test(value)) {
      wait();
    }
  }

}
