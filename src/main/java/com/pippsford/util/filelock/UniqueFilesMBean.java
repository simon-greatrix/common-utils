package com.pippsford.util.filelock;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.pippsford.util.FileLockHelper.LockingFile;
import com.pippsford.util.SimpleInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean to access the UniqueFiles singleton.
 *
 * @author Simon Greatrix on 30/06/2021.
 */
public class UniqueFilesMBean {

  private static final String[] PARAM_FILE = {File.class.getName()};

  private static final String[] PARAM_PATH = {Path.class.getName()};

  private static final String[] PARAM_STRING = {String.class.getName()};

  /** Name of the unique files instance. */
  private static final ObjectName UNIQUE_NAME;

  private static final Logger logger = LoggerFactory.getLogger(UniqueFilesMBean.class);


  /**
   * Get the canonical file for a path. The file instance will be a singleton across all class loaders.
   *
   * @param path the file path
   *
   * @return the canonical file
   *
   * @throws IOException if the canonical file cannot be established
   */
  public static CanonicalFile getCanonicalFile(String path) throws IOException {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      Object value = server.invoke(UNIQUE_NAME, "getCanonicalFile",
          new Object[]{path}, PARAM_STRING
      );
      if (value instanceof CanonicalFile) {
        return (CanonicalFile) value;
      }

      // Has come from a different class loader
      return SimpleInvocationHandler.newProxy(CanonicalFile.class, value);
    } catch (JMException e) {
      throw toIOException(e);
    }
  }


  /**
   * Get the canonical file for a given file. The canonical file is a global singleton across all classloaders.
   *
   * @param file the file
   *
   * @return the canonical file
   *
   * @throws IOException if the locking file cannot be established
   */
  public static CanonicalFile getCanonicalFile(File file) throws IOException {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      Object value = server.invoke(UNIQUE_NAME, "getCanonicalFile",
          new Object[]{file}, PARAM_FILE
      );
      if (value instanceof CanonicalFile) {
        return (CanonicalFile) value;
      }

      // Has come from a different class loader
      return SimpleInvocationHandler.newProxy(CanonicalFile.class, value);
    } catch (JMException e) {
      throw toIOException(e);
    }
  }


  /**
   * Get a file that will act as a lock on another file against other threads
   * and other processes.
   *
   * @param file the file to be protected. The locking file will have the same
   *             path and name with ".lock" appended
   *
   * @return a LockingFile object for the resource.
   *
   * @throws IOException if the locking file cannot be established
   */
  public static LockingFile getLockingFile(File file) throws IOException {
    return getLockingFile(file.toPath());
  }


  /**
   * Get a file that will act as a lock on another file against other threads
   * and other processes.
   *
   * @param path the file or folder to be protected. The locking file will have the same
   *             path and name with ".lock" appended
   *
   * @return a LockingFile object for the resource.
   *
   * @throws IOException if the locking file cannot be established
   */
  public static LockingFile getLockingFile(Path path) throws IOException {
    Object lf;
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      lf = server.invoke(UNIQUE_NAME, "getLockingFile",
          new Object[]{path}, PARAM_PATH
      );
    } catch (JMException e) {
      throw toIOException(e);
    }
    if (lf instanceof LockingFile) {
      return (LockingFile) lf;
    }

    // The locking file instance came from a different class-loader.
    return SimpleInvocationHandler.newProxy(LockingFile.class, lf);
  }


  private static IOException toIOException(JMException e) {
    Throwable t;
    while ((t = e.getCause()) != null) {
      if (t instanceof IOException) {
        return (IOException) t;
      }
    }
    return new IOException("Failed to access file system singleton", e.getCause());
  }


  static {
    MBeanServer server;
    try {
      server = ManagementFactory.getPlatformMBeanServer();
      // Version 2 adds support for paths.
      UNIQUE_NAME = new ObjectName("SETL:type=UniqueFileManager,version=2");
    } catch (MalformedObjectNameException e) {
      throw new InternalError("Valid MX object name rejected", e);
    }

    // Create the FileLockHelper MX bean. There should only be one of these in
    // the runtime, so we synchronize on the runtime instance.
    try {
      synchronized (Runtime.getRuntime()) {
        if (!server.isRegistered(UNIQUE_NAME)) {
          UniqueFiles mx = new UniqueFilesImpl();
          StandardMBean bean = new StandardMBean(
              mx,
              UniqueFiles.class
          );
          server.registerMBean(bean, UNIQUE_NAME);
        }
      }
    } catch (InstanceAlreadyExistsException e) {
      // obviously synchronizing on Runtime did not work
      logger.error("FileLockHelper MX bean already instantiated", e);
    } catch (MBeanRegistrationException | NotCompliantMBeanException e) {
      throw new InternalError("Unexpected JM Exception", e);
    }
  }
}
