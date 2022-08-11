package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;

/**
 * @author Simon Greatrix on 12/07/2017.
 */
public class CriticalTaskTest {


  @Test
  public void await() throws Exception {
    final Object lock = new Object();
    try {
      CriticalTask.await(lock);
      fail();
    } catch (AssertionError e) {
      // correct
    }

    CriticalTask<Void> ct = new CriticalTask<>("test", () -> {
      synchronized (lock) {
        CriticalTask.await(lock);
        return null;
      }
    });
    ct.start();
    WaitTestHelper.testWait(ct::join, () -> {
      synchronized (lock) {
        lock.notifyAll();
      }
    });
  }


  @Test
  public void await1() throws Exception {
    final Object lock = new Object();
    try {
      CriticalTask.await(lock, 10000);
      fail();
    } catch (AssertionError e) {
      // correct
    }

    CriticalTask<Void> ct = new CriticalTask<>("test", () -> {
      synchronized (lock) {
        CriticalTask.await(lock, 10000);
        return null;
      }
    });
    ct.start();
    WaitTestHelper.testWait(ct::join, () -> {
      synchronized (lock) {
        lock.notifyAll();
      }
    });
  }


  @Test
  public void checkFailure() throws Exception {
    CriticalTask<Void> ct = new CriticalTask<>("test", () -> {
      throw new IllegalStateException("TESTING");
    });
    ct.start();
    ct.join();
    try {
      ct.checkFailure();
      fail();
    } catch (ExecutionException ee) {
      assertNotNull(ee.getCause());
      assertEquals("TESTING", ee.getCause().getMessage());
    }
  }


  @Test
  public void getResult() throws Exception {
    CriticalTask<String> ct = new CriticalTask<>("test", () -> "TESTING");
    ct.start();
    ct.join();
    ct.checkFailure();
    assertEquals("TESTING", ct.getResult());
  }


  @Test
  public void interrupt() {
    CriticalTask<Void> ct = new CriticalTask<>("test", () -> null);
    try {
      ct.interrupt();
      fail();
    } catch (SecurityException se) {
      // correct
    }
  }


  @Test
  public void isInterrupted() {
    CriticalTask<Void> ct = new CriticalTask<>("test", () -> null);
    assertFalse(ct.isInterrupted());
  }


  @Test
  public void joinWith() throws Exception {
    Thread t = new Thread();
    t.start();

    try {
      CriticalTask.joinWith(t);
      fail();
    } catch (AssertionError e) {
      // correct
    }

    CriticalTask.execute("Test", () -> {
      CriticalTask.joinWith(t);
      return null;
    });
  }


  @Test
  public void joinWith1() throws Exception {
    Thread t = new Thread();
    t.start();

    try {
      CriticalTask.joinWith(t, 10000);
      fail();
    } catch (AssertionError e) {
      // correct
    }

    CriticalTask.execute("Test", () -> {
      CriticalTask.joinWith(t, 10000);
      return null;
    });
  }


  @SuppressFBWarnings("RU_INVOKE_RUN")
  @Test
  public void runImpl() {
    CriticalTask<Void> ct = new CriticalTask<>("test");
    try {
      ct.run();
      fail();
    } catch (AssertionError e) {
      // correct
    }
  }


  @Test
  public void sleepNow() throws Exception {
    try {
      CriticalTask.sleepNow(10000);
      fail();
    } catch (AssertionError e) {
      // correct
    }

    CriticalTask<Void> ct = new CriticalTask<>("test", () -> {
      CriticalTask.sleepNow(10);
      return null;
    });
    ct.start();
    ct.join();
    ct.checkFailure();
  }

}