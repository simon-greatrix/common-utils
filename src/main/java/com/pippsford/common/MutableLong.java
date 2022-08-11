package com.pippsford.common;

import java.util.function.LongPredicate;

/**
 * A long that can be mutated.
 *
 * @author Simon Greatrix on 01/02/2018.
 */
public class MutableLong extends Number implements Comparable<MutableLong> {

  private static final long serialVersionUID = 1L;

  /** This instance's value. */
  private long value;


  /** New instance with the initial value of zero. */
  public MutableLong() {
    this(0);
  }


  /**
   * New instance with the given initial value.
   *
   * @param value the initial value
   */
  public MutableLong(long value) {
    this.value = value;
  }


  /**
   * Increase the value by the given amount, returning the new value.
   *
   * @param delta the change
   *
   * @return the new value
   */
  public synchronized long add(long delta) {
    value += delta;
    notifyAll();
    return value;
  }


  @Override
  public int compareTo(MutableLong o) {
    return Long.compare(get(), o.get());
  }


  /**
   * Decrease the value by 1 and return the new value.
   *
   * @return the new value
   */
  public long decrement() {
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
    if (o instanceof MutableLong) {
      return ((MutableLong) o).get() == get();
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
  public synchronized long get() {
    return value;
  }


  @Override
  public int hashCode() {
    return Long.hashCode(get());
  }


  /**
   * Increase the value by and return the new value.
   *
   * @return the new value
   */
  public long increment() {
    return add(1);
  }


  @Override
  public int intValue() {
    return (int) get();
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
  public synchronized void max(long v) {
    if (v > get()) {
      set(v);
    }
  }


  /**
   * Set the value of this to the minima of its current value and the provided value.
   *
   * @param v the value
   */
  public synchronized void min(long v) {
    if (v < get()) {
      set(v);
    }
  }


  /**
   * Set the value to a specific value.
   *
   * @param newValue the new value
   */
  public synchronized void set(long newValue) {
    value = newValue;
    notifyAll();
  }


  @Override
  public String toString() {
    return "MutableLong(" + value + ")";
  }


  /**
   * Wait for this to acquire the expected value.
   *
   * @param expected the expected value
   *
   * @throws InterruptedException if interrupted
   */
  public synchronized void waitFor(long expected) throws InterruptedException {
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
  public synchronized void waitWhile(LongPredicate predicate) throws InterruptedException {
    while (predicate.test(value)) {
      wait();
    }
  }

}
