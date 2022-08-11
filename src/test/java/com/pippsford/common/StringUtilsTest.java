package com.pippsford.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringUtilsTest {

  @Test
  public void testCleanString() throws Exception {
    assertEquals("", StringUtils.cleanString(null));
    assertEquals("", StringUtils.cleanString(""));
    assertEquals("abc", StringUtils.cleanString("abc"));
    assertEquals("abc def", StringUtils.cleanString("abc|def"));
    assertEquals("abc+def", StringUtils.cleanString("abc*def"));
    assertEquals(" +", StringUtils.cleanString("|*"));
  }


  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  @Test
  public void testLogSafe() {
    String nl = System.getProperty("line.separator");
    assertEquals("simple message", StringUtils.logSafe("simple message"));
    assertEquals("simple" + nl + "|       message", StringUtils.logSafe("simple\nmessage"));
    assertEquals("simple" + nl + "|       message" + nl, StringUtils.logSafe("simple\rmessage\n"));
    assertEquals("simple" + nl + "|       message", StringUtils.logSafe("simple\n\rmessage"));
    assertEquals("simple" + nl + "|" + nl + "|       message", StringUtils.logSafe("simple\n\nmessage"));
    assertEquals("simple" + nl + "|" + nl + "|       message", StringUtils.logSafe("simple\r\rmessage"));
    assertEquals("simple" + nl + "|       \ufffd       message" + nl, StringUtils.logSafe("simple\r\n\0\tmessage\r"));
    assertEquals(nl + "|       message", StringUtils.logSafe("\nmessage"));
    assertEquals(nl + "|       message", StringUtils.logSafe("\rmessage"));
    assertEquals(nl + "|       message", StringUtils.logSafe("\n\rmessage"));
    assertEquals(nl + "|       message", StringUtils.logSafe("\r\nmessage"));
    assertEquals(nl + "|"+nl+"|       message", StringUtils.logSafe("\n\nmessage"));
    assertEquals(nl + "|"+nl+"|       message", StringUtils.logSafe("\r\rmessage"));

    String clown = new String(Character.toChars(0x1f921));
    // valid surrogate pair
    assertEquals("hello" + clown + "world", StringUtils.logSafe("hello" + clown + "world"));

    // isolated high surrogate
    assertEquals("hello\ufffdworld", StringUtils.logSafe("hello" + clown.substring(0, 1) + "world"));

    // low surrogate followed by high
    assertEquals("hello\ufffd\ufffdworld", StringUtils.logSafe("hello" + clown.substring(1, 2) + clown.substring(0, 1) + "world"));
    assertEquals("12      123     1234    1234567 12345678        end", StringUtils.logSafe("12\t123\t1234\t1234567\t12345678\tend"));
  }


  @Test
  public void testMatchString() throws Exception {

    // String new_s = cleanString(s);
    assertTrue(StringUtils.matchString("Lorem ipsum dolor sit amet, consectetur adipiscing elit.", "*met*"));
    assertTrue(!StringUtils.matchString("Lorem ipsum dolor sit amt, consectetur adipiscing elit.", "*met*"));
    assertTrue(StringUtils.matchString("", "*"));
    assertTrue(!StringUtils.matchString("", "?"));
    assertTrue(StringUtils.matchString("fred", "fred"));
    assertTrue(!StringUtils.matchString("fred1", "fred"));
    assertTrue(!StringUtils.matchString("fred", "fred1"));
    assertTrue(StringUtils.matchString("fred1", "*???"));
    assertTrue(StringUtils.matchString("fred1", "*?????"));
    assertTrue(StringUtils.matchString("fred1", "*?*?*??*?*"));
    assertTrue(!StringUtils.matchString("fred1", "*??????"));
    assertTrue(!StringUtils.matchString("fred1", "*?????d?"));
    assertTrue(StringUtils.matchString("fred1", "*???d?"));
    assertTrue(StringUtils.matchString("pcpcpcpcd", "*pcd"));
    assertTrue(StringUtils.matchString("fred1", "*???d?*"));
    assertTrue(StringUtils.matchString("fred", "fre?"));
    assertTrue(StringUtils.matchString("fred", "f?ed*"));
    assertTrue(StringUtils.matchString("fredfredfred", "fr****??***ed*f???"));

  }

}