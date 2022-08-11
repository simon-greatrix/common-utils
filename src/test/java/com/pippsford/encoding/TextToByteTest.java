package com.pippsford.encoding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Simon Greatrix on 18/08/2017.
 */
public class TextToByteTest {

  @Test
  public void append() {
    char[] buf = new char[8];

    char[] b2 = TextToByte.append(buf, 1, 'a');
    assertSame(buf, b2);

    b2 = TextToByte.append(buf, 8, 'a');
    assertTrue(b2.length > buf.length);
  }


  @Test
  public void removeWhitespace() {
    String[] tests = {
        "abc",
        "a  b  c",
        "\t\t\f\fabc  ",
    };
    for (String test : tests) {
      char[] buf = test.toCharArray();
      char[] buf2 = TextToByte.removeWhitespace(buf);
      assertEquals("abc", new String(buf2));
      assertNotSame(buf, buf2);
    }
  }


  @Test
  public void removeWhitespace1() {
    String[] tests = {
        "abc",
        "a  b  c",
        "\t\t\f\fabc  ",
    };
    for (String test : tests) {
      String s2 = TextToByte.removeWhitespace(test);
      assertEquals("abc", s2);
    }
  }


  @Test
  public void removeWhitespaceInPlace() {
    String[] tests = {
        "abc", "abc",
        "a  b  c", "abc    ",
        "\t\t\f\fabc  ", "abc      "
    };
    for (int i = 0; i < tests.length; i += 2) {
      char[] buf = tests[i].toCharArray();
      int len = TextToByte.removeWhitespaceInPlace(buf);
      assertEquals(tests[i + 1], new String(buf));
      assertEquals("abc", new String(buf, 0, len));
      assertEquals("abc", new String(buf).trim());
    }
  }


  @Test
  public void trim() {
    char[] chars = "Hello, World!".toCharArray();
    char[] c2 = TextToByte.trim(chars, chars.length);
    assertSame(chars, c2);

    c2 = TextToByte.trim(chars, 5);
    assertEquals(5, c2.length);
    assertEquals("Hello", new String(c2));
  }

}
