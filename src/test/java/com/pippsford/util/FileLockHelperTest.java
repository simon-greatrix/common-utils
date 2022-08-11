package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.jimfs.Jimfs;
import com.pippsford.util.FileLockHelper.LockingFile;
import org.junit.Test;

/**
 * @author Simon Greatrix on 22/08/2018.
 */
public class FileLockHelperTest {

  @Test
  public void getCanonicalFile() throws Exception {
    File temp = File.createTempFile("foo", "bar");
    File c1 = FileLockHelper.getCanonicalFile(temp);
    File c2 = FileLockHelper.getCanonicalFile(temp.getCanonicalPath());
    assertSame(c1, c2);
    temp.delete();
  }


  @Test
  public void getLockingFileForFolder() throws Exception {
    FileSystem fs = Jimfs.newFileSystem();
    Path path = fs.getPath("foo", "bar");
    Files.createDirectories(path);
    LockingFile lf = FileLockHelper.getLockingFile(path);
    assertTrue(Files.isSameFile(path, lf.getProtectedPath()));
    assertEquals(path.toUri(), lf.getUri());

    // TODO - how to test this actually locks?
    lf.lock(true);
    assertTrue(Files.exists(fs.getPath("foo", "bar", ".lock")));
    lf.lock(true);
    assertTrue(Files.exists(fs.getPath("foo", "bar", ".lock")));
    lf.unlock();
    assertTrue(Files.exists(fs.getPath("foo", "bar", ".lock")));
    lf.unlock();
    assertFalse(Files.exists(fs.getPath("foo", "bar", ".lock")));

  }

  @Test
  public void getLockingFile() throws Exception {
    FileSystem fs = Jimfs.newFileSystem();
    Path path = fs.getPath("foo", "bar");
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    LockingFile lf = FileLockHelper.getLockingFile(path);
    assertTrue(Files.isSameFile(path, lf.getProtectedPath()));
    assertEquals(path.toUri(), lf.getUri());

    // TODO - how to test this actually locks?
    lf.lock(true);
    assertTrue(Files.exists(fs.getPath("foo", "bar.lock")));
    lf.lock(true);
    assertTrue(Files.exists(fs.getPath("foo", "bar.lock")));
    lf.unlock();
    assertTrue(Files.exists(fs.getPath("foo", "bar.lock")));
    lf.unlock();
    assertFalse(Files.exists(fs.getPath("foo", "bar.lock")));

  }

}