package com.pippsford.util;

import java.lang.Thread.State;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

/**
 * Helper for testing thread wait operations.
 *
 * @author Simon Greatrix on 06/07/2017.
 */
public class WaitTestHelper {

  /**
   * An action that can be tested.
   */
  @FunctionalInterface
  public interface Action {

    void doIt() throws Exception;

  }



  static class ActionRunner implements Runnable {

    final Action action;

    Exception failure = null;


    ActionRunner(Action action) {
      this.action = action;
    }


    public void rethrow() throws Exception {
      if (failure != null) {
        throw failure;
      }
    }


    @Override
    public void run() {
      try {
        action.doIt();
      } catch (Exception e) {
        failure = e;
      }
    }

  }



  static class ObservableExecutor implements ExecutorService {

    final ExecutorService exec = new ThreadPoolExecutor(0, 5, 10, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new CallerRunsPolicy());

    final Object lock = new Object();

    int count = 0;


    public void awaitQuiesence() throws InterruptedException {
      synchronized (lock) {
        while (count != 0) {
          lock.wait();
        }
      }
    }


    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
      return exec.awaitTermination(timeout, unit);
    }


    @Override
    public void execute(@Nonnull final Runnable command) {
      synchronized (lock) {
        count++;
      }
      exec.execute(() -> {
        try {
          command.run();
        } finally {
          synchronized (lock) {
            count--;
            lock.notifyAll();
          }
        }
      });
    }


    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) {
      throw new UnsupportedOperationException();
    }


    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) {
      throw new UnsupportedOperationException();
    }


    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) {
      throw new UnsupportedOperationException();
    }


    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) {
      throw new UnsupportedOperationException();
    }


    @Override
    public boolean isShutdown() {
      return exec.isShutdown();
    }


    @Override
    public boolean isTerminated() {
      return exec.isTerminated();
    }


    @Override
    public void shutdown() {
      exec.shutdown();
    }


    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
      return exec.shutdownNow();
    }


    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
      throw new UnsupportedOperationException();
    }


    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
      throw new UnsupportedOperationException();
    }


    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
      throw new UnsupportedOperationException();
    }

  }


  /**
   * Test a wait happens properly.
   *
   * @param waiter   the action that will wait.
   * @param releaser the action that will release the wait
   *
   * @throws Exception if the actions fail, or the test does not run to plan.
   */
  @SuppressWarnings({"deprecation", "squid:S2925"})
  public static void testWait(Action waiter, Action releaser) throws Exception {
    ActionRunner run1 = new ActionRunner(waiter);

    Thread thread1 = new Thread(run1);
    thread1.start();

    // wait for thread to die or wait
    while (thread1.isAlive() && (thread1.getState() != State.WAITING)) {
      Thread.sleep(1);
    }
    run1.rethrow();

    if (!thread1.isAlive()) {
      throw new AssertionError("Did not wait");
    }

    // try to release wait
    ActionRunner run2 = new ActionRunner(releaser);
    Thread thread2 = new Thread(run2);
    thread2.start();
    thread2.join();
    run2.rethrow();

    thread1.join(5000);
    if (thread1.isAlive()) {
      thread1.stop();
      throw new AssertionError("Did not restart");
    }

    // assuming no exception, we are good.
    run1.rethrow();
  }

}

