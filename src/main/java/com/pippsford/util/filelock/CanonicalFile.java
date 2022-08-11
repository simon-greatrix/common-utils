package com.pippsford.util.filelock;

import java.io.File;
import java.io.IOException;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A File which is guaranteed to be canonical. These should only be created
 * by the Singleton Map to ensure real canonical values.
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class CanonicalFile extends File {

  /** serialVersionUID. */
  private static final long serialVersionUID = -6873531854948575325L;


  /**
   * Create a CanonicalFile from a given path.
   *
   * @param path non-canonical path to file
   */
  CanonicalFile(String path) throws IOException {
    super(new File(path).getCanonicalPath());
  }


  /**
   * Get the canonical version of this file - which is this.
   *
   * @return this as this is canonical
   */
  @Override
  @Nonnull
  public File getCanonicalFile() {
    return this;
  }


  /**
   * Get the canonical path of this file - which is the same as getPath().
   *
   * @return the canonical path
   */
  @Override
  @Nonnull
  public String getCanonicalPath() {
    String path = getPath();
    // As this is canonical, it has a path.
    assert path != null : "Canonical file lacked a path";
    return path;
  }

}
