package com.pippsford.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pippsford.util.FileLockHelper.LockingFile;

/**
 * <p>A safe file output stream.</p>
 *
 * <p>Three files are involved in the output process. These are:</p>
 * <ol>
 * <li>The destination file
 * <li>A temporary file
 * <li>A lock file
 * </ol>
 *
 * <p>When the stream is created, the lock file is created and locked
 * preventing other processes from accessing the file. A small amount of
 * status data is written to the lock file. During writing data is written
 * to the temporary file. When the stream is closed, the temporary file is
 * renamed to the destination file and then the lock file is unlocked and
 * deleted. In this way only one process can write the file and the file's
 * status can be determined as follows:</p>
 *
 * <dl>
 * <dt>Destination file exists
 * <dd>file is complete
 * <dt>Destination file missing, lock file present and locked
 * <dd>file currently being written
 * <dt>Destination file missing, lock file present and unlocked
 * <dd>serious failure in writing process
 * <dt>Destination file missing, lock file missing
 * <dd>file not started
 * </dl>
 *
 * @author Simon Greatrix
 */
public class SafeFileOutputStream extends OutputStream {

  /** Prefix used to create a temp file name from the original file name. */
  static final String TEMP_FILE_PREFIX = "__SETL__.";

  /** Suffix used to create a temp file name from the original file name. */
  static final String TEMP_FILE_SUFFIX = ".pending";

  /** Should temp files be kept after a failure?. */
  private static final boolean KEEP_BAD_TEMP_FILE;

  private static final Logger logger = LoggerFactory.getLogger(SafeFileOutputStream.class);



  /**
   * Progress of writing the file.
   */
  public enum Progress {
    /** The file has been completely written. */
    COMPLETE,

    /** The file is missing. */
    MISSING,

    /** Writing of the file is in progress. */
    IN_PROGRESS,

    /** A previous attempt to write a file failed. This status should be a very rare occurrence as it indicates that the post failure clean-up also failed. */
    FAILED
  }


  /**
   * Get an output stream to write to the specified file. The stream works so
   * that only one process can write the file and the file will always appear
   * to other threads/processes as either missing or complete, never partially
   * complete. If another thread/process is currently writing the specified
   * file, this method will block until that thread/process completes.
   *
   * @param file file to write to
   *
   * @return output stream
   *
   * @throws IOException if problem creating stream
   */
  @Nonnull
  public static SafeFileOutputStream getFileOutputStream(File file) throws IOException {
    return getFileOutputStream(file.toPath());
  }


  /**
   * Get an output stream to write to the specified path. The stream works so
   * that only one process can write the path and the path will always appear
   * to other threads/processes as either missing or complete, never partially
   * complete. If another thread/process is currently writing the specified
   * file, this method will block until that thread/process completes.
   *
   * @param path path to write to
   *
   * @return output stream
   *
   * @throws IOException if problem creating stream
   */
  @Nonnull
  public static SafeFileOutputStream getFileOutputStream(Path path) throws IOException {
    SafeFileOutputStream out = getFileOutputStream(path, true);
    // Should only be null if the "overwrite" value is ignored. This keeps FindBugs happy.
    assert out != null : "Forced overwrite was ignored";
    return out;
  }


  /**
   * Get an output stream to write to the specified file. The stream works so
   * that only one process can write the file and the file will always appear
   * to other threads/processes as either missing or complete, never partially
   * complete. If another thread/process is currently writing the specified
   * file, this method will block until that thread/process completes.
   *
   * @param file      file to write to
   * @param overwrite if true, can overwrite existing file
   *
   * @return output stream, or null if not overwriting and file already exists
   *
   * @throws IOException if problem creating stream
   */
  public static SafeFileOutputStream getFileOutputStream(File file, boolean overwrite) throws IOException {
    return getFileOutputStream(file.toPath(), overwrite);
  }


  /**
   * Get an output stream to write to the specified path. The stream works so
   * that only one process can write the path and the path will always appear
   * to other threads/processes as either missing or complete, never partially
   * complete. If another thread/process is currently writing the specified
   * file, this method will block until that thread/process completes.
   *
   * @param path      path to write to
   * @param overwrite if true, can overwrite existing path
   *
   * @return output stream, or null if not overwriting and path already exists
   *
   * @throws IOException if problem creating stream
   */
  public static SafeFileOutputStream getFileOutputStream(Path path, boolean overwrite) throws IOException {
    // if not over-writing and file exists, just return
    if ((!overwrite) && Files.exists(path)) {
      return null;
    }
    // create output steam
    SafeFileOutputStream fos = new SafeFileOutputStream(path);
    // if not over-writing and file exists, then another process has
    // written it whilst we waited for a lock
    if ((!overwrite) && Files.exists(path)) {
      fos.close(false);
      return null;
    }
    // return the output stream
    return fos;
  }


  /**
   * Test a file which may be currently being written by another thread or
   * process.
   *
   * @param file file to test
   *
   * @return one of COMPLETE, MISSING, IN_PROGRESS or FAILED
   */
  public static Progress testFile(File file) throws IOException {
    return testFile(file.toPath());
  }


  /**
   * Test a path which may be currently being written by another thread or
   * process.
   *
   * @param path path to test
   *
   * @return one of COMPLETE, MISSING, IN_PROGRESS or FAILED
   */
  public static Progress testFile(Path path) throws IOException {
    // if file exists, it is complete
    if (Files.exists(path)) {
      return Progress.COMPLETE;
    }

    // if no lock file, nothing writing file so it is definitely missing
    LockingFile lockFile = FileLockHelper.getLockingFile(path);
    if (!lockFile.exists()) {
      return Progress.MISSING;
    }

    try {
      if (!lockFile.tryLock(false, 1)) {
        return Progress.IN_PROGRESS;
      }

      try {
        // file was not locked, so failed or just completed
        return Files.exists(path) ? Progress.COMPLETE : Progress.FAILED;
      } finally {
        lockFile.unlock();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      IOException ioe = new InterruptedIOException("Lock testing was interrupted");
      ioe.initCause(e);
      throw ioe;
    }
  }


  /**
   * Wait for a file to be either complete or missing. If another process is
   * writing the file, this method blocks until that process is complete.
   *
   * @param file file to test
   *
   * @return true if the file is complete, false if it is missing
   */
  public static boolean waitFor(File file) throws InterruptedException, IOException {
    // if file already exists, no need to wait
    if (file.exists()) {
      return true;
    }

    // if the lock file doesn't exist, no file being written
    LockingFile lockFile = FileLockHelper.getLockingFile(file);
    if (!lockFile.exists()) {
      return false;
    }

    // lock file exists, so either in progress or has failed
    lockFile.lock(false);
    try {
      // whatever else was using the file has now finished, so check if the file exists
      return file.exists();
    } finally {
      lockFile.unlock();
    }
  }


  static {
    KEEP_BAD_TEMP_FILE = Boolean.getBoolean("setl.safe-output.keep-bad-temp-file");
  }

  /** The destination file. */
  private final Path destinationFile;

  /** The lock on the lock file. */
  private final LockingFile lockingFile;

  /** The temporary file used to building the destination. */
  private final Path tempFile;

  /** The output stream to the temp file. */
  private OutputStream outputStream;


  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  private SafeFileOutputStream(Path path) throws IOException {
    destinationFile = path;
    lockingFile = FileLockHelper.getLockingFile(path);
    try {
      lockingFile.lock(true);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      IOException ioe = new InterruptedIOException("Lock acquisition interrupted");
      ioe.initCause(e);
      throw ioe;
    }
    Path temp = null;
    try {
      temp = Files.createTempFile(path.getParent(), TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
      outputStream = Files.newOutputStream(temp);
    } catch (IOException e) {
      if (temp != null) {
        try {
          Files.deleteIfExists(temp);
        } catch (IOException e2) {
          logger.warn("Failed to delete temporary file after error: {}", temp, e2);
        }
      }
      outputStream = null;
      throw e;
    }
    tempFile = temp;
  }


  /**
   * Check the stream is open, throwing an IOException if it is not.
   */
  private void checkOpen() throws IOException {
    if (outputStream == null) {
      throw new IOException("Output stream is closed");
    }
  }


  /** {@inheritDoc} */
  @Override
  public void close() throws IOException {
    close(true);
  }


  /**
   * Close this output stream, optionally committing the data to the
   * destination.
   *
   * @param commit if true, commit data to destination
   */
  public void close(boolean commit) throws IOException {
    if (outputStream == null) {
      return;
    }
    try {
      try {
        outputStream.close();
      } catch (IOException ioe) {
        throw handleException("Failed to close output", ioe);
      }

      // handle commit if appropriate
      if (commit) {
        // delete destination
        if (Files.exists(destinationFile)) {
          try {
            Files.delete(destinationFile);
          } catch (IOException ioe) {
            throw handleException("Failed to delete destination file: " + destinationFile.toAbsolutePath(), ioe);
          }
        }

        // rename temp to complete commit
        try {
          Files.move(tempFile, destinationFile);
        } catch (IOException ioe) {
          throw handleException(
              "Failed to rename temporary file " + tempFile.toAbsolutePath()
                  + " to " + destinationFile.toAbsolutePath(), ioe);
        }
      }

    } finally {
      // release file level lock
      lockingFile.unlock();

      // all done with output
      outputStream = null;
    }
  }


  /**
   * {@inheritDoc}
   *
   * @deprecated As Object.finalize() is deprecated.
   */
  @Override
  @Deprecated
  @SuppressWarnings({"squid:ObjectFinalizeOverridenCheck", "checkstyle:NoFinalizer"})
  protected void finalize() throws Throwable {
    close(false);
  }


  /** {@inheritDoc} */
  @Override
  public void flush() throws IOException {
    checkOpen();
    try {
      outputStream.flush();
    } catch (IOException e) {
      throw handleException("Failed to flush output stream to temporary storage", e);
    }
  }


  /**
   * Create an IOException to represent the specified error and clean-up.
   *
   * @param err Error message
   * @param ioe IOException
   *
   * @return BMException
   */
  private IOException handleException(String err, IOException ioe) {
    IOException ioeOut = new IOException(err, ioe);

    OutputStream out = outputStream;
    try {
      if (out != null) {
        out.close();
      }
    } catch (IOException e) {
      ioeOut.addSuppressed(e);
    }
    outputStream = null;

    // delete the temp file if appropriate
    if (!KEEP_BAD_TEMP_FILE) {
      try {
        Files.delete(tempFile);
      } catch (IOException ioe2) {
        ioeOut.addSuppressed(new IOException("Failed to delete temporary file", ioe2));
      }
    }

    // log the exception
    logger.warn("Error during safe file output", ioeOut);

    // return exception
    return ioeOut;
  }


  /**
   * Copy the provided input stream to this output stream.
   *
   * @param in  input stream to copy
   * @param buf buffer to use whilst copying
   */
  public void transferFrom(InputStream in, byte[] buf) throws IOException {
    if (buf == null) {
      buf = new byte[4096];
    }
    int size;
    while ((size = in.read(buf)) > 0) {
      write(buf, 0, size);
    }
  }


  /** {@inheritDoc} */
  @Override
  public void write(@Nonnull byte[] b, int off, int len) throws IOException {
    checkOpen();
    try {
      outputStream.write(b, off, len);
    } catch (IOException e) {
      throw handleException("Failed to write byte array to temporary file", e);
    }
  }


  /** {@inheritDoc} */
  @Override
  public void write(@Nonnull byte[] b) throws IOException {
    write(b, 0, b.length);
  }


  /** {@inheritDoc} */
  @Override
  public void write(int b) throws IOException {
    checkOpen();
    try {
      outputStream.write(b);
    } catch (IOException e) {
      throw handleException("Failed to write single byte to temporary file", e);
    }
  }

}
