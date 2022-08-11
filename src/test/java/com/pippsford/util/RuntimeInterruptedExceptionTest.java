package com.pippsford.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class RuntimeInterruptedExceptionTest {

  @Test
  public void test() {
    InterruptedException ie = new InterruptedException();
    RuntimeInterruptedException e = new RuntimeInterruptedException(ie);
    assertEquals(ie, e.getCause());
  }

}