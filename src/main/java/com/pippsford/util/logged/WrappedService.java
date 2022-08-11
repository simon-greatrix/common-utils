package com.pippsford.util.logged;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.pippsford.util.LoggedThread;

/**
 * A wrapper class that exposes only the ExecutorService methods of an ExecutorService implementation.
 */
@SuppressWarnings("checkstyle:OneTopLevelClass")
public class WrappedService extends AbstractExecutorService {

  private static <T> Collection<? extends Callable<T>> logged(Collection<? extends Callable<T>> tasks) {
    return tasks.stream().map(t -> LoggedThread.logged(LoggedThread.logger, t)).collect(Collectors.toCollection(ArrayList::new));
  }


  private final ExecutorService e;


  public WrappedService(ExecutorService executor) {
    e = executor;
  }


  public boolean awaitTermination(long timeout, TimeUnit unit)
      throws InterruptedException {
    return e.awaitTermination(timeout, unit);
  }


  public void execute(Runnable command) {
    e.execute(LoggedThread.logged(command));
  }


  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return e.invokeAll(logged(tasks));
  }


  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks,
      long timeout, TimeUnit unit
  )
      throws InterruptedException {
    return e.invokeAll(logged(tasks), timeout, unit);
  }


  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return e.invokeAny(logged(tasks));
  }


  @Override
  public <T> T invokeAny(
      Collection<? extends Callable<T>> tasks,
      long timeout, TimeUnit unit
  )
      throws InterruptedException, ExecutionException, TimeoutException {
    return e.invokeAny(logged(tasks), timeout, unit);
  }


  public boolean isShutdown() {
    return e.isShutdown();
  }


  public boolean isTerminated() {
    return e.isTerminated();
  }


  public void shutdown() {
    e.shutdown();
  }


  @Override
  public List<Runnable> shutdownNow() {
    return e.shutdownNow();
  }


  @Override
  public Future<?> submit(Runnable task) {
    return e.submit(LoggedThread.logged(task));
  }


  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return e.submit(LoggedThread.logged(LoggedThread.logger, task));
  }


  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return e.submit(LoggedThread.logged(task), result);
  }

}
