package com.pippsford.util.filelock;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pippsford.util.FileLockHelper.LockingFile;

/**
 * A file which acts as a lock for another file, locking it against other
 * threads and processes.
 *
 * @author Simon Greatrix
 */
public class LockingFileImpl implements LockingFile {

  /** Set of locked files to prevent them being garbage collected. */
  private static final Set<LockingFile> LOCKED_FILES = Collections.synchronizedSet(new HashSet<>());

  private static final Logger logger = LoggerFactory.getLogger(LockingFileImpl.class);

  /** The lock file. */
  private final Path lockFile;

  /**
   * The locking types. A thread that holds a write lock can acquire any
   * number of further locks of any type. If no write locks are held, then
   * any number of threads can acquire any number of read locks, but no
   * write locks can be acquired. Thus if any write lock is held all locks
   * are held by one thread so thread safety does not matter. If no write
   * locks, then all locks are read locks and hence thread safety does not
   * matter as everything is the same.
   */
  private final Deque<Boolean> lockTypes = new LinkedList<>();

  /** The protected file. */
  private final Path protectedFile;

  /** The thread locking. */
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  /** Synchronization lock for internal state updates. */
  private final Object syncLock = new Object();

  /** The URI of the protected file. */
  private final URI uri;

  /** The file channel that holds the lock. */
  private FileChannel channel = null;

  /** The process lock. */
  private FileLock fileLock = null;


  /**
   * Create a LockingFile for the specified file.
   *
   * @param uri  the URI of the file that is locked
   * @param path the path equivalent of the path
   *
   * @throws IOException on failure to create LockingFile
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  LockingFileImpl(URI uri, Path path) throws IOException {
    this.uri = uri;
    protectedFile = path;
    if (Files.isDirectory(protectedFile)) {
      lockFile = path.resolve(".lock");
    } else {
      lockFile = path.resolveSibling(path.getFileName() + ".lock");
    }
  }


  private void checkLockContent() {
    try {
      if (channel.size() == 0) {
        String txt = "DO NOT DELETE OR RENAME THIS FILE\n\n"
            + "This file is used to prevent concurrent updates of:\n"
            + uri.toASCIIString()
            + "\n\nCreated at : " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + "\n";

        byte[] bytes = txt.getBytes(StandardCharsets.UTF_8);
        channel.position(0).write(ByteBuffer.wrap(bytes));
        channel.force(true);
      }
    } catch (IOException ioe) {
      // As long as the file exists, it should not matter if it is empty
      logger.info("Unable to write contents to lock file {}", uri.toASCIIString());
    }
  }


  /** Close the lock file, releasing all OS resources. */
  private void close() {
    // close everything...
    try {
      // first the lock
      closeReleaseLock();
    } finally {
      try {
        // Delete the file.
        closeDeleteFile();
      } finally {
        try {
          // Close the file channel
          closeChannel();
        } finally {
          // ensure this can be garbage collected now it is not locked
          LOCKED_FILES.remove(this);
        }
      }
    }
  }


  /**
   * Final part of the close operation - close the channel.
   */
  private void closeChannel() {
    try {
      if (channel != null) {
        channel.close();
      }
    } catch (IOException ioe) {
      logger.error("IO failure when closing channel.", ioe);
    } finally {
      channel = null;
    }
  }


  /**
   * Second part of the close operation - delete the lock file.
   */
  private void closeDeleteFile() {
    if (channel == null) {
      return;
    }

    try {
      FileLock deleteLock = channel.tryLock(0, Long.MAX_VALUE, false);
      if (deleteLock != null) {
        try {
          if (Files.deleteIfExists(lockFile)) {
            logger.debug("Lock file {} deleted", uri);
          } else {
            logger.debug("Lock file {} was already deleted", uri);
          }
        } finally {
          deleteLock.release();
        }
      } else {
        logger.debug("Lock file not deleted, as locked by another process");
      }
    } catch (IOException ioe) {
      logger.error("IO failure when deleting lock file. Continuing to close channel.", ioe);
    }
  }


  /**
   * First step in the close operation - release the lock.
   */
  private void closeReleaseLock() {
    try {
      if (fileLock != null) {
        fileLock.release();
      }
    } catch (IOException ioe) {
      logger.error("IO failure when releasing lock. Continuing to close channel.", ioe);
    } finally {
      fileLock = null;
    }
  }


  @Override
  public boolean exists() {
    return Files.exists(lockFile);
  }


  /**
   * Ensure everything is unlocked and closed.
   *
   * @deprecated Deprecated because Object.finalize() is deprecated
   */
  @Override
  @Deprecated(since = "1.2")
  @SuppressWarnings({"squid:ObjectFinalizeOverridenCheck", "checkstyle:NoFinalizer"})
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }


  @Override
  public String getPath() {
    return lockFile.toString();
  }


  /**
   * Get the file protected by this locking file.
   *
   * @return the protected file
   */
  @Override
  public File getProtectedFile() {
    return protectedFile.toFile();
  }


  @Override
  public Path getProtectedPath() {
    return protectedFile;
  }


  @Override
  public URI getUri() {
    return uri;
  }


  /**
   * Lock the file.
   *
   * @param shared if true, the lock is shared
   */
  @Override
  public void lock(boolean shared) throws InterruptedException, IOException {
    tryLock(shared, -1);
  }


  /**
   * Lock the file against other processes.
   *
   * @param shared do we use a shared lock?
   */
  private void lockFile(boolean shared, long endTime, long timeout) throws InterruptedException, IOException {
    // Close and re-acquire the O/S lock. This allows us to switch lock types, and creates an opportunity for other processes to have a turn with the file.
    close();

    channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    if (endTime < 0) {
      fileLock = channel.lock(0L, Long.MAX_VALUE, shared);

      // ensure this is not garbage collected whilst locked
      LOCKED_FILES.add(this);
      checkLockContent();
      return;
    }

    while (true) {
      fileLock = channel.tryLock(0L, Long.MAX_VALUE, shared);
      if (fileLock != null) {
        // ensure this is not garbage collected whilst locked
        LOCKED_FILES.add(this);
        checkLockContent();
        return;
      }

      // Are we out of time?
      if (System.currentTimeMillis() > endTime) {
        return;
      }

      // As lock may be held by another process, we can only briefly wait and then spin back. Note: This is a deliberate busy wait.
      Thread.sleep(timeout / 10);
    }
  }


  @Override
  public String toString() {
    return "LockFile[" + lockFile.toUri() + " protecting " + uri + "]";
  }


  @Override
  @SuppressWarnings("squid:S2222") // The point is to acquire a lock, so not releasing it is correct behaviour
  public boolean tryLock(boolean shared, int timeout) throws InterruptedException, IOException {
    Lock lock = shared ? rwLock.readLock() : rwLock.writeLock();
    long endTime;
    if (timeout < 0) {
      endTime = -1;
      lock.lock();
    } else {
      endTime = System.currentTimeMillis() + timeout;
      if (!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
        return false;
      }
    }

    // the read-write lock protects the file lock so we can manipulate
    // that now
    synchronized (syncLock) {
      if (fileLock == null) {
        try {
          lockFile(shared, endTime, timeout);
        } catch (IOException ioe) {
          lock.unlock();
          throw ioe;
        }
      }
      lockTypes.push(shared);
    }
    return true;
  }


  /**
   * Unlock the file.
   */
  @Override
  public void unlock() {
    Lock lock;
    synchronized (syncLock) {
      if (fileLock == null) {
        throw new IllegalStateException("File not OS locked");
      }
      Boolean shared = lockTypes.pop();
      if (lockTypes.isEmpty()) {
        close();
      }
      lock = shared ? rwLock.readLock() : rwLock.writeLock();
    }
    lock.unlock();
  }

}