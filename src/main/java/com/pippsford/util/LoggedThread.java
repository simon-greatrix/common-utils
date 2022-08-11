package com.pippsford.util;


import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pippsford.util.logged.WrappedScheduledService;
import com.pippsford.util.logged.WrappedService;

/**
 * Ensure abnormal termination of a callable or a runnable is logged.
 *
 * @author Simon Greatrix on 20/09/2018.
 */
public class LoggedThread extends Thread {

  /** Logger for this class. */
  public static final Logger logger = LoggerFactory.getLogger(LoggedThread.class);


  /**
   * Create a logged callable which will log any abnormal termination on the provided logger.
   *
   * @param log      the logger
   * @param callable the callable
   * @param <V>      the callable's type
   *
   * @return a logging callable
   */
  @SuppressWarnings("squid:S1181") // I want to log errors if I can
  public static <V> Callable<V> logged(Logger log, Callable<V> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (RuntimeException re) {
        log.error("Callable abnormally terminated by runtime exception", re);
        throw re;
      } catch (Exception e) {
        log.error("Callable abnormally terminated by exception", e);
        throw e;
      } catch (Error e) {
        log.error("Callable abnormally terminated by error", e);
        throw e;
      }
    };
  }


  /**
   * Wrap an executor service so that the unexpected failure of any task is logged.
   *
   * @param executorService the service to wrap
   *
   * @return the wrapped service
   */
  public static ExecutorService logged(ExecutorService executorService) {
    if (executorService instanceof WrappedService) {
      return executorService;
    }
    return new WrappedService(executorService);
  }


  /**
   * Wrap a scheduled executor service so that the unexpected failure of any task is logged.
   *
   * @param executorService the service to wrap
   *
   * @return the wrapped service
   */
  public static ScheduledExecutorService logged(ScheduledExecutorService executorService) {
    if (executorService instanceof WrappedScheduledService) {
      return executorService;
    }
    return new WrappedScheduledService(executorService);
  }


  /**
   * Wrap a callable instance inside a try-catch that ensures abnormal termination is logged.
   *
   * @param callable the callable
   * @param <V>      the callable's return type
   *
   * @return the wrapped callable
   */
  public static <V> Callable<V> logged(Callable<V> callable) {
    return logged(logger, callable);
  }


  /**
   * Wrap a runnable instance inside a try-catch that ensures abnormal termination is logged.
   *
   * @param runnable the runnable
   *
   * @return the wrapped runnable
   */
  public static Runnable logged(Runnable runnable) {
    return logged(logger, runnable);
  }


  /**
   * Create a runnable which will log the cause of any abnormal termination on the provided logger.
   *
   * @param log      the logger
   * @param runnable the runnable
   *
   * @return a runnable which will log
   */
  @SuppressWarnings("squid:S1181") // I want to log errors if I can
  public static Runnable logged(Logger log, Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (RuntimeException re) {
        log.error("Thread abnormally terminated with runtime exception", re);
        throw re;
      } catch (Exception e) {
        // This can only happen if someone is doing silly stuff.
        log.error("Thread abnormally terminated with undeclared exception", e);
        throw new UndeclaredThrowableException(e);
      } catch (Error e) {
        log.error("Thread abnormally terminated with error", e);
        throw e;
      }
    };
  }


  /**
   * Create a thread factory which generated LoggedThread instances.
   *
   * @param name the stem for thread names
   *
   * @return the factory instance
   */
  public static ThreadFactory loggedThreadFactory(String name) {
    return loggedThreadFactory(name, null);
  }


  /**
   * Create a thread factory that generates threads which guarantee to log abnormal terminations.
   *
   * @param name the stem used to generate names for this factory
   * @param log  the logger to write errors to
   *
   * @return the new thread factory
   */
  public static ThreadFactory loggedThreadFactory(String name, Logger log) {
    final AtomicInteger idSrc = new AtomicInteger();
    final Logger actualLogger = log != null ? log : logger;
    return r -> new LoggedThread(actualLogger, r, name + ":" + idSrc.incrementAndGet());
  }


  /** The logger for abnormal termination. */
  private final Logger myLogger;


  /**
   * Create a thread which will log abnormal termination on the default logger. Sub-classes must override {@code runImpl} to do anything.
   */
  public LoggedThread() {
    myLogger = logger;
  }


  /**
   * Create a thread which will log abnormal termination of the provided target runnable on the default logger.
   *
   * @param target the target runnable
   */
  public LoggedThread(Runnable target) {
    super(target);
    myLogger = logger;
  }


  /**
   * Create a thread with the specified name which will log abnormal termination on the default logger.
   * Sub-classes must override {@code runImpl} to do anything.
   *
   * @param name the thread's name
   */
  public LoggedThread(String name) {
    super(name);
    myLogger = logger;
  }


  /**
   * Create a thread with the specified name which will log abnormal termination of the provided target runnable on the default logger.
   *
   * @param target the target runnable
   * @param name   the thread's name
   */
  public LoggedThread(Runnable target, String name) {
    super(target, name);
    myLogger = logger;
  }


  /**
   * Create a thread which will log abnormal termination on the provided logger. Sub-classes must override {@code runImpl} to do anything.
   *
   * @param myLogger the logger for abnormal termination
   */
  public LoggedThread(Logger myLogger) {
    this.myLogger = myLogger;
  }


  /**
   * Create a thread which will log abnormal termination of the provided target runnable.
   *
   * @param myLogger the logger for abnormal termination
   * @param target   the target runnable
   */
  public LoggedThread(Logger myLogger, Runnable target) {
    super(target);
    this.myLogger = myLogger;
  }


  /**
   * Create a thread with the specified name which will log abnormal termination. Sub-classes must override {@code runImpl} to do anything.
   *
   * @param myLogger the logger for abnormal termination
   * @param name     the thread's name
   */
  public LoggedThread(Logger myLogger, String name) {
    super(name);
    this.myLogger = myLogger;
  }


  /**
   * Create a thread with the specified name which will log abnormal termination of the provided target runnable.
   *
   * @param myLogger the logger for abnormal termination
   * @param target   the target runnable
   * @param name     the thread's name
   */
  public LoggedThread(Logger myLogger, Runnable target, String name) {
    super(target, name);
    this.myLogger = myLogger;
  }


  @Override
  @SuppressWarnings("squid:S1181") // I want to log errors if I can
  public final void run() {
    try {
      runImpl();
    } catch (RuntimeException re) {
      myLogger.error("Thread abnormally terminated with runtime exception", re);
      throw re;
    } catch (Exception e) {
      // This can only happen if someone is doing silly stuff.
      myLogger.error("Thread abnormally terminated with undeclared exception", e);
      throw new UndeclaredThrowableException(e);
    } catch (Error e) {
      myLogger.error("Thread abnormally terminated with error", e);
      throw e;
    }
  }


  /**
   * Sub-classes may override this method as they would normally over {@code run}. Abnormal termination of this method will be logged.
   */
  @SuppressWarnings("squid:S1217") // I really do mean to call Thread.run directly
  protected void runImpl() {
    super.run();
  }

}



