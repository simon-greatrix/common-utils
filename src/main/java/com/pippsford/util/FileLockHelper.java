package com.pippsford.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import com.pippsford.util.filelock.UniqueFilesMBean;

/**
 * FileLockHelper provides methods to assist with locking.
 */
public class FileLockHelper {

  /**
   * A file which acts as a lock for another file, locking it against other
   * threads and processes.
   *
   * @author Simon Greatrix
   */
  public interface LockingFile {

    /**
     * Check if the file currently exists. If it does, some lock is probably held.
     *
     * @return true if the file exists
     */
    boolean exists();


    /**
     * Get the absolute path of the lock file.
     *
     * @throws UnsupportedOperationException if the lock file is not on the default file system
     */
    String getPath();


    /**
     * Get the file protected by this locking file.
     *
     * @return the protected file
     */
    File getProtectedFile();


    /**
     * Get the path protected by this locking file.
     *
     * @return the protected path
     */
    Path getProtectedPath();


    /** Get the URI of the lock file. */
    URI getUri();


    /**
     * Lock the file.
     *
     * @param shared if true, the lock is shared
     */
    void lock(boolean shared) throws InterruptedException, IOException;


    /**
     * Attempt to lock the file.
     *
     * @param shared  if true, the lock is shared
     * @param timeout the number of milliseconds to wait for the lock
     *
     * @return true if the lock was acquired
     */
    boolean tryLock(boolean shared, int timeout) throws InterruptedException, IOException;


    /**
     * Unlock the file.
     */
    void unlock();

  }


  /**
   * Get a really canonical file. The file return will be canonical and it
   * will be a singleton within the JVM.
   *
   * @param file the file to canonicalize.
   *
   * @return the canonical file
   *
   * @throws IOException if the canonical file cannot be determined
   */
  public static File getCanonicalFile(File file) throws IOException {
    return UniqueFilesMBean.getCanonicalFile(file);
  }


  /**
   * Get a really canonical file. The file return will be canonical and it
   * will be a singleton within the JVM.
   *
   * @param path the file path to canonicalize.
   *
   * @return the canonical file
   *
   * @throws IOException if the canonical file cannot be determined
   */
  public static File getCanonicalFile(String path) throws IOException {
    return UniqueFilesMBean.getCanonicalFile(path);
  }


  /**
   * Get a file that will act as a lock on another file against other threads
   * and other processes.
   *
   * @param file the file to be protected. The locking file will have the same
   *             path and name with ".lock" appended
   *
   * @return a LockingFile object for the resource.
   */
  public static LockingFile getLockingFile(File file) throws IOException {
    return UniqueFilesMBean.getLockingFile(file);
  }


  /**
   * Get a file that will act as a lock on another file against other threads
   * and other processes.
   *
   * @param path the file to be protected. The locking file will have the same
   *             path and name with ".lock" appended
   *
   * @return a LockingFile object for the resource.
   */
  public static LockingFile getLockingFile(Path path) throws IOException {
    return UniqueFilesMBean.getLockingFile(path);
  }


  /**
   * Convert a path specification that might be a file path or a URI to a NIO Path.
   *
   * @param pathOrUri the file or URI
   *
   * @return the NIO Path
   */
  public static Path toPath(String pathOrUri) {
    try {
      URI uri = new URI(pathOrUri);
      if (uri.getScheme() != null) {
        return Path.of(uri).normalize().toAbsolutePath();
      }
    } catch (URISyntaxException e) {
      // Do nothing. It could still be a valid path somehow.
    }

    // Resolve against the default file system
    return FileSystems.getDefault().getPath(pathOrUri).normalize().toAbsolutePath();
  }

}
