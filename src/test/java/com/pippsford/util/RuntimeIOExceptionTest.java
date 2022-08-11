package com.pippsford.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class RuntimeIOExceptionTest {

  @Test
  public void test() {
    IOException ie = new IOException();
    RuntimeIOException e = new RuntimeIOException(ie);
    assertEquals(ie, e.getCause());
  }

}