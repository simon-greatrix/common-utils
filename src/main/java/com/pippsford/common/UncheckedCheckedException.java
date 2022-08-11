package com.pippsford.common;

import java.util.Objects;

/**
 * A wrapper for a checked exception where the API requires only unchecked exceptions may be thrown.
 *
 * @author Simon Greatrix on 06/01/2021.
 */
public class UncheckedCheckedException extends RuntimeException {

  /**
   * New instance.
   *
   * @param throwable the checked exception
   */
  public UncheckedCheckedException(Exception throwable) {
    super(Objects.requireNonNull(throwable));
  }


  /**
   * New instance.
   *
   * @param throwable the checked exception
   */
  public UncheckedCheckedException(String message, Exception throwable) {
    super(message, Objects.requireNonNull((throwable)));
  }


  @Override
  public synchronized Exception getCause() {
    return (Exception) super.getCause();
  }

}
