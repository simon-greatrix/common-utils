package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.pippsford.util.SafeFileOutputStream.Progress;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Simon Greatrix on 22/08/2018.
 */
public class SafeFileOutputStreamTest {


  @Test
  public void correctBehaviour() throws Exception {
    File temp = File.createTempFile("foo", "bar");
    temp.delete();

    assertFalse(SafeFileOutputStream.waitFor(temp));
    assertEquals(Progress.MISSING, SafeFileOutputStream.testFile(temp));

    SafeFileOutputStream out = SafeFileOutputStream.getFileOutputStream(temp, false);
    assertNotNull(out);

    assertEquals(Progress.IN_PROGRESS, SafeFileOutputStream.testFile(temp));

    out.write("Hello World".getBytes(StandardCharsets.ISO_8859_1));
    assertEquals(Progress.IN_PROGRESS, SafeFileOutputStream.testFile(temp));
    out.flush();
    out.write((int) '\t');

    byte[] data = "Hello World again!".getBytes(StandardCharsets.ISO_8859_1);
    out.transferFrom(new ByteArrayInputStream(data), null);

    out.close();
    assertEquals(Progress.COMPLETE, SafeFileOutputStream.testFile(temp));
    assertTrue(SafeFileOutputStream.waitFor(temp));

    data = Files.readAllBytes(temp.toPath());
    assertEquals("Hello World\tHello World again!", new String(data, StandardCharsets.ISO_8859_1));
    temp.delete();
  }


  @Test
  public void exceptionHandling() throws Exception {
    File temp = File.createTempFile("foo", "bar");
    temp.delete();

    SafeFileOutputStream out = SafeFileOutputStream.getFileOutputStream(temp, false);
    Field field = SafeFileOutputStream.class.getDeclaredField("outputStream");
    field.setAccessible(true);
    field.set(out, null);
    try {
      out.write(1);
      fail();
    } catch (IOException e) {
      assertEquals("Output stream is closed", e.getMessage());
    }

    OutputStream mock = Mockito.mock(OutputStream.class);
    field.set(out, mock);
    Mockito.doThrow(new IOException("TESTING")).when(mock).close();

    try {
      out.close();
      fail();
    } catch (IOException e) {
      assertEquals("TESTING", e.getCause().getMessage());
    }
  }
}