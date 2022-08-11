package com.pippsford.common;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A mutable object reference. Useful for passing objects out of lambdas.
 *
 * @author Simon Greatrix on 13/02/2018.
 */
public class MutableObject<T> {

  /** The actual value. */
  private T value;


  /**
   * New instance with an initial value of <code>null</code>.
   */
  public MutableObject() {
    value = null;
  }


  /**
   * New instance wrapping the provided value.
   *
   * @param value the initial value
   */
  public MutableObject(T value) {
    this.value = value;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MutableObject<?> that = (MutableObject<?>) o;

    return Objects.equals(get(), that.get());
  }


  public synchronized T get() {
    return value;
  }


  @Override
  public int hashCode() {
    T v = get();
    return v != null ? v.hashCode() : 0;
  }


  public synchronized void set(T newValue) {
    value = newValue;
    notifyAll();
  }


  @Override
  public String toString() {
    return "MutableObject(" + value + ")";
  }


  /**
   * Wait for this to acquire the expected value.
   *
   * @param expected the expected value
   *
   * @throws InterruptedException if interrupted
   */
  public synchronized void waitFor(T expected) throws InterruptedException {
    while (!Objects.equals(expected, value)) {
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
  public synchronized void waitWhile(Predicate<T> predicate) throws InterruptedException {
    while (predicate.test(value)) {
      wait();
    }
  }

}
