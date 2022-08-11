package com.pippsford.util.logged;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.pippsford.util.LoggedThread;

/**
 * A wrapper class that provides the ScheduledExecutorService methods.
 */
@SuppressWarnings("checkstyle:OneTopLevelClass")
public class WrappedScheduledService extends WrappedService implements ScheduledExecutorService {

  private final ScheduledExecutorService scheduled;


  public WrappedScheduledService(ScheduledExecutorService service) {
    super(service);
    scheduled = service;
  }


  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return scheduled.schedule(LoggedThread.logged(command), delay, unit);
  }


  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return scheduled.schedule(LoggedThread.logged(callable), delay, unit);
  }


  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return scheduled.scheduleAtFixedRate(LoggedThread.logged(command), initialDelay, period, unit);
  }


  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return scheduled.scheduleWithFixedDelay(LoggedThread.logged(command), initialDelay, delay, unit);
  }

}
