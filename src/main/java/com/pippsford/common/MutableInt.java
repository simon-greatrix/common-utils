package com.pippsford.common;

import java.util.function.IntPredicate;

/**
 * An integer that can be mutated.
 *
 * @author Simon Greatrix on 01/02/2018.
 */
public class MutableInt extends Number implements Comparable<MutableInt> {

  private static final long serialVersionUID = 1L;

  /** This instance's value. */
  private int value;


  /** New instance with the initial value of zero. */
  public MutableInt() {
    this(0);
  }


  /**
   * New instance with the given initial value.
   *
   * @param value the initial value
   */
  public MutableInt(int value) {
    this.value = value;
  }


  /**
   * Increase the value by the given amount, returning the new value.
   *
   * @param delta the change
   *
   * @return the new value
   */
  public synchronized int add(int delta) {
    value += delta;
    notifyAll();
    return value;
  }


  @Override
  public int compareTo(MutableInt o) {
    return Integer.compare(get(), o.get());
  }


  /**
   * Decrease the value by 1 and return the new value.
   *
   * @return the new value
   */
  public int decrement() {
    return add(-1);
  }


  @Override
  public double doubleValue() {
    return get();
  }


  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof MutableInt) {
      return ((MutableInt) o).get() == get();
    }
    return false;
  }


  @Override
  public float floatValue() {
    return get();
  }


  /**
   * Get the value of this.
   *
   * @return this instance's value
   */
  public synchronized int get() {
    return value;
  }


  @Override
  public int hashCode() {
    return Integer.hashCode(get());
  }


  /**
   * Increase the value by and return the new value.
   *
   * @return the new value
   */
  public int increment() {
    return add(1);
  }


  @Override
  public int intValue() {
    return get();
  }


  @Override
  public long longValue() {
    return get();
  }


  /**
   * Set the value of this to the maxima of its current value and the provided value.
   *
   * @param v the value
   */
  public synchronized void max(int v) {
    if (v > get()) {
      set(v);
    }
  }


  /**
   * Set the value of this to the minima of its current value and the provided value.
   *
   * @param v the value
   */
  public synchronized void min(int v) {
    if (v < get()) {
      set(v);
    }
  }


  /**
   * Set the value to a specific value.
   *
   * @param newValue the new value
   */
  public synchronized void set(int newValue) {
    value = newValue;
    notifyAll();
  }


  @Override
  public String toString() {
    return "MutableInt(" + value + ")";
  }


  /**
   * Wait for this to acquire the expected value.
   *
   * @param expected the expected value
   *
   * @throws InterruptedException if interrupted
   */
  public synchronized void waitFor(int expected) throws InterruptedException {
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
  public synchronized void waitWhile(IntPredicate predicate) throws InterruptedException {
    while (predicate.test(value)) {
      wait();
    }
  }

}
