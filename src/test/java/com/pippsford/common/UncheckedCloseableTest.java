package com.pippsford.common;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Test;

/**
 * @author Simon Greatrix on 07/07/2021.
 */
public class UncheckedCloseableTest {

  class Closeable implements AutoCloseable {

    private final boolean doFail;

    int closeCount = 0;

    int id = -1;


    public Closeable(boolean doFail) {
      this.doFail = doFail;
    }


    public void close() throws Exception {
      closeCount++;
      id = idSrc.getAndIncrement();
      if (doFail) {
        throw new IOException("Test failure:"+id);
      }
    }

  }



  private AtomicInteger idSrc = new AtomicInteger(1);


  @Test
  public void test1() throws Exception {
    Closeable c1 = new Closeable(false);
    Closeable c2 = new Closeable(true);
    Closeable c3 = new Closeable(false);

    UncheckedCloseable uc = UncheckedCloseable.wrap(c3).nest(c2).nest(c1);
    try {
      uc.close();
    } catch (Exception e) {
      if (!((e instanceof IOException) && e.getMessage().equals("Test failure:2"))) {
        throw e;
      }
    }

    assertEquals(1, c1.id);
    assertEquals(1, c1.closeCount);
    assertEquals(2, c2.id);
    assertEquals(1, c2.closeCount);
    assertEquals(3, c3.id);
    assertEquals(1, c3.closeCount);
  }

  @Test
  public void test2() throws Exception {
    Closeable c1 = new Closeable(true);
    Closeable c2 = new Closeable(true);
    Closeable c3 = new Closeable(true);
    UncheckedCloseable uc = UncheckedCloseable.wrap(c3).nest(c2).nest(c1);

    IntStream stream = IntStream.of(1,2,3,4).onClose(uc);
    int sum = stream.sum();

    try {
      stream.close();
    } catch ( UncheckedCheckedException uce ) {
      // this is OK
    }

    assertEquals(10,sum);

    assertEquals(1, c1.id);
    assertEquals(1, c1.closeCount);
    assertEquals(2, c2.id);
    assertEquals(1, c2.closeCount);
    assertEquals(3, c3.id);
    assertEquals(1, c3.closeCount);
  }
}