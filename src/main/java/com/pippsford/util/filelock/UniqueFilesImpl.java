package com.pippsford.util.filelock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.pippsford.util.ConcurrentWeakValueMap;
import com.pippsford.util.FileLockHelper.LockingFile;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Implementation of the unique file handling.
 */
public class UniqueFilesImpl implements UniqueFiles {

  /** Map of paths to canonical files. */
  private static final ConcurrentWeakValueMap<String, CanonicalFile> CANON_FILES = new ConcurrentWeakValueMap<>();


  /** Map of path URIs to locking files. */
  private static final ConcurrentWeakValueMap<URI, LockingFile> CANON_LOCKS = new ConcurrentWeakValueMap<>();


  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  CanonicalFile computeCanonicalFile(String p) {
    CanonicalFile canon;
    try {
      canon = new CanonicalFile(p);
    } catch (IOException ioe) {
      throw new UndeclaredThrowableException(ioe);
    }
    String canonPath = canon.getPath();
    if (canonPath.equals(p)) {
      return canon;
    }

    return CANON_FILES.computeIfAbsent(canonPath, p2 -> canon);
  }


  @SuppressFBWarnings("LEST_LOST_EXCEPTION_STACK_TRACE")
  @Override
  public CanonicalFile getCanonicalFile(String path) throws IOException {
    try {
      return CANON_FILES.computeIfAbsent(path, this::computeCanonicalFile);
    } catch (UndeclaredThrowableException ute) {
      throw (IOException) ute.getUndeclaredThrowable();
    }
  }


  @Override
  public CanonicalFile getCanonicalFile(File path) throws IOException {
    if (path instanceof CanonicalFile) {
      return (CanonicalFile) path;
    }
    return getCanonicalFile(path.getAbsolutePath());
  }


  @SuppressFBWarnings({"LEST_LOST_EXCEPTION_STACK_TRACE", "PATH_TRAVERSAL_IN"})
  @Override
  public LockingFile getLockingFile(Path path) throws IOException {
    final Path realPath;
    if (Files.exists(path)) {
      realPath = path.toRealPath();
    } else {
      Path parent = path.getParent();
      if (parent == null) {
        throw new IOException("Cannot create a file system root: " + path);
      }
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      realPath = parent.toRealPath().resolve(path.getFileName());
    }

    final URI uri = realPath.toUri().normalize();
    try {
      return CANON_LOCKS.computeIfAbsent(uri, u -> {
        try {
          return new LockingFileImpl(u, realPath);
        } catch (IOException bme) {
          throw new UndeclaredThrowableException(bme);
        }
      });
    } catch (UndeclaredThrowableException ute) {
      throw (IOException) ute.getUndeclaredThrowable();
    }
  }

}
