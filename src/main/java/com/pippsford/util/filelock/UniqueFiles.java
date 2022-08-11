package com.pippsford.util.filelock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.pippsford.util.FileLockHelper.LockingFile;

/**
 * Interface for managing unique file information across applications within
 * one application server.
 */
public interface UniqueFiles {

  /**
   * Get the single File instance that represents the canonical file
   * specified.
   *
   * @param path the file's path
   *
   * @return the singleton canonical file instance
   */
  File getCanonicalFile(File path) throws IOException;


  /**
   * Get the single File instance that represents the canonical file
   * specified.
   *
   * @param path the file's path
   *
   * @return the singleton canonical file instance
   */
  File getCanonicalFile(String path) throws IOException;


  /**
   * Get the single File instance that represents the canonical file
   * specified.
   *
   * @param path the file's path
   *
   * @return the singleton canonical file instance
   */
  LockingFile getLockingFile(Path path) throws IOException;

}
