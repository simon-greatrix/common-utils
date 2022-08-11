package com.pippsford.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Correct handling of InterruptedExceptions is often to run all finalizers and terminate the process. This happens naturally when an unchecked exception is
 * raised. Specific handling for InterruptedExceptions can lead to a bloat in boiler-plate code. Converting the exception to a runtime exception can provide
 * the correct handling without the need for specific code.
 *
 * <p>Suggested usage of this class is:</p>
 * <pre>
 *   try {
 *     ... code ..
 *   } catch ( InterruptedException ie ) {
 *     throw new RuntimeInterruptedException(ie);
 *   }
 * </pre>
 *
 * @author Simon Greatrix on 2019-05-07.
 */
public class RuntimeInterruptedException extends RuntimeException {

  private static final Logger logger = LoggerFactory.getLogger(RuntimeInterruptedException.class);


  public RuntimeInterruptedException(InterruptedException cause) {
    super(cause);
    logger.error("Process was interrupted", cause);
  }

}
