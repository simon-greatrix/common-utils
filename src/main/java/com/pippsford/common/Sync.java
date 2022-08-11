package com.pippsford.common;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An object which provides a synchronization holder for another object. Any work on the held object should be protected by a synchronization block on this
 * object.
 *
 * @author Simon Greatrix on 2019-03-22.
 */
public class Sync<V> {

  private V value;


  public Sync() {
    value = null;
  }


  public Sync(V newValue) {
    value = newValue;
  }


  /**
   * Compute a value for this if it's value is currently null.
   *
   * @param supplier the supplier of a value
   *
   * @return the new value
   */
  public V compute(Supplier<V> supplier) {
    synchronized (this) {
      if (value == null && supplier != null) {
        value = supplier.get();
      }
      return value;
    }
  }


  /**
   * Get the value held by this.
   *
   * @return the value
   */
  public V get() {
    synchronized (this) {
      return value;
    }
  }


  /**
   * Run a command synchronized on this.
   *
   * @param command the command to run
   */
  public void run(Consumer<V> command) {
    synchronized (this) {
      command.accept(value);
    }
  }


  /**
   * Set the value held by this.
   *
   * @param newValue the new value
   */
  public void set(V newValue) {
    synchronized (this) {
      value = newValue;
    }
  }


  /**
   * Set the value held by this and notify any waiting threads of a change.
   *
   * @param newValue the new value
   */
  public void setAndNotify(V newValue) {
    synchronized (this) {
      value = newValue;
      notifyAll();
    }
  }


  @Override
  public String toString() {
    return "Sync{" + value + '}';
  }


  /**
   * Wait for the value held by this to match a given condition.
   *
   * @param condition the condition to match
   */
  public void waitFor(Predicate<V> condition) throws InterruptedException {
    synchronized (this) {
      while (!condition.test(value)) {
        wait();
      }
    }
  }

}
