package com.pippsford.util;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Equivalent to java.io.UncheckedIOException.
 *
 * @deprecated Use UncheckedIOException instead.
 */
@Deprecated
public class RuntimeIOException extends UncheckedIOException {

  private static final long serialVersionUID = 5165589461489000669L;


  public RuntimeIOException(IOException ioe) {
    super(ioe.getMessage(), ioe);
  }

}
