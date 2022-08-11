package com.pippsford.util;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class SimpleInvocationHandlerTest {

  static class HasASize {

    public String isEmpty() {
      return "false";
    }


    public int size() {
      return 5;
    }
  }


  @Test(expected = NoSuchMethodError.class)
  public void invoke() {
    Set<?> set = SimpleInvocationHandler.newProxy(Set.class, new HasASize());
    set.contains("f");
  }


  @Test(expected = NoSuchMethodError.class)
  public void invoke2() {
    Set<?> set = SimpleInvocationHandler.newProxy(Set.class, new HasASize());
    set.isEmpty();
  }


  @Test
  public void newProxy() {
    Set<?> set = SimpleInvocationHandler.newProxy(Set.class, new HasASize());
    assertEquals(5, set.size());
  }
}