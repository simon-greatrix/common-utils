package com.pippsford.common;

/**
 * An auto-closeable that can be disabled.
 *
 * @author Simon Greatrix on 07/07/2021.
 */
public class AutoCloser implements AutoCloseable, Runnable {

  private UncheckedCloseable closeable = UncheckedCloseable.wrap(() -> {
    // do nothing
  });

  private boolean enabled = true;


  public AutoCloser() {
    // accept defaults
  }


  /**
   * Create a new instance which invokes the same auto-closeable and is enabled.
   *
   * @param toCopy the original
   */
  public AutoCloser(AutoCloser toCopy) {
    this.closeable = toCopy.closeable;
  }


  /**
   * Close whatever this auto-closer was to close, provided this is enabled. After the close is performed this will not be enabled, so multiple calls will only
   * invoke close the first time.
   */
  @Override
  public void close() throws Exception {
    if (enabled) {
      enabled = false;
      closeable.close();
    }
  }


  /**
   * Create a copy of this. The copy will invoke the same auto-closeable and is enabled.
   *
   * @return a new instance
   */
  public AutoCloser copy() {
    return new AutoCloser(this);
  }


  public boolean isEnabled() {
    return enabled;
  }


  /**
   * Nest the specified auto-closeable within this.
   *
   * @param value the auto-closeable
   * @param <T>   the type of the auto-closeable
   *
   * @return the value
   */
  public <T extends AutoCloseable> T nest(T value) {
    closeable = closeable.nest(value);
    return value;
  }


  @Override
  public void run() {
    try {
      close();
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new UncheckedCheckedException(e);
    }
  }


  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

}
