package com.pippsford.common;

/**
 * A functional interface that facilitates the chaining of "close" calls.
 *
 * @author Simon Greatrix on 07/07/2021.
 */
@FunctionalInterface
public interface UncheckedCloseable extends Runnable, AutoCloseable {

  /**
   * Wrap an {@link AutoCloseable} as an {@link UncheckedCloseable}.
   *
   * @param c the closeable to wrap
   *
   * @return the new wrapper
   */
  static UncheckedCloseable wrap(AutoCloseable c) {
    if (c instanceof UncheckedCloseable) {
      return (UncheckedCloseable) c;
    }
    return c::close;
  }


  /**
   * Nest another closeable within this. The {@code close()} method on the nested instance will be invoked prior to the {@code close()} method on this instance.
   *
   * @param c the closeable to nest within this
   *
   * @return a new closeable that implements the nesting
   */
  default UncheckedCloseable nest(AutoCloseable c) {
    return () -> {
      try (UncheckedCloseable c1 = this) {
        c.close();
      }
    };
  }


  /**
   * Invoke the close operation. If the wrapped {@code close()} methods fails with a checked exception, then it will be wrapped in a {@link
   * UncheckedCheckedException}.
   */
  default void run() {
    try {
      close();
    } catch (RuntimeException runtimeException) {
      throw runtimeException;
    } catch (Exception ex) {
      throw new UncheckedCheckedException(ex);
    }
  }

}
