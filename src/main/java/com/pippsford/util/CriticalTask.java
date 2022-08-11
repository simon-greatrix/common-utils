package com.pippsford.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread that cannot be interrupted.
 *
 * @author Simon Greatrix on 12/07/2017.
 */
// Error messages do not need to be explicit constants.
@SuppressWarnings("squid:S1192")
public class CriticalTask<V> extends Thread {

  static final Logger LOG = LoggerFactory.getLogger(CriticalTask.class);

  /** ID source for pooled threads. */
  private static final AtomicInteger ID_SRC = new AtomicInteger();


  /**
   * Wait for a notification on the specified object. The current thread must be a CriticalTask to guarantee it will not be interrupted.
   *
   * @param obj the object to wait upon
   */
  // This is an interrupt free replacement for wait(). Warnings regarding how wait() can be
  // interrupted or spuriously wake up do not need to be addressed here.
  @SuppressFBWarnings("WA_NOT_IN_LOOP")
  @SuppressWarnings({"squid:S2142", "squid:S2273", "squid:S2274"})
  public static void await(Object obj) {
    if (!(Thread.currentThread() instanceof CriticalTask)) {
      Error e = new AssertionError("await() used by non critical task.");
      LOG.error("CriticalTask API misused.", e);
      throw e;
    }
    try {
      obj.wait();
    } catch (InterruptedException ie) {
      throw new AssertionError("Critical task was interrupted.", ie);
    }
  }


  /**
   * Wait for a notification on the specified object. The current thread must be a CriticalTask to guarantee it will not be interrupted.
   *
   * @param obj    the object to wait upon
   * @param millis milliseconds to wait
   */
  // This is an interrupt free replacement for wait(). Warnings regarding how wait() can be
  // interrupted or spuriously wake up do not need to be addressed here.
  @SuppressWarnings({"squid:S2142", "squid:S2273", "squid:S2274"})
  public static void await(Object obj, long millis) {
    if (!(Thread.currentThread() instanceof CriticalTask)) {
      Error e = new AssertionError("await() used by non critical task.");
      LOG.error("CriticalTask API misused.", e);
      throw e;
    }
    try {
      obj.wait(millis);
    } catch (InterruptedException ie) {
      throw new AssertionError("Critical task was interrupted.", ie);
    }
  }


  /**
   * Execute a critical task. If the calling thread is interrupted, it will not stop but its interrupted status will be set on exit.
   *
   * @param <V>  the return type
   * @param name the name of this task
   * @param task the task
   *
   * @return the output of the task
   *
   * @throws ExecutionException if the task fails
   */
  public static <V> V execute(String name, Callable<V> task) throws ExecutionException {
    CriticalTask<V> ct = new CriticalTask<>(name, task);
    ct.start();
    boolean wasInterrupted = false;
    try {
      while (ct.isAlive()) {
        try {
          ct.join();
        } catch (InterruptedException ie) {
          wasInterrupted = true;
        }
      }
    } finally {
      if (wasInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
    ct.checkFailure();
    return ct.getResult();
  }


  /**
   * Join with another thread. The current thread must be a CriticalTask to guarantee it will not be interrupted.
   *
   * @param thread the thread to join
   */
  // Critical tasks cannot be interrupted
  @SuppressWarnings("squid:S2142")
  public static void joinWith(Thread thread) {
    if (!(Thread.currentThread() instanceof CriticalTask)) {
      Error e = new AssertionError("joinWith() used by non critical task.");
      LOG.error("CriticalTask API misused.", e);
      throw e;
    }
    try {
      thread.join();
    } catch (InterruptedException ie) {
      throw new AssertionError("Critical task was interrupted.", ie);
    }
  }


  /**
   * Join with another thread. The current thread must be a CriticalTask to guarantee it will not be interrupted.
   *
   * @param thread the thread to join
   * @param millis milliseconds to wait
   */
  // Critical tasks cannot be interrupted
  @SuppressWarnings("squid:S2142")
  public static void joinWith(Thread thread, long millis) {
    if (!(Thread.currentThread() instanceof CriticalTask)) {
      Error e = new AssertionError("joinWith() used by non critical task.");
      LOG.error("CriticalTask API misused.", e);
      throw e;
    }
    try {
      thread.join(millis);
    } catch (InterruptedException ie) {
      throw new AssertionError("Critical task was interrupted.", ie);
    }
  }


  /**
   * Sleep a critical task.
   *
   * @param millis milliseconds to sleep for
   */
  // Critical tasks cannot be interrupted
  @SuppressWarnings({"squid:S2142"})
  public static void sleepNow(long millis) {
    if (!(Thread.currentThread() instanceof CriticalTask)) {
      Error e = new AssertionError("sleepNow() used by non critical task.");
      LOG.error("CriticalTask API misused.", e);
      throw e;
    }
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ie) {
      throw new AssertionError("Critical task was interrupted.", ie);
    }
  }


  /**
   * The task to run.
   */
  private final Callable<V> task;

  /** This task's name. */
  private final String taskName;

  /**
   * Any exception that occurs during execution.
   */
  private ExecutionException failure = null;

  /**
   * The result of the execution.
   */
  private V result = null;


  /**
   * Create a new critical task.
   *
   * @param name the task's name
   * @param task the task
   */
  public CriticalTask(String name, Callable<V> task) {
    super("CriticalTask-" + ID_SRC.getAndIncrement());
    this.task = task;
    taskName = name;
  }


  /**
   * Create a new critical task. Used this way the <code>runImpl()</code> method must be over-ridden.
   *
   * @param name the task's name
   */
  public CriticalTask(String name) {
    super("CriticalTask-" + ID_SRC.getAndIncrement());
    this.taskName = name;
    this.task = null;
  }


  /**
   * Check if the task failed.
   *
   * @throws ExecutionException if the task threw an exception.
   */
  public void checkFailure() throws ExecutionException {
    if (failure != null) {
      throw failure;
    }
  }


  /**
   * Get the result of the task.
   *
   * @return the result
   */
  public V getResult() {
    return result;
  }


  /**
   * Attempt to interrupt this thread - which will produce a security exception because you really should not be trying to do that.
   */
  @Override
  public final void interrupt() {
    SecurityException e = new SecurityException("Attempt was made to interrupt a critical task");
    LOG.error("Attempt was made to interrupt a critical task.", e);
    throw e;
  }


  /**
   * This thread cannot be interrupted.
   *
   * @return false, always
   */
  @Override
  public boolean isInterrupted() {
    return false;
  }


  @Override
  public void run() {
    LOG.info("Starting critical task {}", taskName);
    try {
      result = runImpl();
    } catch (Exception e) {
      LOG.warn("Critical task {} failed", taskName, e);
      failure = new ExecutionException(e);
    }
    LOG.info("Finished critical task {} with result {}", taskName, result);
  }


  protected V runImpl() throws Exception {
    if (task == null) {
      throw new AssertionError("Task was not set for critical task.");
    }
    return task.call();
  }

}
