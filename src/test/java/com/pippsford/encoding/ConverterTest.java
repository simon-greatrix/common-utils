package com.pippsford.encoding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Simon Greatrix on 16/08/2017.
 */
@Ignore("Tests are invoked by concrete implementations")
public abstract class ConverterTest {

  Converter converter;


  @Test
  public void clean() {
    assertNull("Cleaning null should return null.", converter.clean((String) null));

    String[] values = getCleanTestValues();
    for (String v : values) {
      String y = converter.clean(v);
      assertNotEquals("Cleaning should remove characters", v, y);

      byte[] data1;
      try {
        data1 = converter.decode(v);
      } catch (IllegalArgumentException e) {
        // OK
        continue;
      }
      String u = converter.encode(data1);
      assertEquals("Clean output should be canonical.", y, u);
      byte[] data2 = converter.decode(y);
      assertEquals("Converter output should be unchanged by clean.", HexDump.dump(data1), HexDump.dump(data2));
    }
  }


  @Test
  public void decodeEncode() {
    assertNull("Decoding null should return null.", converter.decode((String) null));
    assertNull("Encoding null should return null.", converter.encode(null));

    Random rand = new Random(0x7e57ab1e);
    for (int i = 0; i < 500; i++) {
      int len = rand.nextInt(256);
      byte[] value = new byte[len];
      rand.nextBytes(value);
      String s = converter.encode(value);
      byte[] output = converter.decode(s);
      assertEquals("Converter failed on encode-decode cycle.", HexDump.dump(value), HexDump.dump(output));

      String c = converter.clean(s);
      assertEquals("Converter output should be canonical.", c, s);
    }
  }


  protected abstract String[] getBadDecodeValues();


  protected abstract String[] getCleanTestValues();


  @Test
  public void randomTest() {
    Random rand = new Random(0x7e57ab1e);
    for (int i = 0; i < 500; i++) {
      int len = 256 + rand.nextInt(256);
      char[] text = new char[len];
      for (int j = 0; j < len; j++) {
        text[j] = (char) rand.nextInt(256);
      }

      char[] cleaned = converter.clean(text);
      byte[] bytes = converter.decode(cleaned);
      char[] encoded = converter.encodeChars(bytes);

      assertEquals("Converter output for " + i + " should be canonical", new String(cleaned), new String(encoded));
    }
  }


  @Test
  public void testBadDecodes() {
    String[] values = getBadDecodeValues();
    for (String s : values) {
      try {
        converter.decode(s);
        fail();
      } catch (IllegalArgumentException e) {
        // correct
      }
    }
  }


  @Test
  public void testEmpty() {
    assertEquals("", converter.clean(""));
    assertEquals("", converter.encode(new byte[0]));
    assertEquals(0, converter.decode("").length);
  }


  @Test
  public void testNulls() {
    assertNull(converter.decode((String) null));
    assertNull(converter.decode((char[]) null));
    assertNull(converter.clean((char[]) null));
    assertNull(converter.clean((String) null));
    assertNull(converter.encode(null));
    assertNull(converter.encodeChars(null));
  }

}
